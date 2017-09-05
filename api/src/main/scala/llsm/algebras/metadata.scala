package llsm.algebras

import java.nio.file.Path

import cats.free.{
  Free,
  FreeApplicative,
  Inject
}
import llsm.io.LLSMImg
import llsm.io.metadata.{
  ConfigurableMetadata,
  FilenameMetadata,
  FileMetadata,
  TextMetadata
}

trait MetadataAPI[F[_]] {
  def readMetadata(path: Path): F[FileMetadata]
  def writeMetadata(path: Path, metas: List[LLSMImg]): F[Unit]
}

sealed trait MetadataF[A]

object MetadataAPI {
  case class ReadMetadata[A](path: Path, next: FileMetadata => A) extends MetadataF[A]
  case class WriteMetadata[A](path: Path, metas: List[LLSMImg], next: Unit => A) extends MetadataF[A]

  implicit val metadata = new MetadataAPI[MetadataF] {
    def readMetadata(path: Path): MetadataF[FileMetadata] =
      ReadMetadata(path, identity)
    def writeMetadata(path: Path, metas: List[LLSMImg]): MetadataF[Unit] =
      WriteMetadata(path, metas, identity)
  }

  implicit def metadataInject[F[_], G[_]](implicit F: MetadataAPI[F], I: Inject[F, G]): MetadataAPI[Free[G, ?]] =
    new MetadataAPI[Free[G, ?]] {
      def readMetadata(path: Path): Free[G, FileMetadata] =
        Free.inject[F, G](F.readMetadata(path))
      def writeMetadata(path: Path, metas: List[LLSMImg]): Free[G, Unit] =
        Free.inject[F, G](F.writeMetadata(path, metas))
    }

}

sealed trait MetadataLowF[A]

object MetadataLow {
  case object ConfigurableMeta extends MetadataLowF[ConfigurableMetadata]
  case class ExtractFilenameMeta(path: Path) extends MetadataLowF[FilenameMetadata]
  case class ExtractTextMeta(path: Path) extends MetadataLowF[TextMetadata]
  case class WriteMetadata(path: Path, metas: List[LLSMImg]) extends MetadataLowF[Unit]

  def configurableMeta: FreeApplicative[MetadataLowF, ConfigurableMetadata] =
    FreeApplicative.lift(ConfigurableMeta)
  def extractFilenameMeta(path: Path): FreeApplicative[MetadataLowF, FilenameMetadata] =
    FreeApplicative.lift(ExtractFilenameMeta(path))
  def extractTextMeta(path: Path): FreeApplicative[MetadataLowF, TextMetadata] =
    FreeApplicative.lift(ExtractTextMeta(path))
  def writeMeta(path: Path, metas: List[LLSMImg]): FreeApplicative[MetadataLowF, Unit] =
    FreeApplicative.lift(WriteMetadata(path, metas))
}
