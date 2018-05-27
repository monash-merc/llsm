package llsm.algebras

import java.nio.file.Path

import cats.InjectK
import cats.free.Free
import llsm.io.LLSMImg
import llsm.io.metadata.FileMetadata

trait ImgReaderAPI[F[_]] {
  def readImg(path: Path, meta: FileMetadata): F[LLSMImg]
}

sealed trait ImgReaderF[A]

object ImgReaderAPI {

  case class ReadImg[A](path: Path, meta: FileMetadata, next: LLSMImg => A)
      extends ImgReaderF[A]

  implicit val imgReader = new ImgReaderAPI[ImgReaderF] {
    def readImg(path: Path, meta: FileMetadata): ImgReaderF[LLSMImg] =
      ReadImg(path, meta, identity)
  }

  implicit def imgReaderInject[F[_], G[_]](
      implicit F: ImgReaderAPI[F],
      I: InjectK[F, G]): ImgReaderAPI[Free[G, ?]] =
    new ImgReaderAPI[Free[G, ?]] {
      def readImg(path: Path, meta: FileMetadata): Free[G, LLSMImg] =
        Free.inject[F, G](F.readImg(path, meta))
    }
}
