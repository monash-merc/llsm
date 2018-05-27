package llsm.ij

import cats.free.Free
import io.scif.img.SCIFIOImgPlus
import net.imglib2.`type`.numeric.integer.UnsignedShortType

sealed trait VisualiseIJF[A]
case class ShowHyper(img: SCIFIOImgPlus[UnsignedShortType])
    extends VisualiseIJF[Unit]
case class ShowBDV(img: SCIFIOImgPlus[UnsignedShortType])
    extends VisualiseIJF[Unit]

object VisualiseIJF {
  def showHyper(
      img: SCIFIOImgPlus[UnsignedShortType]): Free[VisualiseIJF, Unit] =
    Free.liftF(ShowHyper(img))
  def showBDV(img: SCIFIOImgPlus[UnsignedShortType]): Free[VisualiseIJF, Unit] =
    Free.liftF(ShowBDV(img))
}
