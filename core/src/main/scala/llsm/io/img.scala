package llsm.io

import scala.collection.mutable.ListMap
import scala.collection.JavaConverters._

import io.scif.ImageMetadata
import llsm.io.metadata.{LLSMMeta, AggregatedMeta, FileMetadata, MetadataUtils}
import net.imglib2.RandomAccessibleInterval
import net.imglib2.img.{Img, ImgView}
import net.imglib2.`type`.numeric.integer.UnsignedShortType
import net.imglib2.view.Views

sealed trait LLSMImgs
case class LLSMStack(img: Img[UnsignedShortType], meta: FileMetadata) extends LLSMImgs
case class LLSMImg(img: Img[UnsignedShortType], meta: AggregatedMeta) extends LLSMImgs

object LLSMStack {
  implicit val ordering = new Ordering[LLSMStack] {
    override def compare(x: LLSMStack, y: LLSMStack): Int = LLSMMeta.compare(x.meta, y.meta)
  }
}

object ImgUtils {

  /** Generate a 5D image from a list of Imgs
   *
   * Takes a list of stacks and aggregates them into a 4-5D image. Metaddata in
   * each 3D stack is used to order the images by timepoint and then channel
   * resulting a 5D image of XYZCT order. Importantly, singleton dimensions are
   * dropped.
   *
   * @param imgs List of 3D stacks
   * @return 5D image and metadata
   */
  def aggregateImgs(lImgs: List[LLSMStack]): LLSMImg = {
    val aggImg: Img[UnsignedShortType] = {
      val tStacks: List[RandomAccessibleInterval[UnsignedShortType]] =
        ListMap(
          lImgs
            .groupBy(_.meta.filename.timeStamp)
            .toSeq
            .sortBy(_._1): _*).values
          .map(cStacks => {
            val cGroups: List[RandomAccessibleInterval[UnsignedShortType]] =
              cStacks.map(c => c.img)
            if (cGroups.size > 1)
              Views.stack[UnsignedShortType](cGroups.asJava)
            else cGroups.head
          })
          .toList
      ImgView.wrap(
        if (tStacks.size > 1) Views.stack[UnsignedShortType](tStacks.asJava)
        else tStacks.head,
        lImgs(0).img.factory
      )
    }

    val meta: ImageMetadata = MetadataUtils.convertMetadata(lImgs.map(_.meta))

    LLSMImg(aggImg, AggregatedMeta(meta))
  }
}
