package llsm.cli

import java.nio.file.{Files, Path, Paths}
import java.util.stream.Collectors

import scala.collection.JavaConverters._
import scala.util.{Try, Success, Failure}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import cats._
import cats.data.Coproduct
import cats.implicits._
import io.scif.SCIFIOService
import io.scif.img.cell.SCIFIOCellImgFactory
import llsm.{
  InterpolationMethod,
  NoInterpolation,
  NNInterpolation,
  LinearInterpolation,
  LanczosInterpolation,
  Programs
}
import llsm.algebras.{
  MetadataF,
  ImgReaderF,
  ImgWriterF,
  ProcessF,
  ProgressF
}
import llsm.cli.Interpreters._
import llsm.fp.ParSeq.ops._
import llsm.interpreters._
import llsm.io.{
  ImgContainer,
  ArrayContainer,
  PlanarContainer,
  CellContainer
}
import llsm.io.metadata.ConfigurableMetadata
import net.imglib2.img.array.ArrayImgFactory
import net.imglib2.img.planar.PlanarImgFactory
import net.imglib2.`type`.numeric.integer.UnsignedShortType
import org.scijava.Context
import org.scijava.service.SciJavaService

case class Config(
    input: Path = Paths.get("."),
    output: Path = Paths.get("."),
    xVoxelSize: Double = 0.104,
    yVoxelSize: Double = 0.104,
    interpolation: InterpolationMethod = NoInterpolation,
    container: ImgContainer = CellContainer,
    single: Boolean = false,
    parallel: Boolean = false)

object Config {

  implicit val interp: scopt.Read[InterpolationMethod] =
    scopt.Read.reads(str => str match {
      case "None"     => NoInterpolation
      case "Nearest"  => NNInterpolation
      case "Linear"   => LinearInterpolation
      case "Lanczos"  => LanczosInterpolation
    })

  implicit val container: scopt.Read[ImgContainer] =
    scopt.Read.reads(str => str match {
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
      .action( (x, c) => c.copy(input = x) )
      .text("Path to directory of images or image (single) to be converted/processed")

    opt[Path]('o', "output")
      .required()
      .valueName("<dir> or <file>")
      .action( (x, c) => c.copy(output = x) )
      .text("Path to which processed ouput will be saved")

    opt[Double]("vx")
      .action( (x, c) => c.copy(xVoxelSize = x) )
      .text("Voxel size X. Default is 0.104 μm")

    opt[Double]("vy")
      .action( (y, c) => c.copy(yVoxelSize = y) )
      .text("Voxel size Y. Default is 0.104 μm")

    opt[InterpolationMethod]("interp")
      .action( (i, c) => c.copy(interpolation = i) )
      .text("Interpolation method. Options are \"None\" (default), \"Nearest\", \"Linear\", \"Lanczos\"")

    opt[ImgContainer]("container")
      .action( (x, c) => c.copy(container = x) )
      .text("Img backing container. Options are \"Cell\" (default), \"Array\" or \"Planar\"")

    opt[Unit]("parallel")
      .action( (_, c) => c.copy(parallel = true) )
      .hidden()
      .text("Enables experimental parallel processing.")

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
      .action( (_, c) => c.copy(single = true) )
      .text("Process only a single stack. This is useful for HPC environments.")

    checkConfig( c =>
        if (c.single && Files.isDirectory(c.input))
          failure("--input must be a path to a LLSM stack file.")
        else if (!c.single && !Files.isDirectory(c.input))
          failure("--input must be a path to a directory containing a LLSM dataset")
        else if (c.single && !c.output.toString.endsWith(".ome.tif"))
          failure("Only OME-TIFF (.ome.tif) output format is supported for converting single files")
        else success )
  }

  parser.parse(args, Config()) match {
    case Some(config) => {

      type App[A] = Coproduct[ImgWriterF, Coproduct[ProcessF, Coproduct[ImgReaderF, Coproduct[MetadataF, ProgressF, ?], ?], ?], A]

      val context = new Context(classOf[SCIFIOService], classOf[SciJavaService])

      val imgFactory = config.container match {
        case ArrayContainer   => new ArrayImgFactory[UnsignedShortType]
        case PlanarContainer  => new PlanarImgFactory[UnsignedShortType]
        case CellContainer    => new SCIFIOCellImgFactory[UnsignedShortType]
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
          Files.list(config.input)
            .collect(Collectors.toList[Path])
            .asScala
            .filter(_.toString.endsWith(".tif"))
            .toList

      def compiler[M[_]: MonadError[?[_], Throwable]] =
                llsmWriter[M](context) or
                    (processCompiler[M] or
                      (cliImgReader[M](context, imgFactory) or
                        (cliMetadataReader[M](conf) or
                          consoleProgress[M])))

      val program = Programs.convertImgsP[App](fl, config.output)

      val outputF: Try[List[Unit]] => Unit =
        result => result match {
          case Success(_) => {
            println("Successfully converted images")
            context.dispose
          }
          case Failure(e) => println(s"ERROR: $e")
        }

      if (config.parallel) {
        program.run(compiler[Future]) onComplete outputF
      } else {
        outputF(program.run(compiler[Try]))
      }
    }
    case None =>
  }
}