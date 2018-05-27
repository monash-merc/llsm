package llsm.algebras

import cats.InjectK
import cats.free.Free
import io.scif.img.SCIFIOImgPlus
import net.imglib2.`type`.numeric.integer.UnsignedShortType

trait VisualiseAPI[F[_]] {
  def show(img: SCIFIOImgPlus[UnsignedShortType]): F[Unit]
}

sealed trait VisualiseF[A]

object VisualiseAPI {
  case class Show[A](img: SCIFIOImgPlus[UnsignedShortType], next: Unit => A)
      extends VisualiseF[A]

  implicit val visualise = new VisualiseAPI[VisualiseF] {
    def show(img: SCIFIOImgPlus[UnsignedShortType]): VisualiseF[Unit] =
      Show(img, identity)
  }

  implicit def visualiseInject[F[_], G[_]](
      implicit F: VisualiseAPI[F],
      I: InjectK[F, G]): VisualiseAPI[Free[G, ?]] =
    new VisualiseAPI[Free[G, ?]] {
      def show(img: SCIFIOImgPlus[UnsignedShortType]): Free[G, Unit] =
        Free.inject[F, G](F.show(img))
    }
}
