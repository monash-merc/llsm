package llsm.io.metadata

import java.nio.file.{Path, Paths}

import cats.free._
import cats.instances.try_._
import llsm.algebras.{Metadata, MetadataF}
import org.scalacheck.Gen
import org.scalacheck.Shrink
import org.scalatest.Matchers._
import scala.util.{Failure, Success, Try}

class MetadataAPISpec extends MetadataSuite {

  implicit val noShrink: Shrink[Int] = Shrink.shrinkAny

  val xGen = Gen.choose(2, 256)
  val yGen = Gen.choose(2, 256)
  val zGen = Gen.choose(2, 10)
  val cGen = Gen.choose(2, 3)
  val tGen = Gen.choose(2, 3)

  "MetadataAPI" should "allow programs to be defined using the Free Monad and Applicative machinery" in forAll(
    (xGen, "x"),
    (yGen, "y"),
    (zGen, "z"),
    (cGen, "c"),
    (tGen, "t"),
    minSuccessful(10)) {
    (w: Int, h: Int, z: Int, c: Int, t: Int) =>
      val imgPath: String = s"16bit-unsigned&pixelType=uint16&lengths=$w,$h,$z,$c,$t&axes=X,Y,Z,Channel,Time.fake"
      val r = scala.util.Random
      val channel = r.nextInt(c)
      val time = r.nextInt(t)

      def program[M[_]: Metadata](path: Path): Free[M, FileMetadata] =
        Metadata[M].readMetadata(path)

      program[MetadataF](Paths.get(imgPath)).foldMap(metaMockInterpreter[Try](channel*time, channel, time)) match {
        case Success(FileMetadata(FilenameMetadata(_, _, st, ch, _, _, _), wave@_, cam@_, _, _)) => {
          st should equal (channel * time)
          ch should equal (channel)
        }
        case Failure(e) => fail(e)
      }
  }
}
