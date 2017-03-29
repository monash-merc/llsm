package llsm.algebras

import java.nio.file.Path

import cats.free.{Free, FreeApplicative, Inject}
import llsm.io.metadata.{ConfigurableMetadata, FilenameMetadata, ImgMetadata, FileMetadata, TextMetadata}

trait MetadataAPI[F[_]] {
  def readMetadata(path: Path): F[FileMetadata]
  def writeMetadata(path: Path, meta: FileMetadata): F[Unit]
}

sealed trait MetadataF[A]

object MetadataAPI {
  case class ReadMetadata[A](path: Path, next: FileMetadata => A) extends MetadataF[A]
  case class WriteMetadata[A](path: Path, meta: FileMetadata, next: Unit => A) extends MetadataF[A]

  implicit val metadata = new MetadataAPI[MetadataF] {
    def readMetadata(path: Path): MetadataF[FileMetadata] =
      ReadMetadata(path, identity)
    def writeMetadata(path: Path, meta: FileMetadata): MetadataF[Unit] =
      WriteMetadata(path, meta, identity)
  }

  implicit def metadataInject[F[_], G[_]](implicit F: MetadataAPI[F], I: Inject[F, G]): MetadataAPI[Free[G, ?]] =
    new MetadataAPI[Free[G, ?]] {
      def readMetadata(path: Path): Free[G, FileMetadata] =
        Free.inject[F, G](F.readMetadata(path))
      def writeMetadata(path: Path, meta: FileMetadata): Free[G, Unit] =
        Free.inject[F, G](F.writeMetadata(path, meta))
    }
}

sealed trait MetadataLowF[A]

object MetadataLow {
  case object ConfigurableMeta extends MetadataLowF[ConfigurableMetadata]
  case class ExtractBasicImageMeta(path: Path) extends MetadataLowF[Option[ImgMetadata]]
  case class ExtractFilenameMeta(path: Path) extends MetadataLowF[FilenameMetadata]
  case class ExtractTextMeta(path: Path) extends MetadataLowF[TextMetadata]
  case class WriteMetadata(path: Path, meta: FileMetadata) extends MetadataLowF[Unit]

  def configurableMeta: FreeApplicative[MetadataLowF, ConfigurableMetadata] =
    FreeApplicative.lift(ConfigurableMeta)
  def extractImgMeta(path: Path): FreeApplicative[MetadataLowF, Option[ImgMetadata]] =
    FreeApplicative.lift(ExtractBasicImageMeta(path))
  def extractFilenameMeta(path: Path): FreeApplicative[MetadataLowF, FilenameMetadata] =
    FreeApplicative.lift(ExtractFilenameMeta(path))
  def extractTextMeta(path: Path): FreeApplicative[MetadataLowF, TextMetadata] =
    FreeApplicative.lift(ExtractTextMeta(path))
  def writeMeta(path: Path, meta: FileMetadata): FreeApplicative[MetadataLowF, Unit] =
    FreeApplicative.lift(WriteMetadata(path, meta))
}
