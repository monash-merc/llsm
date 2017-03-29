package llsm.algebras

import cats.free.{Free, Inject}

trait ConfigAPI[F[_]] {
  def get[A](key: String): F[Option[A]]
  def put[A](key: String, value: A): F[Unit]
  def delete(key: String): F[Unit]
  def update[A](key: String, f: A => A): F[Unit]
}

sealed trait ConfigF[A]
case class Get[A, T](key: String, next: Option[T] => A) extends ConfigF[A]
case class Put[A, T](key: String, value: T, next: Unit => A) extends ConfigF[A]
case class Delete[A](key: String, next: Unit => A) extends ConfigF[A]

object ConfigAPI {
  def apply[F[_]](implicit ev: ConfigAPI[Free[F, ?]]): ConfigAPI[Free[F, ?]] = ev

  implicit val config = new ConfigAPI[ConfigF] {
    def get[A](key: String): ConfigF[Option[A]] = Get[Option[A], A](key, identity)
    def put[A](key: String, value: A): ConfigF[Unit] = Put(key, value, identity)
    def delete(key: String): ConfigF[Unit] = Delete(key, identity)
    def update[A](key: String, f: A => A): ConfigF[Unit] =
      Get(key, (a: Option[A]) => a match {
        case Some(a) => { put[A](key, f(a)); ()}
        case None => ()
      })
  }

  implicit def configInject[F[_], G[_]](implicit F: ConfigAPI[F], I: Inject[F, G]): ConfigAPI[Free[G, ?]] =
    new ConfigAPI[Free[G, ?]] {
      def get[A](key: String): Free[G, Option[A]] = Free.inject[F, G](F.get[A](key))
      def put[A](key: String, value: A): Free[G, Unit] = Free.inject[F, G](F.put[A](key, value))
      def delete(key: String): Free[G, Unit] = Free.inject[F, G](F.delete(key))
      def update[A](key: String, f: A => A): Free[G, Unit] = Free.inject[F, G](F.update(key, f))
    }
}
