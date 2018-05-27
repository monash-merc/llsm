package llsm.ij

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.util.stream.Collectors

import scala.collection.JavaConverters._

import cats.MonadError
import cats.data.EitherK
import cats.free.Free
import cats.implicits._
import io.scif.img.cell.SCIFIOCellImgFactory
import llsm.{
  Programs,
  NoInterpolation,
  NNInterpolation,
  LinearInterpolation,
  LanczosInterpolation
}
import llsm.algebras.{
  Metadata,
  MetadataF,
  ImgReader,
  ImgReaderF,
  Process,
  ProcessF,
  Progress,
  ProgressF,
  Visualise,
  VisualiseF
}
import llsm.interpreters._
import llsm.io.metadata.ConfigurableMetadata
import llsm.ij.interpreters._
import net.imagej.DatasetService
import net.imglib2.cache.img.DiskCachedCellImgOptions
import net.imglib2.img.ImgFactory
import net.imglib2.img.array.ArrayImgFactory
import net.imglib2.img.planar.PlanarImgFactory
import net.imglib2.`type`.numeric.integer.UnsignedShortType
import org.scijava.{Context, ItemIO}
import org.scijava.app.StatusService
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.ui.UIService
import org.scijava.plugin.{Plugin, Parameter}

/** ImageJ plugin for reading, processing and previewing raw LLSM
  * datasets in ImageJ/Fiji.
  */
@Plugin(`type` = classOf[Command],
        headless = true,
        menuPath = "Plugins>LLSM>Preview Dataset...")
class PreviewPlugin extends Command {

  @Parameter(style = "directory", `type` = ItemIO.INPUT)
  var input: File = new File(System.getProperty("user.home"))

  @Parameter(label = "X/Y voxel size (um)", required = true)
  var pixelSize: Double = 0.104

  @Parameter(label = "Img Container Type",
             choices = Array("Array", "Planar", "Cell"),
             persist = false,
             required = false)
  var container: String = "Cell"

  @Parameter(label = "Interpolation scheme",
             choices = Array("None", "Nearest Neighbour", "Linear", "Lanczos"),
             persist = false,
             required = false)
  var interpolation: String = "None"

  @Parameter(label = "Preview in BigDataViewer", persist = false)
  var bdv: Boolean = false

  @Parameter
  var ds: DatasetService = _

  @Parameter
  var ui: UIService = _

  @Parameter
  var log: LogService = _

  @Parameter
  var status: StatusService = _

  @Parameter
  var context: Context = _

  def processImgs[F[_]: Metadata: ImgReader: Process: Progress: Visualise](
      paths: List[Path]
  ): Free[F, Unit] =
    for {
      imgs <- paths.zipWithIndex.traverse {
        case (p, i) =>
          for {
            img <- Programs.processImg[F](p)
            _   <- Progress[F].progress(i + 1, paths.size)
          } yield img
      }
      agg <- Process[F].aggregate(imgs)
      o   <- Visualise[F].show(agg)
    } yield o

  /**
    * Entry point to running a plugin.
    */
  override def run(): Unit = {

    type App[A] =
      EitherK[VisualiseF,
              EitherK[ProcessF,
                      EitherK[ImgReaderF, EitherK[MetadataF, ProgressF, ?], ?],
                      ?],
              A]

    val config = ConfigurableMetadata(
      pixelSize,
      pixelSize,
      interpolation match {
        case "Nearest Neighbour" => NNInterpolation
        case "Linear"            => LinearInterpolation
        case "Lancsoz"           => LanczosInterpolation
        case _                   => NoInterpolation
      }
    )

    val imgFactory: ImgFactory[UnsignedShortType] = container match {
      case "Array" =>
        new ArrayImgFactory[UnsignedShortType](new UnsignedShortType)
      case "Planar" =>
        new PlanarImgFactory[UnsignedShortType](new UnsignedShortType)
      case "Cell" =>
        new SCIFIOCellImgFactory[UnsignedShortType](
          new UnsignedShortType,
          DiskCachedCellImgOptions.options()
        )
      case _ =>
        throw new Exception(
          "Unknown Img container type. Please submit a bug report.")
    }

    def compiler[M[_]: MonadError[?[_], Throwable]] =
      visInterpreter[M](if (bdv) BigDataViewer else HyperStack) or
        (processInterpreter[M] or
          (ijImgReaderInterpreter[M](context, imgFactory, log) or
            (ijMetadataInterpreter[M](config, context, log) or
              ijProgressInterpreter[M](status))))

    val imgPaths = Files
      .list(Paths.get(input.getPath))
      .collect(Collectors.toList[Path])
      .asScala
      .filter(_.toString.endsWith(".tif"))

    val prog = processImgs[App](imgPaths.toList)

    prog.foldMap(compiler[Either[Throwable, ?]]) match {
      case Left(e) => log.error(e)
      case _       => ()
    }
  }
}
