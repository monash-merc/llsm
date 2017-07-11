package llsm.io

import java.nio.file.{Path, Paths}
import scala.util.{Try, Success, Failure}

import cats.data.Coproduct
import cats.implicits._
import cats.free.Free
import llsm.algebras.{Metadata, MetadataF, ImgReader, ImgReaderF, Process, ProcessF}
import llsm.interpreters._
import llsm.io.metadata.MetadataSuite
import net.imglib2.img.Img
import net.imglib2.img.array.ArrayImgFactory
import net.imglib2.`type`.numeric.integer.UnsignedShortType
import _root_.io.scif.SCIFIO

import org.scalacheck.Gen
import org.scalatest.Matchers._

class IOSpec extends IOSuite {

  System.setProperty("scijava.log.level", "NONE")

  val xGen = Gen.choose(1, 256)
  val yGen = Gen.choose(1, 256)
  val zGen = Gen.choose(1, 10)
  val cGen = Gen.choose(1, 3)
  val tGen = Gen.choose(1, 5)
  val shearFactor = Gen.choose(1.0, 5.0)

  "IO APIs" should "allow programs to be defined using the Free Monad and Applicative machinery" ignore forAll(
    (xGen, "x"),
    (yGen, "y"),
    (zGen, "z"),
    (cGen, "c"),
    (tGen, "t"),
    (shearFactor, "shear"),
    minSuccessful(10)) {
    (w: Int, h: Int, z: Int, c: Int, t: Int, shear: Double) =>
      val imgPath: String = s"16bit-unsigned&pixelType=uint16&lengths=$w,$h,$z,$c,$t&axes=X,Y,Z,Channel,Time.fake"

      val r = scala.util.Random
      val channel = r.nextInt(c)
      val time = r.nextInt(t)

      def program[M[_]: Metadata: ImgReader: Process](path: Path): Free[M, LLSMImg] =
          for {
            m <- Metadata[M].readMetadata(path)
            i <- ImgReader[M].readImg(path, m)
            deskewedImg <- Process[M].deskewImg(i, 0, 2, shear, m.config.interpolation)
          } yield deskewedImg

      type MetaImg[A] = Coproduct[MetadataF, ImgReaderF, A]
      type App[A] = Coproduct[ProcessF, MetaImg, A]

      val interpreter = processCompiler[Try] or (new MetadataSuite().metaMockCompiler[Try](channel*time, channel, time) or scifioReader[Try](new SCIFIO().getContext, new ArrayImgFactory[UnsignedShortType]))

      program[App](Paths.get(imgPath)).foldMap(interpreter) match {
        case Success(LLSMImg(img, meta)) => {
          img shouldBe a [Img[_]]
          img.numDimensions should equal (5)
          val dims = Array.ofDim[Long](img.numDimensions)
          img.dimensions(dims)
          val w2 = w + (z * shear) - shear
          dims should equal (Array(w2.toLong, h.toLong, z.toLong, c.toLong, t.toLong))
        }
        case Failure(e) => fail(e)
      }
  }
}
