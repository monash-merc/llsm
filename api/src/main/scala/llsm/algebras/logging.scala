package llsm.algebras

import cats.InjectK
import cats.free.Free

trait LoggingAPI[F[_]] {
  def info(msg: String): F[Unit]
  def infoCause(msg: String, cause: Throwable): F[Unit]
  def warn(msg: String): F[Unit]
  def warnCause(msg: String, cause: Throwable): F[Unit]
  def debug(msg: String): F[Unit]
  def debugCause(msg: String, cause: Throwable): F[Unit]
  def error(msg: String): F[Unit]
  def errorCause(msg: String, cause: Throwable): F[Unit]
  def trace(msg: String): F[Unit]
  def traceCause(msg: String, cause: Throwable): F[Unit]
}

sealed trait LoggingF[A]

object LoggingAPI {
  def apply[F[_]](implicit ev: LoggingAPI[F]): LoggingAPI[F] = ev

  case class Info(msg: String)                         extends LoggingF[Unit]
  case class InfoCause(msg: String, cause: Throwable)  extends LoggingF[Unit]
  case class Warn(msg: String)                         extends LoggingF[Unit]
  case class WarnCause(msg: String, cause: Throwable)  extends LoggingF[Unit]
  case class Debug(msg: String)                        extends LoggingF[Unit]
  case class DebugCause(msg: String, cause: Throwable) extends LoggingF[Unit]
  case class Error(msg: String)                        extends LoggingF[Unit]
  case class ErrorCause(msg: String, cause: Throwable) extends LoggingF[Unit]
  case class Trace(msg: String)                        extends LoggingF[Unit]
  case class TraceCause(msg: String, cause: Throwable) extends LoggingF[Unit]

  implicit val logging = new LoggingAPI[LoggingF] {
    def info(msg: String): LoggingF[Unit] = LoggingAPI.Info(msg)
    def infoCause(msg: String, cause: Throwable): LoggingF[Unit] =
      LoggingAPI.InfoCause(msg, cause)
    def warn(msg: String): LoggingF[Unit] = LoggingAPI.Warn(msg)
    def warnCause(msg: String, cause: Throwable): LoggingF[Unit] =
      LoggingAPI.WarnCause(msg, cause)
    def debug(msg: String): LoggingF[Unit] = LoggingAPI.Debug(msg)
    def debugCause(msg: String, cause: Throwable): LoggingF[Unit] =
      LoggingAPI.DebugCause(msg, cause)
    def error(msg: String): LoggingF[Unit] = LoggingAPI.Error(msg)
    def errorCause(msg: String, cause: Throwable): LoggingF[Unit] =
      LoggingAPI.ErrorCause(msg, cause)
    def trace(msg: String): LoggingF[Unit] = LoggingAPI.Trace(msg)
    def traceCause(msg: String, cause: Throwable): LoggingF[Unit] =
      LoggingAPI.TraceCause(msg, cause)
  }

  implicit def loggingInject[F[_], G[_]](
      implicit F: LoggingAPI[F],
      I: InjectK[F, G]): LoggingAPI[Free[G, ?]] =
    new LoggingAPI[Free[G, ?]] {
      def info(msg: String): Free[G, Unit] = Free.inject[F, G](F.info(msg))
      def infoCause(msg: String, cause: Throwable): Free[G, Unit] =
        Free.inject[F, G](F.infoCause(msg, cause))
      def warn(msg: String): Free[G, Unit] = Free.inject[F, G](F.warn(msg))
      def warnCause(msg: String, cause: Throwable): Free[G, Unit] =
        Free.inject[F, G](F.warnCause(msg, cause))
      def debug(msg: String): Free[G, Unit] = Free.inject[F, G](F.debug(msg))
      def debugCause(msg: String, cause: Throwable): Free[G, Unit] =
        Free.inject[F, G](F.debugCause(msg, cause))
      def error(msg: String): Free[G, Unit] = Free.inject[F, G](F.error(msg))
      def errorCause(msg: String, cause: Throwable): Free[G, Unit] =
        Free.inject[F, G](F.errorCause(msg, cause))
      def trace(msg: String): Free[G, Unit] = Free.inject[F, G](F.trace(msg))
      def traceCause(msg: String, cause: Throwable): Free[G, Unit] =
        Free.inject[F, G](F.traceCause(msg, cause))
    }
}
