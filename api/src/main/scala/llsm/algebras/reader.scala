package llsm.algebras

import java.nio.file.Path

import cats.free.{Free, Inject}
import llsm.io.LLSMStack
import llsm.io.metadata.FileMetadata

trait ImgReaderAliases {
}

trait ImgReaderAPI[F[_]] {
  def readImg(path: Path, meta: FileMetadata): F[LLSMStack]
}

sealed trait ImgReaderF[A]

object ImgReaderAPI {

  case class ReadImg[A](path: Path, meta: FileMetadata, next: LLSMStack => A) extends ImgReaderF[A]


  implicit val imgReader = new ImgReaderAPI[ImgReaderF] {
    def readImg(path: Path, meta: FileMetadata): ImgReaderF[LLSMStack] =
      ReadImg(path, meta, identity)
  }

  implicit def imgReaderInject[F[_], G[_]](implicit F: ImgReaderAPI[F], I: Inject[F, G]): ImgReaderAPI[Free[G, ?]] =
    new ImgReaderAPI[Free[G, ?]] {
      def readImg(path: Path, meta: FileMetadata): Free[G, LLSMStack] =
        Free.inject[F, G](F.readImg(path, meta))
    }
}
