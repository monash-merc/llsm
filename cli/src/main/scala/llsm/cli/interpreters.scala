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

  def consoleProgressInterpreter[M[_]](implicit
    M: MonadError[M, Throwable]
  ): ProgressF ~> M =
    new (ProgressF ~> M) {
      def apply[A](fa: ProgressF[A]): M[A] =
        fa match {
          case ProgressAPI.Progress(value: Int, max: Int) =>
            M.catchNonFatal(println(s"Progress ${(value + 1).toDouble  / max.toDouble * 100}%"))
          case ProgressAPI.Status(message: String) =>
            M.catchNonFatal(println(s"Status $message"))
        }
    }

  def consoleLoggingInterpreter[M[_]](
    debug: Boolean
  )(implicit
    M: MonadError[M, Throwable]
  ): LoggingF ~> M =
    new (LoggingF ~> M) {
      def apply[A](fa: LoggingF[A]): M[A] =
        fa match {
          case LoggingAPI.Info(msg) => M.catchNonFatal { if (debug) println(s"INFO: $msg") }
          case LoggingAPI.InfoCause(msg, cause) => M.catchNonFatal { if (debug) println(s"INFO: $msg.\n$cause") }
          case LoggingAPI.Warn(msg) => M.catchNonFatal { if (debug) println(s"WARN: $msg") }
          case LoggingAPI.WarnCause(msg, cause) => M.catchNonFatal { if (debug) println(s"WARN: $msg.\n$cause") }
          case LoggingAPI.Debug(msg) => M.catchNonFatal { if (debug) println(s"DEBUG: $msg") }
          case LoggingAPI.DebugCause(msg, cause) => M.catchNonFatal { if (debug) println(s"DEBUG: $msg.\n$cause") }
          case LoggingAPI.Error(msg) => M.catchNonFatal { if (debug) println(s"ERROR: $msg") }
          case LoggingAPI.ErrorCause(msg, cause) => M.catchNonFatal { if (debug) println(s"ERROR: $msg.\n$cause") }
          case LoggingAPI.Trace(msg) => M.catchNonFatal { if (debug) println(s"TRACE: $msg") }
          case LoggingAPI.TraceCause(msg, cause) => M.catchNonFatal { if (debug) println(s"TRACE: $msg.\n$cause") }
        }
    }


  def cliMetadataInterpreter[M[_]](
    config: ConfigurableMetadata,
    context: Context,
    debug: Boolean
  )(implicit
    M: MonadError[M, Throwable]
  ): MetadataF ~> M =
    new (MetadataF ~> M) {
      def apply[A](fa: MetadataF[A]): M[A] =
        for {
          _ <- metadataLogging(fa).unhalt.foldMap(consoleLoggingInterpreter[M](debug))
          m <- basicMetadataInterpreter[M](config, context)(M)(fa)
        } yield m
    }


  def cliImgReaderInterpreter[M[_]](
    context: Context,
    factory: ImgFactory[UnsignedShortType],
    debug: Boolean
  )(implicit
    M: MonadError[M, Throwable]
  ): ImgReaderF ~> M =
    new (ImgReaderF ~> M) {
      def apply[A](fa: ImgReaderF[A]): M[A] =
        for {
          _ <- readerLogging(fa).unhalt.foldMap(consoleLoggingInterpreter[M](debug))
          m <- scifioReaderInterpreter[M](context, factory)(M)(fa)
        } yield m
    }
}
