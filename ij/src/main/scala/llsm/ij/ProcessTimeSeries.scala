package llsm.ij

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.util.stream.Collectors

import scala.collection.JavaConverters._
import scala.util.{Success, Failure}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import cats.MonadError
import cats.data.Coproduct
import cats.implicits._
import ij.ImagePlus
import ij.measure.Calibration
import llsm.{Programs, NoInterpolation, NNInterpolation, LinearInterpolation, LanczosInterpolation}
import llsm.fp.ParSeq.ops._
import llsm.algebras.{MetadataF, ImgReaderF, ProcessF, ProgressF}
import llsm.interpreters._
import llsm.io.LLSMImg
import llsm.io.metadata.{AggregatedMeta, ConfigurableMetadata}
import llsm.ij.interpreters._
import net.imagej.axis.Axes
import net.imglib2.img.display.imagej.ImageJFunctions
import net.imglib2.view.Views
import org.scijava.{Context, ItemIO}
import org.scijava.app.StatusService
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.ui.UIService
import org.scijava.plugin.{Plugin, Parameter}


@Plugin(`type` = classOf[Command],
  headless = true,
  menuPath = "Plugins>LLSM>Deskew Time Series")
class DeskewTimeSeriesPlugin extends Command {

  @Parameter(style = "directory", `type` = ItemIO.INPUT)
  var input: File = _

  @Parameter(label = "X/Y voxel size (um)", required = true)
  var pixelSize: Double = 0.1018

  @Parameter(label = "Incident objective angle")
  var objectiveAngle: Double = 31.8

  @Parameter(label = "Interpolation scheme",
    choices = Array("None", "Nearest Neighbour", "Linear", "Lanczos"),
    required = false)
  var interpolation: String = "None"

  @Parameter
  var ui: UIService = _

  @Parameter
  var log: LogService = _

  @Parameter
  var status: StatusService = _

  @Parameter
  var context: Context = _

  /**
   * Entry point to running a plugin.
   */
  override def run(): Unit = {

    type App[A] = Coproduct[ProcessF,
    Coproduct[ImgReaderF,
    Coproduct[MetadataF, ProgressF, ?], ?], A]

    val config = ConfigurableMetadata(
      pixelSize,
      pixelSize,
      interpolation match {
        case "Nearest Neighbour" => NNInterpolation
        case "Linear" => LinearInterpolation
        case "Lancsoz" => LanczosInterpolation
        case _ => NoInterpolation
      })

    def compiler[M[_]: MonadError[?[_], Throwable]] =
      processCompiler[M] or
    (ijImgReader[M](context, log) or
      (ijMetadataReader[M](config, log) or
        ijProgress[M](status)))

    val imgPaths = Files.list(Paths.get(input.getPath))
      .collect(Collectors.toList[Path])
      .asScala.filter(_.toString.endsWith(".tif"))

    Programs.processImgs[App](imgPaths.toList).run(compiler[Future]) onComplete {
      r => r match {
        case Success(LLSMImg(img, AggregatedMeta(imeta))) => {
          val xIndex = imeta.getAxisIndex(Axes.X)
          val zIndex = imeta.getAxisIndex(Axes.Z)
          val cIndex: Option[Int] = imeta.getAxisIndex(Axes.CHANNEL) match {
            case -1     => None
            case i: Int => Some(i)
          }

          val imgDims = Array.ofDim[Long](img.numDimensions)
          img.dimensions(imgDims)

          val out: ImagePlus = cIndex match {
            case Some(i) =>
              ImageJFunctions.wrap(Views.permute(img, i, 2),
                s"${imeta.getName}_deskewed")
              case None =>
                ImageJFunctions.wrap(img, s"${imeta.getName}_deskewed")
          }
          val cal = new Calibration(out)
          cal.setUnit("um")
          cal.pixelWidth = imeta.getAxis(Axes.X).calibratedValue(1)
          cal.pixelHeight = imeta.getAxis(Axes.Y).calibratedValue(1)
          cal.pixelDepth = imeta.getAxis(Axes.Z).calibratedValue(1)

          if (imeta.getAxisLength(Axes.TIME) > 1) {
            cal.setTimeUnit("ms")
            cal.frameInterval = imeta.getAxis(Axes.TIME).calibratedValue(1)
          }

          out.setCalibration(cal)
          out.setDimensions(imeta.getAxisLength(Axes.CHANNEL).toInt,
            imeta.getAxisLength(Axes.Z).toInt,
            imeta.getAxisLength(Axes.TIME).toInt)
          ui.show(out)
        }
              case Failure(e) => log.error(e)
      }
    }
  }
}
