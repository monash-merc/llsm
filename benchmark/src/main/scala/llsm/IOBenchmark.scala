package llsm

import java.io.File
import java.nio.file.{Path, Paths}
import java.util.concurrent.TimeUnit

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

import cats.MonadError
import cats.data.EitherK
import cats.free._
import cats.implicits._
import llsm.algebras.{ImgReader, ImgReaderF, Metadata, MetadataF, Process, ProcessF}
import llsm.fp.ParSeq
import llsm.fp.ParSeq.ops._
import llsm.interpreters._
import llsm.io.LLSMImg
import llsm.io.metadata.ConfigurableMetadata
import monix.eval.Task
import net.imglib2.img.Img
import net.imglib2.img.cell.CellImgFactory
import net.imglib2.`type`.numeric.integer.UnsignedShortType
import org.openjdk.jmh.annotations._
// import _root_.io.scif.config.SCIFIOConfig
import _root_.io.scif.img.ImgOpener

trait IOContext extends BenchmarkContext {

  type App[A] = EitherK[ProcessF, EitherK[ImgReaderF, MetadataF, ?], A]

  def config(interp: InterpolationMethod) = ConfigurableMetadata(0.1018, 0.1018, interp)
    val imgOpener = new ImgOpener(context)
    val cf = new CellImgFactory[UnsignedShortType](
      new UnsignedShortType
    )

  def imgsPath: Path = Paths.get("/Users/keithschulze/Desktop/test5")

  def imgPath: Path = Paths.get("/Users/keithschulze/Desktop/Test 10/test 10_ch0_stack0000_488nm_0000000msec_0016619538msecAbs.tif")

  val imgPaths: List[Path] = new File(imgsPath.toString).listFiles().filter(_.getPath().endsWith(".tif")).map(f => Paths.get(f.getPath())).toList


}

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.SECONDS)
class IOBenchmark extends IOContext {

  def processImg[F[_]: Metadata: ImgReader: Process](
      path: Path
  ): Free[F, LLSMImg] =
    for {
      m   <- Metadata[F].readMetadata(path)
      img <- ImgReader[F].readImg(path, m)
      deskewedImg <- Process[F].deskewImg(
        img,
        0,
        2,
        Deskew.calcShearFactor(m.waveform.sPZTInterval, m.sample.angle, m.config.xVoxelSize),
        m.config.interpolation)
    } yield deskewedImg


  def processStacks[F[_]: Metadata: ImgReader: Process](
      paths: List[Path]
  ): Free[F, List[LLSMImg]] =
    paths.traverse(p => processImg[F](p))

  def processImgs[F[_]: Metadata: ImgReader: Process](
      paths: List[Path]
  ): ParSeq[F, List[LLSMImg]] =
    paths.traverse(p => ParSeq.liftSeq(processImg[F](p)))

  def compiler[M[_]: MonadError[?[_], Throwable]] =
    processInterpreter[M] or
    (scifioReaderInterpreter[M](context, cf) or
    basicMetadataInterpreter[M](config(NNInterpolation), context))

  @Benchmark def ioTry: Try[List[LLSMImg]] = processImgs[App](imgPaths).run(compiler[Try])

  @Benchmark def ioEither: Either[Throwable, List[LLSMImg]] = processImgs[App](imgPaths).run(compiler[Either[Throwable, ?]])

  @Benchmark def ioFuture: List[LLSMImg] = Await.result(processImgs[App](imgPaths).run(compiler[Future]), 1.minutes)

  @Benchmark def ioTask: List[LLSMImg] = {
    import monix.execution.Scheduler.Implicits.global
    Await.result(processImgs[App](imgPaths).run(compiler[Task]).runAsync, 1.minutes)
  }

  def ioRaw: Img[UnsignedShortType] = {
    // val scifioConfig = new SCIFIOConfig().imgOpenerSetImgModes(SCIFIOConfig.ImgMode.CELL)

    imgOpener.openImgs(imgPath.toString, cf).get(0)
  }
}
