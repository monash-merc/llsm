package llsm.ij

import cats._
import cats.implicits._
import llsm.algebras.{ImgReaderF, LoggingAPI, LoggingF, MetadataF, ProgressAPI, ProgressF}
import llsm.fp._
import llsm.interpreters._
import llsm.io.metadata.ConfigurableMetadata
import org.scijava.Context
import org.scijava.app.StatusService
import org.scijava.log.LogService

package object interpreters {

  def ijProgress[M[_]](status: StatusService)(implicit M: MonadError[M, Throwable]): ProgressF ~> M =
    new (ProgressF ~> M) {
      def apply[A](fa: ProgressF[A]): M[A] =
        fa match {
          case ProgressAPI.Progress(value: Int, max: Int) =>
            M.catchNonFatal(status.showProgress(value, max))
          case ProgressAPI.Status(message: String) =>
            M.catchNonFatal(status.showStatus(message))
        }
    }

  def ijLogging[M[_]](log: LogService)(implicit M: MonadError[M, Throwable]): LoggingF ~> M =
    new (LoggingF ~> M) {
      def apply[A](fa: LoggingF[A]): M[A] =
        fa match {
          case LoggingAPI.Info(msg) => M.catchNonFatal(log.info(msg))
          case LoggingAPI.InfoCause(msg, cause) => M.catchNonFatal(log.info(msg, cause))
          case LoggingAPI.Warn(msg) => M.catchNonFatal(log.warn(msg))
          case LoggingAPI.WarnCause(msg, cause) => M.catchNonFatal(log.warn(msg, cause))
          case LoggingAPI.Debug(msg) => M.catchNonFatal(log.debug(msg))
          case LoggingAPI.DebugCause(msg, cause) => M.catchNonFatal(log.debug(msg, cause))
          case LoggingAPI.Error(msg) => M.catchNonFatal(log.error(msg))
          case LoggingAPI.ErrorCause(msg, cause) => M.catchNonFatal(log.error(msg, cause))
          case LoggingAPI.Trace(msg) => M.catchNonFatal(log.trace(msg))
          case LoggingAPI.TraceCause(msg, cause) => M.catchNonFatal(log.trace(msg, cause))
        }
    }



  def ijMetadataReader[M[_]](
    config: ConfigurableMetadata,
    log: LogService
  )(implicit
    M: MonadError[M, Throwable]
  ): MetadataF ~> M =
    new (MetadataF ~> M) {
      def apply[A](fa: MetadataF[A]): M[A] =
        for {
          _ <- metadataLogging(fa).unhalt.foldMap(ijLogging[M](log))
          m <- basicMetadataReader[M](config)(M)(fa)
        } yield m
    }


  def ijImgReader[M[_]](context: Context, log: LogService)(implicit M: MonadError[M, Throwable]): ImgReaderF ~> M =
    new (ImgReaderF ~> M) {
      def apply[A](fa: ImgReaderF[A]): M[A] =
        for {
          _ <- readerLogging(fa).unhalt.foldMap(ijLogging[M](log))
          m <- scifioReader[M](context)(M)(fa)
        } yield m
    }
}
