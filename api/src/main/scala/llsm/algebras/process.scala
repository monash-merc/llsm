package llsm.algebras

import cats.free.{Free, Inject}
import llsm.InterpolationMethod
import llsm.io.{LLSMImg, LLSMStack}


trait ProcessAPI[F[_]] {
  def aggregateImgs(imgs: List[LLSMStack]): F[LLSMImg]
  def deskewImg(img: LLSMStack, shearDim: Int, refDim: Int, shearFactor: Double, interpolation: InterpolationMethod): F[LLSMStack]
  def deconvolve(img: LLSMStack, psf: LLSMStack, maxIterations: Int): F[LLSMStack]
}

sealed trait ProcessF[A]

object ProcessAPI {

  case class AggregateImgs[A](imgs: List[LLSMStack], next: LLSMImg => A) extends ProcessF[A]
  case class DeskewImg[A](img: LLSMStack,
                          shearDim: Int,
                          refDim: Int,
                          shearFactor: Double,
                          interpolation: InterpolationMethod,
                          next: LLSMStack => A) extends ProcessF[A]
  case class Deconvolve[A](img: LLSMStack, psf: LLSMStack, maxIterations: Int, next: LLSMStack => A) extends ProcessF[A]

  implicit val process = new ProcessAPI[ProcessF] {
    def aggregateImgs(imgs: List[LLSMStack]): ProcessF[LLSMImg] =
      AggregateImgs(imgs, identity)
    def deskewImg(img: LLSMStack, shearDim: Int, refDim: Int, shearFactor: Double, interpolation: InterpolationMethod): ProcessF[LLSMStack] =
      DeskewImg(img, shearDim, refDim, shearFactor, interpolation, identity)
    def deconvolve(img: LLSMStack, psf: LLSMStack, maxIterations: Int): ProcessF[LLSMStack] = Deconvolve(img, psf, maxIterations, identity)
  }

  implicit def processInject[F[_], G[_]](implicit F: ProcessAPI[F], I: Inject[F, G]): ProcessAPI[Free[G, ?]] =
    new ProcessAPI[Free[G, ?]] {
      def aggregateImgs(imgs: List[LLSMStack]): Free[G, LLSMImg] =
        Free.inject[F, G](F.aggregateImgs(imgs))
      def deskewImg(img: LLSMStack, shearDim: Int, refDim: Int, shearFactor: Double, interpolation: InterpolationMethod): Free[G, LLSMStack] =
        Free.inject[F, G](F.deskewImg(img, shearDim, refDim, shearFactor, interpolation))
      def deconvolve(img: LLSMStack, psf: LLSMStack, maxIterations: Int): Free[G, LLSMStack] =
        Free.inject[F, G](F.deconvolve(img, psf, maxIterations))
    }
}
