package llsm

import cats.free.{Free, Inject}

package object algebras {

  type Progress[G[_]] = Inject[ProgressF, G]

  object Progress {
    def apply[F[_]](implicit ev: ProgressAPI[Free[F, ?]]): ProgressAPI[Free[F, ?]] = ev
  }

  type ImgReader[G[_]] = Inject[ImgReaderF, G]

  object ImgReader {
    def apply[F[_]](implicit ev: ImgReaderAPI[Free[F, ?]]): ImgReaderAPI[Free[F, ?]] = ev
  }

  type Metadata[G[_]] = Inject[MetadataF, G]

  object Metadata {
    def apply[F[_]](implicit ev: MetadataAPI[Free[F, ?]]): MetadataAPI[Free[F, ?]] = ev
  }

  type Process[G[_]] = Inject[ProcessF, G]

  object Process {
    def apply[F[_]](implicit ev: ProcessAPI[Free[F, ?]]): ProcessAPI[Free[F, ?]] = ev
  }

  type ImgWriter[G[_]] = Inject[ImgWriterF, G]

  object ImgWriter {
    def apply[F[_]](implicit ev: ImgWriterAPI[Free[F, ?]]): ImgWriterAPI[Free[F, ?]] = ev
  }
}
