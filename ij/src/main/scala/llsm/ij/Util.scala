package llsm.ij

import java.nio.file.{
  Files,
  Path,
  Paths
}
import java.util.UUID
import scala.collection.JavaConverters._
import scala.xml._
import scala.xml.transform._

import io.scif.{ ImageMetadata, SCIFIO }
import io.scif.img.SCIFIOImgPlus
import io.scif.ome.services.{ OMEMetadataService, OMEXMLService }
import llsm.io.LLSMImg
import llsm.io.metadata.{FileMetadata, MetadataUtils}
import llsm.interpreters.WriterUtils
import net.imglib2.RandomAccessibleInterval
import net.imglib2.img.{Img, ImgView}
import net.imglib2.`type`.numeric.integer.UnsignedShortType
import net.imglib2.view.Views
import ome.xml.model.primitives.NonNegativeInteger
import org.scijava.Context


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
  def aggregateImgs(lImgs: List[LLSMImg]): SCIFIOImgPlus[UnsignedShortType] = {
    val aggImg: Img[UnsignedShortType] = {
      val tStacks: List[RandomAccessibleInterval[UnsignedShortType]] =
        lImgs
          .groupBy(_.meta.filename.stack)
          .toSeq
          .sortBy(_._1)
          .map {
            case (_, cStack) => {
              val cGroups: List[RandomAccessibleInterval[UnsignedShortType]] =
                cStack.map(c => c.img)
              if (cGroups.size > 1)
                Views.stack[UnsignedShortType](cGroups.asJava)
              else cGroups.head
            }
          }.toList
      ImgView.wrap(
        if (tStacks.size > 1) Views.stack[UnsignedShortType](tStacks.asJava)
        else tStacks.head,
        lImgs(0).img.factory
      )
    }

    val meta: ImageMetadata = MetadataUtils.createImageMetadata(lImgs)

    val out = new SCIFIOImgPlus(aggImg)
    out.setImageMetadata(meta)
    out
  }

  /** Writes a companion OME metadata file for a List of processed LLSM Images.
   *  
   * Takes a list of processed [[llsm.io.LLSMImg]]s and builds an OMEXML data 
   * structure that describes the entire dataset.
   * @param path Path to the output file
   * @param imgs List of processed LLSMImgs
   * @param context SciJava context that is used for SCIFIO
   * @return Unit
   */
  def writeOMEMetadata(path: Path, imgs: List[LLSMImg], context: Context): Unit  = {
    val outPath: Path = path.getParent
    val (outName, outExt) = WriterUtils.splitExtension(path.getFileName.toString)
    val companionName = s"$outName.companion.ome"

    val scifio: SCIFIO = new SCIFIO(context)
    val omeService = context.getService(classOf[OMEMetadataService])
    val omexmlService = context.getService(classOf[OMEXMLService])

    val omexml = omexmlService.createOMEXMLMetadata
    omeService.populateMetadata(omexml, 0, companionName, MetadataUtils.createImageMetadata(imgs))
    omexml.setUUID(UUID.nameUUIDFromBytes(outName.getBytes).toString)
    
    val omeString: Option[String] = outExt match {
      case ".ome.tif" => {
        imgs.foreach {
          case LLSMImg(_, FileMetadata(file, wave, _, _, _, _)) => {
            omexml.setTiffDataFirstC(
              new NonNegativeInteger(file.channel),
              0,
              file.channel * wave.nFrames + file.stack
            )
            omexml.setTiffDataFirstT(
              new NonNegativeInteger(file.stack),
              0,
              file.channel * wave.nFrames + file.stack
            )
            omexml.setTiffDataPlaneCount(
              new NonNegativeInteger(wave.nSlices.toInt),
              0,
              file.channel * wave.nFrames + file.stack
            )
            omexml.setUUIDFileName(
              file.name,
              0,
              file.channel * wave.nFrames + file.stack
            )
            omexml.setUUIDValue(
              file.id.toString,
              0,
              file.channel * wave.nFrames + file.stack
           
            )
          }
        }
        Some(omexml.dumpXML)
      }
      case _ => {
        val rewriteTransform = new RewriteRule {
          override def transform(n: Node): Seq[Node] = n match {
            case e @ Elem(prefix, "Pixels", att, scope, child @ _*) =>
              Elem(prefix, "Pixels", att, scope, false, child ++ <MetadataOnly/> : _*)
            case other => other
          }
        }
        new RuleTransformer(rewriteTransform).transform(XML.loadString(omexml.dumpXML)).headOption.map(x => x.toString)
      }
    }
    
    omeString match {
      case Some(ome) => {
        Files.write(Paths.get(outPath.toString, companionName), ome.getBytes)
        ()
      }
      case None => ()
    }
    
  }
}