package llsm.algebras

import cats.free.{Free, Inject}
import llsm.InterpolationMethod
import llsm.io.LLSMImg


trait ProcessAPI[F[_]] {
  def deskewImg(img: LLSMImg, shearDim: Int, refDim: Int, shearFactor: Double, interpolation: InterpolationMethod): F[LLSMImg]
}

sealed trait ProcessF[A]

object ProcessAPI {

  case class DeskewImg[A](img: LLSMImg,
                          shearDim: Int,
                          refDim: Int,
                          shearFactor: Double,
                          interpolation: InterpolationMethod,
                          next: LLSMImg => A) extends ProcessF[A]

  implicit val process = new ProcessAPI[ProcessF] {
    def deskewImg(img: LLSMImg, shearDim: Int, refDim: Int, shearFactor: Double, interpolation: InterpolationMethod): ProcessF[LLSMImg] =
      DeskewImg(img, shearDim, refDim, shearFactor, interpolation, identity)
  }

  implicit def processInject[F[_], G[_]](implicit F: ProcessAPI[F], I: Inject[F, G]): ProcessAPI[Free[G, ?]] =
    new ProcessAPI[Free[G, ?]] {
      def deskewImg(img: LLSMImg, shearDim: Int, refDim: Int, shearFactor: Double, interpolation: InterpolationMethod): Free[G, LLSMImg] =
        Free.inject[F, G](F.deskewImg(img, shearDim, refDim, shearFactor, interpolation))
    }
}
