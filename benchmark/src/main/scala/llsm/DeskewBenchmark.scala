package llsm

import org.openjdk.jmh.annotations._
import java.util.concurrent.TimeUnit
import net.imglib2.RandomAccessibleInterval
import net.imglib2.img.Img
import net.imglib2.interpolation.randomaccess.{
  NLinearInterpolatorFactory,
  LanczosInterpolatorFactory
}
import net.imglib2.`type`.numeric.integer.UnsignedShortType
import _root_.io.scif.img.ImgOpener
import _root_.io.scif.config.SCIFIOConfig

trait FakeDeskewData extends BenchmarkContext {
  val imgio: ImgOpener = new ImgOpener(context)
  val conf: SCIFIOConfig =
    new SCIFIOConfig().imgOpenerSetOpenAllImages(false).imgOpenerSetIndex(0)
  val fakeImg: String =
    "16bit-unsigned&pixelType=uint16&lengths=100,100,100&axes=X,Y,Z.fake"
  val img: Img[UnsignedShortType] =
    imgio.openImgs(fakeImg, new UnsignedShortType, conf).get(0)
}

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.SECONDS)
class FakeDeskewBenchmark extends FakeDeskewData {

  def toImg(view: RandomAccessibleInterval[UnsignedShortType])
    : Img[UnsignedShortType] = {
    val out: Img[UnsignedShortType] =
      img.factory().create(view, new UnsignedShortType)

    val oc  = out.localizingCursor
    val vra = view.randomAccess
    while (oc.hasNext) {
      oc.fwd
      vra.setPosition(oc)

      oc.get().set(vra.get())
    }
    out
  }

  @Benchmark def deskewBBTransform: Img[UnsignedShortType] =
    toImg(Deskew.deskewStack(img, 0, 2, 3))

  @Benchmark def deskewNearestNeighbourInt: Img[UnsignedShortType] =
    toImg(Deskew.deskewRealStack(img, 0, 2, 2.9))

  @Benchmark def deskewNLinearInt: Img[UnsignedShortType] =
    toImg(
      Deskew.deskewRealStack(img, 0, 2, 2.9, new NLinearInterpolatorFactory))

  // @Benchmark
  def deskewLanczosInt: Img[UnsignedShortType] =
    toImg(
      Deskew.deskewRealStack(img, 0, 2, 2.9, new LanczosInterpolatorFactory))
}
