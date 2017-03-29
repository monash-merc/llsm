package llsm.algebras

import java.nio.file.Path
import cats.free.{Free, Inject}

import llsm.io.LLSMImgs

/**
 * API we want to expose
 * writeImg[TIFF, ShortType](pth, img, meta)
 */
trait ImgWriterAPI[F[_]] {
  def writeImg(path: Path, img: LLSMImgs): F[Unit]
}

sealed trait ImgWriterF[A]

object ImgWriterAPI {
  def apply[F[_]](implicit ev: ImgWriterAPI[F]): ImgWriterAPI[F] = ev

  case class WriteImg[A](path: Path, img: LLSMImgs, next: Unit => A) extends ImgWriterF[A]

  implicit val imgWriter = new ImgWriterAPI[ImgWriterF] {
    def writeImg(path: Path, img: LLSMImgs): ImgWriterF[Unit] = WriteImg(path, img, identity)
  }

  implicit def imgWriterInject[F[_], G[_]](implicit F: ImgWriterAPI[F], I: Inject[F, G]): ImgWriterAPI[Free[G, ?]] =
    new ImgWriterAPI[Free[G, ?]] {
      def writeImg(path: Path, img: LLSMImgs): Free[G, Unit] =
        Free.inject[F, G](F.writeImg(path, img))
    }
}

sealed trait LowWriterF[A]
case class WriteSCIFIO(path: Path, img: LLSMImgs) extends LowWriterF[Unit]
case class WriteHDF5(path: Path, img: LLSMImgs) extends LowWriterF[Unit]

object LowWriterAPI {
  def writeSCIFIO(path: Path, img: LLSMImgs): Free[LowWriterF, Unit] =
    Free.liftF[LowWriterF, Unit](WriteSCIFIO(path, img))
  def writeHDF5(path: Path, img: LLSMImgs): Free[LowWriterF, Unit] =
    Free.liftF[LowWriterF, Unit](WriteHDF5(path, img))
}
