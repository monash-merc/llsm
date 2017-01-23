package llsm.io

import net.imglib2.img.Img
import net.imglib2.`type`.numeric.RealType
import _root_.io.scif.ImageMetadata

sealed trait ImgWriterF[A]
case class WriteImg[A <: RealType[A]](path: String, img: Img[A], meta: ImageMetadata)
