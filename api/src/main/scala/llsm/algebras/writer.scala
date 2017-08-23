package llsm.algebras

import java.nio.file.Path
import cats.free.{Free, Inject}

import llsm.io.LLSMImg
import llsm.io.metadata.FileMetadata

trait ImgWriterAPI[F[_]] {
  def writeImg(path: Path, img: LLSMImg): F[FileMetadata]
}

sealed trait ImgWriterF[A]

object ImgWriterAPI {
  def apply[F[_]](implicit ev: ImgWriterAPI[F]): ImgWriterAPI[F] = ev

  case class WriteImg[A](path: Path, img: LLSMImg, next: FileMetadata => A) extends ImgWriterF[A]

  implicit val imgWriter = new ImgWriterAPI[ImgWriterF] {
    def writeImg(path: Path, img: LLSMImg): ImgWriterF[FileMetadata] = WriteImg(path, img, identity)
  }

  implicit def imgWriterInject[F[_], G[_]](implicit F: ImgWriterAPI[F], I: Inject[F, G]): ImgWriterAPI[Free[G, ?]] =
    new ImgWriterAPI[Free[G, ?]] {
      def writeImg(path: Path, img: LLSMImg): Free[G, FileMetadata] =
        Free.inject[F, G](F.writeImg(path, img))
    }
}

sealed trait LowWriterF[A]
case class WriteOMETIFF(path: Path, img: LLSMImg) extends LowWriterF[FileMetadata]
case class WriteHDF5(path: Path, img: LLSMImg) extends LowWriterF[FileMetadata]
case class WriteError(t: Throwable) extends LowWriterF[FileMetadata]

object LowWriterAPI {
  def writeOMETIFF(path: Path, img: LLSMImg): Free[LowWriterF, FileMetadata] =
    Free.liftF[LowWriterF, FileMetadata](WriteOMETIFF(path, img))
  def writeHDF5(path: Path, img: LLSMImg): Free[LowWriterF, FileMetadata] =
    Free.liftF[LowWriterF, FileMetadata](WriteHDF5(path, img))
  def writeError(t: Throwable): Free[LowWriterF, FileMetadata] =
    Free.liftF[LowWriterF, FileMetadata](WriteError(t))
}
