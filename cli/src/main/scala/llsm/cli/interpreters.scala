package llsm.cli

import cats._
import cats.implicits._
import llsm.algebras.{
  ImgReaderF,
  LoggingAPI,
  LoggingF,
  MetadataF,
  ProgressAPI,
  ProgressF
}
import llsm.fp._
import llsm.interpreters._
import llsm.io.metadata.ConfigurableMetadata
import net.imglib2.img.ImgFactory
import net.imglib2.`type`.numeric.integer.UnsignedShortType
import org.scijava.Context

object Interpreters {

  def consoleProgress[M[_]](implicit
    M: MonadError[M, Throwable]
  ): ProgressF ~> M =
    new (ProgressF ~> M) {
      def apply[A](fa: ProgressF[A]): M[A] =
        fa match {
          case ProgressAPI.Progress(value: Int, max: Int) =>
            M.catchNonFatal(println(s"Progress ${value/max*100}%"))
          case ProgressAPI.Status(message: String) =>
            M.catchNonFatal(println(s"Status $message"))
        }
    }

  def consoleLogging[M[_]](implicit
    M: MonadError[M, Throwable]
  ): LoggingF ~> M =
    new (LoggingF ~> M) {
      def apply[A](fa: LoggingF[A]): M[A] =
        fa match {
          case LoggingAPI.Info(msg) => M.catchNonFatal(println(s"INFO: $msg"))
          case LoggingAPI.InfoCause(msg, cause) => M.catchNonFatal(println(s"INFO: $msg.\n$cause"))
          case LoggingAPI.Warn(msg) => M.catchNonFatal(println(s"WARN: $msg"))
          case LoggingAPI.WarnCause(msg, cause) => M.catchNonFatal(println(s"WARN: $msg.\n$cause"))
          case LoggingAPI.Debug(msg) => M.catchNonFatal(println(s"DEBUG: $msg"))
          case LoggingAPI.DebugCause(msg, cause) => M.catchNonFatal(println(s"DEBUG: $msg.\n$cause"))
          case LoggingAPI.Error(msg) => M.catchNonFatal(println(s"ERROR: $msg"))
          case LoggingAPI.ErrorCause(msg, cause) => M.catchNonFatal(println(s"ERROR: $msg.\n$cause"))
          case LoggingAPI.Trace(msg) => M.catchNonFatal(println(s"TRACE: $msg"))
          case LoggingAPI.TraceCause(msg, cause) => M.catchNonFatal(println(s"TRACE: $msg.\n$cause"))
        }
    }


  def cliMetadataReader[M[_]](
    config: ConfigurableMetadata,
    context: Context
  )(implicit
    M: MonadError[M, Throwable]
  ): MetadataF ~> M =
    new (MetadataF ~> M) {
      def apply[A](fa: MetadataF[A]): M[A] =
        for {
          _ <- metadataLogging(fa).unhalt.foldMap(consoleLogging[M])
          m <- basicMetadataReader[M](config, context)(M)(fa)
        } yield m
    }


  def cliImgReader[M[_]](
    context: Context,
    factory: ImgFactory[UnsignedShortType]
  )(implicit
    M: MonadError[M, Throwable]
  ): ImgReaderF ~> M =
    new (ImgReaderF ~> M) {
      def apply[A](fa: ImgReaderF[A]): M[A] =
        for {
          _ <- readerLogging(fa).unhalt.foldMap(consoleLogging[M])
          m <- scifioReader[M](context, factory)(M)(fa)
        } yield m
    }
}
