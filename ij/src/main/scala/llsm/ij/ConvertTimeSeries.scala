package llsm.ij

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.util.stream.Collectors

import scala.collection.JavaConverters._
import scala.util.{Try, Success, Failure}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import cats.MonadError
import cats.data.Coproduct
import cats.implicits._
import llsm.{NoInterpolation, NNInterpolation, LinearInterpolation, LanczosInterpolation, Programs}
import llsm.algebras.{MetadataF, ImgReaderF, ImgWriterF, ProcessF, ProgressF}
import llsm.fp.ParSeq.ops._
import llsm.interpreters._
import llsm.io.metadata.ConfigurableMetadata
import llsm.ij.interpreters._
import org.scijava.{Context, ItemIO}
import org.scijava.app.StatusService
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.ui.UIService
import org.scijava.plugin.{Plugin, Parameter}


@Plugin(`type` = classOf[Command],
        headless = true,
        menuPath = "Plugins>LLSM>Convert Time Series")
class LLSMConvertPlugin extends Command {

  @Parameter(style = "directory", `type` = ItemIO.INPUT)
  var input: File = _

  @Parameter(style = "directory", label = "Output Directory", `type` = ItemIO.INPUT)
  var output: File = _

  @Parameter(validater = "validateName")
  var outputFileName: String = _

  var validName: Boolean = false

  @Parameter(label = "X/Y voxel size (um)", required = true)
  var pixelSize: Double = 0.1018

  @Parameter(label = "Incident objective angle")
  var objectiveAngle: Double = 31.8

  @Parameter(label = "Interpolation scheme",
             choices = Array("None", "Nearest Neighbour", "Linear", "Lanczos"),
             required = false)
  var interpolation: String = "None"

  @Parameter(label = "Parallel processing (experimental)")
  var parallel: Boolean = false

  @Parameter
  var ui: UIService = _

  @Parameter
  var log: LogService = _

  @Parameter
  var status: StatusService = _

  @Parameter
  var context: Context = _

  def validateName(): Unit = {
    validName =
      if (outputFileName.endsWith(".h5") || outputFileName.endsWith(".ome.tif"))
        true
      else false
  }
  /**
    * Entry point to running a plugin.
    */
  override def run(): Unit = if (validName) {

    type App[A] = Coproduct[ImgWriterF, Coproduct[ProcessF, Coproduct[ImgReaderF, Coproduct[MetadataF, ProgressF, ?], ?], ?], A]

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
                llsmWriter[M](context) or
                    (processCompiler[M] or
                      (ijImgReader[M](context, log) or
                        (ijMetadataReader[M](config, log) or
                          ijProgress[M](status))))

    val imgPaths = Files.list(Paths.get(input.getPath))
      .collect(Collectors.toList[Path])
      .asScala.filter(_.toString.endsWith(".tif"))

    val program = Programs.convertImgsP[App](imgPaths.toList, Paths.get(output.getPath, outputFileName))

    val outputF: Try[List[Unit]] => Unit = result => result match {
      case Success(_) => {
        log.info("Successfully converted images")
      }
      case Failure(e) => log.error(e)
    }

    if (parallel) {
      program.run(compiler[Future]) onComplete outputF
    } else {
      outputF(program.run(compiler[Try]))
    }
  } else {
    log.error("Invalid output type. Only .h5 and .ome.tif are supported")
  }
}
