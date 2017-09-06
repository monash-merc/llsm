package llsm.io.metadata

import java.nio.file.{Path, Paths}
import java.util.UUID

import cats._
import cats.arrow.FunctionK
import cats.data.Kleisli
import llsm.{BaseSuite, NoInterpolation}
import llsm.algebras.{MetadataF, MetadataLow, MetadataLowF}
import llsm.interpreters._

class MetadataSuite extends BaseSuite with MetadataImplicits {

  type Exec[M[_], A] = Kleisli[M, Unit, A]

  private def metaLowMockInterpreter[M[_]](
    s: Int,
    c: Int,
    t: Int
  )(
    implicit
    M: ApplicativeError[M, Throwable]
  ): FunctionK[MetadataLowF, Exec[M, ?]] = λ[FunctionK[MetadataLowF, Exec[M, ?]]] {
    fa => Kleisli { _ =>
      fa match {
        case MetadataLow.ConfigurableMeta => M.pure(ConfigurableMetadata(0.1018, 0.1018, NoInterpolation))
        case MetadataLow.ExtractFilenameMeta(path@_) => M.pure(FilenameMetadata(UUID.randomUUID, "Test", s, c, 488, t.toLong * 200L, 0L))
        case MetadataLow.ExtractTextMeta(path@_) => {
          val f: Path = Paths.get(
            getClass
              .getResource("/io/data/Resolution test 4_Settings.txt")
              .toURI
              .getPath)

          val meta = FileMetadata.readMetadataFromTxtFile(f)
          meta.leftMap {
            case FileMetadata.MetadataIOError(msg) => new Throwable(msg)
          } match {
            case Right(m) => M.pure(m)
            case Left(e) => M.raiseError(e)
          }
        }
        case MetadataLow.WriteMetadata(path@_, meta@_) => ???
      }
    }
  }

  def metaMockInterpreter[M[_]](s: Int, c: Int, t: Int)(implicit M: ApplicativeError[M, Throwable]): MetadataF ~> M =
    new (MetadataF ~> M) {
      def apply[A](fa: MetadataF[A]): M[A] =
        metaToMetaLowTranslator(fa)
          .foldMap[Exec[M, ?]](metaLowMockInterpreter[M](s, c, t)(M))
          .run(())
    }
}
