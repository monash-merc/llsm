package llsm
package ij

import cats.syntax.either._
import llsm.Deskew
import llsm.io._
import llsm.io.metadata.Metadata._
import java.io.File
import java.util.Arrays
import java.lang.Math
import net.imagej.DatasetService
import net.imagej.axis.Axes
import net.imglib2.img.display.imagej.ImageJFunctions
import net.imglib2.interpolation.randomaccess.{
  NearestNeighborInterpolatorFactory,
  NLinearInterpolatorFactory,
  LanczosInterpolatorFactory
}
import net.imglib2.view.Views
import org.scijava.ItemIO
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.ui.UIService
import org.scijava.plugin.{Plugin, Parameter}
import _root_.ij.ImagePlus
import _root_.ij.measure.Calibration

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
  var ds: DatasetService = _

  @Parameter
  var log: LogService = _

  // @Parameter(`type` = ItemIO.OUTPUT)
  // var out: ImagePlus = _

  /**
    * Entry point to running a plugin.
    */
  override def run(): Unit = {
    if (!input.isDirectory) {
      log.error("input must be a directory")
    }

    val output: Either[LLSMIOError, ImagePlus] = for {
      meta <- extractMetadata(input).leftMap[LLSMIOError](LLSMIOError.MetadataIOError)
      imeta <- convertMetadata(meta)
      img <- readImgs(
        input.listFiles().filter(_.getName contains ".tif").toList,
        imeta)
    } yield {
      val xIndex = imeta.getAxisIndex(Axes.X)
      val zIndex = imeta.getAxisIndex(Axes.Z)
      val cIndex: Option[Int] = imeta.getAxisIndex(Axes.CHANNEL) match {
        case -1     => None
        case i: Int => Some(i)
      }

      val shearFactor: Double = (Math.cos(Math.toRadians(objectiveAngle)) * meta.waveform.sPZTOffset) / pixelSize

      val zInterval: Double = Math.sin(Math.toRadians(objectiveAngle)) * meta.waveform.sPZTOffset + meta.waveform.zPZTOffset

      log.warn(imeta)
      log.warn(imeta.getAxisLength(Axes.X))
      log.warn(imeta.getAxisLength(Axes.Y))
      log.warn(imeta.getAxisLength(Axes.Z))
      log.warn(imeta.getAxisLength(Axes.CHANNEL))
      log.warn(imeta.getAxisLength(Axes.TIME))

      val imgDims = Array.ofDim[Long](img.numDimensions)
      img.dimensions(imgDims)
      log.warn(Arrays.toString(imgDims))

      val rai = interpolation match {
        case "None" =>
          Deskew.deskewStack(img,
                             xIndex,
                             zIndex,
                             Math.round(shearFactor).toInt)
        case "Nearest Neighbour" =>
          Deskew.deskewRealStack(img,
                                 xIndex,
                                 zIndex,
                                 shearFactor,
                                 new NearestNeighborInterpolatorFactory())
        case "Linear" =>
          Deskew.deskewRealStack(img,
                                 xIndex,
                                 zIndex,
                                 shearFactor,
                                 new NLinearInterpolatorFactory())
        case "Lanczos" =>
          Deskew.deskewRealStack(img,
                                 xIndex,
                                 zIndex,
                                 shearFactor,
                                 new LanczosInterpolatorFactory())
      }

      val out: ImagePlus = cIndex match {
        case Some(i) =>
          ImageJFunctions.wrap(Views.permute(rai, i, 2),
                               s"${imeta.getName}_deskewed")
        case None => ImageJFunctions.wrap(rai, s"${imeta.getName}_deskewed")
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
      out
    }

    output match {
      case Right(img) => {
        ui.show(img)
      }
      case Left(LLSMIOError.MetadataIOError(e)) =>
        log.error(s"Error reading metadata:\n$e")
      case Left(LLSMIOError.ImgIOError(e)) => log.error(s"Error reading images:\n$e")
    }
  }
}
