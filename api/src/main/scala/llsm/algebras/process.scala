package llsm.algebras

import cats.InjectK
import cats.free.Free
import io.scif.img.SCIFIOImgPlus
import llsm.InterpolationMethod
import llsm.io.LLSMImg
import net.imglib2.`type`.numeric.integer.UnsignedShortType


trait ProcessAPI[F[_]] {
  def aggregate(imgs: List[LLSMImg]): F[SCIFIOImgPlus[UnsignedShortType]]
  def deskewImg(img: LLSMImg, shearDim: Int, refDim: Int, shearFactor: Double,
                interpolation: InterpolationMethod): F[LLSMImg]
}

sealed trait ProcessF[A]

object ProcessAPI {

  case class Aggregate[A](imgs: List[LLSMImg],
                          next: SCIFIOImgPlus[UnsignedShortType] => A) extends ProcessF[A]
  case class DeskewImg[A](img: LLSMImg,
                          shearDim: Int,
                          refDim: Int,
                          shearFactor: Double,
                          interpolation: InterpolationMethod,
                          next: LLSMImg => A) extends ProcessF[A]

  implicit val process = new ProcessAPI[ProcessF] {
    def aggregate(imgs: List[LLSMImg]): ProcessF[SCIFIOImgPlus[UnsignedShortType]] =
      Aggregate(imgs, identity)
    def deskewImg(img: LLSMImg, shearDim: Int, refDim: Int, shearFactor: Double, interpolation: InterpolationMethod): ProcessF[LLSMImg] =
      DeskewImg(img, shearDim, refDim, shearFactor, interpolation, identity)
  }

  implicit def processInject[F[_], G[_]](implicit F: ProcessAPI[F], I: InjectK[F, G]): ProcessAPI[Free[G, ?]] =
    new ProcessAPI[Free[G, ?]] {
      def aggregate(imgs: List[LLSMImg]): Free[G, SCIFIOImgPlus[UnsignedShortType]] =
        Free.inject[F, G](F.aggregate(imgs))
      def deskewImg(img: LLSMImg, shearDim: Int, refDim: Int, shearFactor: Double, interpolation: InterpolationMethod): Free[G, LLSMImg] =
        Free.inject[F, G](F.deskewImg(img, shearDim, refDim, shearFactor, interpolation))
    }
}
