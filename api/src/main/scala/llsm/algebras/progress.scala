package llsm.algebras

import cats.InjectK
import cats.free.Free

trait ProgressAPI[F[_]] {
  def progress(value: Int, max: Int): F[Unit]
  def status(message: String): F[Unit]
}

sealed trait ProgressF[A]

object ProgressAPI {

  case class Progress(value: Int, max: Int) extends ProgressF[Unit]
  case class Status(message: String) extends ProgressF[Unit]

  implicit val progress = new ProgressAPI[ProgressF] {
    def progress(value: Int, max: Int): ProgressF[Unit] =
      Progress(value, max)
    def status(message: String): ProgressF[Unit] =
      Status(message)
  }

  implicit def progressInject[F[_], G[_]](
    implicit
    F: ProgressAPI[F],
    I: InjectK[F, G]
  ): ProgressAPI[Free[G, ?]] =
    new ProgressAPI[Free[G, ?]] {
      def progress(value: Int, max: Int): Free[G, Unit] =
        Free.inject[F, G](F.progress(value, max))
      def status(message: String): Free[G, Unit] =
        Free.inject[F, G](F.status(message))
    }
}

