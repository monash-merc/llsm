package llsm.io.metadata

import java.nio.file.{Path, Paths}
import java.util.UUID

import cats._
import cats.arrow.FunctionK
import cats.data.Kleisli
import llsm.{BaseSuite, NoInterpolation}
import llsm.algebras.{MetadataF, MetadataLow, MetadataLowF}
import llsm.interpreters._
import _root_.io.scif.SCIFIO

import scala.util.{Failure, Success, Try}

class MetadataSuite extends BaseSuite with MetadataImplicits {

  type Exec[M[_], A] = Kleisli[M, Unit, A]

  private def metaLowMockCompiler[M[_]](s: Int, c: Int, t: Int)(implicit M: ApplicativeError[M, Throwable]) = λ[FunctionK[MetadataLowF, Exec[M, ?]]] {
    fa => Kleisli { _ =>
      fa match {
        case MetadataLow.ConfigurableMeta => M.pure(ConfigurableMetadata(0.1018, 0.1018, NoInterpolation))
        case MetadataLow.ExtractFilenameMeta(path) => M.pure(FilenameMetadata(UUID.randomUUID, "Test", s, c, 488, t.toLong * 200L, 0L))
        case MetadataLow.ExtractTextMeta(path) => {
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
        case MetadataLow.ExtractBasicImageMeta(path) => Try {
          val scifio = new SCIFIO
          val format = scifio.format().getFormat(path.toString)
          format.createParser().parse(path.toString)
        } match {
          case Success(meta) => {
            val im = meta.get(0)
            val cals = Array[Double](0.1018, 0.1018, 1)
            M.pure(Some(ImgMetadata(im.getAxesLengths, cals)))
          }
          case Failure(e) => M.raiseError(e)
        }
        case MetadataLow.WriteMetadata(path, meta) => ???
      }
    }
  }

  def metaMockCompiler[M[_]](s: Int, c: Int, t: Int)(implicit M: ApplicativeError[M, Throwable]): MetadataF ~> M =
    new (MetadataF ~> M) {
      def apply[A](fa: MetadataF[A]): M[A] =
        metaFToMetaLowF(fa).foldMap(metaLowMockCompiler[M](s, c, t)(M)).run(())

    }
}
