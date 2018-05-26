package llsm

import cats.InjectK
import cats.free.Free

package object algebras {

  type Progress[G[_]] = InjectK[ProgressF, G]

  object Progress {
    def apply[F[_]](implicit ev: ProgressAPI[Free[F, ?]]): ProgressAPI[Free[F, ?]] = ev
  }

  type ImgReader[G[_]] = InjectK[ImgReaderF, G]

  object ImgReader {
    def apply[F[_]](implicit ev: ImgReaderAPI[Free[F, ?]]): ImgReaderAPI[Free[F, ?]] = ev
  }

  type Metadata[G[_]] = InjectK[MetadataF, G]

  object Metadata {
    def apply[F[_]](implicit ev: MetadataAPI[Free[F, ?]]): MetadataAPI[Free[F, ?]] = ev
  }

  type Process[G[_]] = InjectK[ProcessF, G]

  object Process {
    def apply[F[_]](implicit ev: ProcessAPI[Free[F, ?]]): ProcessAPI[Free[F, ?]] = ev
  }

  type ImgWriter[G[_]] = InjectK[ImgWriterF, G]

  object ImgWriter {
    def apply[F[_]](implicit ev: ImgWriterAPI[Free[F, ?]]): ImgWriterAPI[Free[F, ?]] = ev
  }

  type Visualise[G[_]] = InjectK[VisualiseF, G]

  object Visualise {
    def apply[F[_]](implicit ev: VisualiseAPI[Free[F, ?]]): VisualiseAPI[Free[F, ?]] = ev
  }
}
