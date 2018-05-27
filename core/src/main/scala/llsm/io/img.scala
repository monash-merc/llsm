package llsm.io

import llsm.io.metadata.FileMetadata
import net.imglib2.img.Img
import net.imglib2.`type`.numeric.integer.UnsignedShortType

case class LLSMImg(img: Img[UnsignedShortType], meta: FileMetadata)

object LLSMImg {
  implicit val ordering = new Ordering[LLSMImg] {
    override def compare(x: LLSMImg, y: LLSMImg): Int =
      FileMetadata.compare(x.meta, y.meta)
  }
}

sealed trait ImgContainer
case object CellContainer   extends ImgContainer
case object PlanarContainer extends ImgContainer
case object ArrayContainer  extends ImgContainer

object ImgContainer {}
