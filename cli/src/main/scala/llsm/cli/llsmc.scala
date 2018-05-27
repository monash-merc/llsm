package llsm.cli

import java.nio.file.{Files, Path, Paths}
import java.util.stream.Collectors

import scala.collection.JavaConverters._
import scala.util.{Try, Success, Failure}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import cats._
import cats.data.EitherK
import cats.implicits._
import io.scif.SCIFIOService
import io.scif.img.cell.SCIFIOCellImgFactory
import llsm.{
  InterpolationMethod,
  NoInterpolation,
  NNInterpolation,
  LinearInterpolation,
  LanczosInterpolation,
  Programs,
  ImgUtils
}
import llsm.algebras.{
  Metadata,
  MetadataF,
  ImgReader,
  ImgReaderF,
  ImgWriter,
  ImgWriterF,
  Process,
  ProcessF,
  Progress,
  ProgressF
}
import llsm.interpreters.WriterUtils
import llsm.cli.Interpreters._
import llsm.fp.ParSeq
import llsm.fp.ParSeq.ops._
import llsm.interpreters._
import llsm.io.{ImgContainer, ArrayContainer, PlanarContainer, CellContainer}
import llsm.io.metadata.ConfigurableMetadata
import net.imglib2.img.array.ArrayImgFactory
import net.imglib2.img.planar.PlanarImgFactory
import net.imglib2.`type`.numeric.integer.UnsignedShortType
import org.scijava.Context
import org.scijava.service.SciJavaService

case class Config(input: Path = Paths.get("."),
                  output: Path = Paths.get("."),
                  xVoxelSize: Double = 0.104,
                  yVoxelSize: Double = 0.104,
                  interpolation: InterpolationMethod = NoInterpolation,
                  container: ImgContainer = CellContainer,
                  single: Boolean = false,
                  overwrite: Boolean = false,
                  parallel: Boolean = false,
                  debug: Boolean = false)

object Config {

  implicit val interp: scopt.Read[InterpolationMethod] =
    scopt.Read.reads(str =>
      str match {
        case "None"    => NoInterpolation
        case "Nearest" => NNInterpolation
        case "Linear"  => LinearInterpolation
        case "Lanczos" => LanczosInterpolation
    })

  implicit val container: scopt.Read[ImgContainer] =
    scopt.Read.reads(str =>
      str match {
        case "Array"  => ArrayContainer
        case "Planar" => PlanarContainer
        case "Cell"   => CellContainer
    })

  implicit val path: scopt.Read[Path] =
    scopt.Read.reads(Paths.get(_))

  implicit val showConfig = new Show[Config] {
    def show(c: Config): String = {
      s"input: ${c.input}\noutput: ${c.output}\nx: ${c.xVoxelSize}"
    }
  }
}

object LLSMC extends App {
  import Config._

  val parser = new scopt.OptionParser[Config]("llsmc") {
    head("llsm convert", "0.3.0")

    opt[Path]('i', "input")
      .required()
      .valueName("<dir>")
      .action((x, c) => c.copy(input = x))
      .text("Path to directory of images or image (single) to be converted/processed")

    opt[Path]('o', "output")
      .required()
      .valueName("<dir> or <file>")
      .action((x, c) => c.copy(output = x))
      .text("Path to which processed ouput will be saved")

    opt[Double]("vx")
      .action((x, c) => c.copy(xVoxelSize = x))
      .text("Voxel size X. Default is 0.104 μm")

    opt[Double]("vy")
      .action((y, c) => c.copy(yVoxelSize = y))
      .text("Voxel size Y. Default is 0.104 μm")

    opt[InterpolationMethod]("interp")
      .action((i, c) => c.copy(interpolation = i))
      .text(
        "Interpolation method. Options are \"None\" (default), \"Nearest\", \"Linear\", \"Lanczos\"")

    opt[ImgContainer]("container")
      .action((x, c) => c.copy(container = x))
      .text(
        "Img backing container. Options are \"Cell\" (default), \"Array\" or \"Planar\"")

    opt[Unit]("overwrite")
      .action((_, c) => c.copy(overwrite = true))
      .text("Overwrite outputs if they already exist")

    opt[Unit]("parallel")
      .action((_, c) => c.copy(parallel = true))
      .hidden()
      .text("Enables experimental parallel processing.")

    opt[Unit]("debug")
      .action((_, c) => c.copy(debug = true))
      .hidden()
      .text("Enable debug logging")

    // opt[Double]("zPiezoStep")
    //   .action( (z, c) => c.copy(zPiezoStep = Some(z)) )
    //   .text("Z Piezo stage increment step size")

    // opt[Double]("sPiezoStep")
    //   .action( (s, c) => c.copy(sPiezoStep = Some(s)) )
    //   .text("Sample Piezo stage increment step size")

    // opt[Double]("angle")
    //   .action( (a, c) => c.copy(angle = Some(a)) )
    //   .text("Angle between sample stage and illumination objective")

    help("help")
      .text("Prints this usage text")

    note("\n")

    cmd("single")
      .action((_, c) => c.copy(single = true))
      .text("Process only a single stack. This is useful for HPC environments.")

    checkConfig(c =>
      if (c.single) {
        if (Files.isDirectory(c.input))
          failure("--input must be a path to a LLSM stack file.")
        else if (!c.output.toString.endsWith(".ome.tif"))
          failure(
            "Only OME-TIFF (.ome.tif) output format is supported for converting single files")
        else if (!Files.exists(c.output.getParent()))
          failure(
            "Parent path for the output doesn't exist i.e., please create the directories leading to the output image.")
        else if (!Files.exists(c.output) && !c.overwrite)
          failure(
            "Output file already exists. Please include the --overwrite flag if you want to overwrite the exisiting file.")
        else success
      } else {
        if (!Files.isDirectory(c.input))
          failure(
            "--input must be a path to a directory containing a LLSM dataset")
        else if (!Files.exists(c.output.getParent()))
          failure(
            "Parent path for the output doesn't exist i.e., please create the directories leading to the output image.")
        else if (WriterUtils.outputExists(c.output) && !c.overwrite)
          failure(
            "Output file/files already exist. Please include the --overwrite flag if you want to overwrite the exisiting file/files.")
        else success
    })
  }

  parser.parse(args, Config()) match {
    case Some(config) => {

      type App[A] =
        EitherK[
          ImgWriterF,
          EitherK[ProcessF,
                  EitherK[ImgReaderF, EitherK[MetadataF, ProgressF, ?], ?],
                  ?],
          A]

      val context = new Context(classOf[SCIFIOService], classOf[SciJavaService])

      val imgFactory = config.container match {
        case ArrayContainer =>
          new ArrayImgFactory[UnsignedShortType](
            new UnsignedShortType
          )
        case PlanarContainer =>
          new PlanarImgFactory[UnsignedShortType](
            new UnsignedShortType
          )
        case CellContainer =>
          new SCIFIOCellImgFactory[UnsignedShortType](
            new UnsignedShortType
          )
      }

      val conf = ConfigurableMetadata(
        config.xVoxelSize,
        config.yVoxelSize,
        config.interpolation
      )

      val fl: List[Path] =
        if (config.single)
          List(config.input)
        else
          Files
            .list(config.input)
            .collect(Collectors.toList[Path])
            .asScala
            .filter(_.toString.endsWith(".tif"))
            .toList

      def compiler[M[_]: MonadError[?[_], Throwable]] =
        imgWriterInterpreter[M](context) or
          (processInterpreter[M] or
            (cliImgReaderInterpreter[M](context, imgFactory, config.debug) or
              (cliMetadataInterpreter[M](conf, context, config.debug) or
                consoleProgressInterpreter[M])))

      val prog = program[App](fl, config.output, context)

      val outputF: Try[Either[Throwable, Unit]] => Unit =
        result => {
          context.dispose
          result match {
            case Success(Right(_)) =>
              println("Successfully converted images")
            case Success(Left(e)) => println(s"ERROR: ${e.getMessage}")
            case Failure(e)       => println(s"ERROR: ${e.getMessage}")
          }
        }

      if (config.parallel) {
        prog.run(compiler[Future]) onComplete outputF
      } else {
        outputF(prog.run(compiler[Try]))
      }
    }
    case None => println("ERROR: Bad config")
  }

  def program[F[_]: Metadata: ImgReader: ImgWriter: Process: Progress](
      paths: List[Path],
      outputPath: Path,
      context: Context
  ): ParSeq[F, Either[Throwable, Unit]] =
    Programs
      .convertImgsP[F](paths, outputPath)
      .map(lImg =>
        ImgUtils
          .writeOMEMetadata[Either[Throwable, ?]](outputPath, lImg, context))
}
