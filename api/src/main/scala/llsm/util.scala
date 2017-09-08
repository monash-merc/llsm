package llsm

import java.nio.file.{
  Files,
  Path,
  Paths
}
import java.util.UUID
import scala.collection.JavaConverters._
import scala.xml._
import scala.xml.transform._

import cats.ApplicativeError
import cats.implicits._
import _root_.io.scif.img.SCIFIOImgPlus
import _root_.io.scif.ome.services.OMEMetadataService
import llsm.io.LLSMImg
import llsm.io.metadata.{FileMetadata, MetadataUtils}
import llsm.interpreters.WriterUtils
import loci.formats.ome.OMEXMLMetadataImpl
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
  def aggregateImgs(lImgs: List[LLSMImg]): Option[SCIFIOImgPlus[UnsignedShortType]] = {
    val aggImg: Option[Img[UnsignedShortType]] = {
      val tStacks: Option[List[RandomAccessibleInterval[UnsignedShortType]]] =
        lImgs
          .groupBy(_.meta.filename.stack)
          .toSeq
          .sortBy(_._1)
          .map {
            case (_, cStack) => {
              val cGroups: List[RandomAccessibleInterval[UnsignedShortType]] =
                cStack.map(c => c.img)
              if (cGroups.size > 1)
                Some(Views.stack[UnsignedShortType](cGroups.asJava))
              else cGroups.headOption
            }
          }.toList.sequence
      tStacks.flatMap {
        ts => {
          val stack = if (ts.size > 1) Some(Views.stack[UnsignedShortType](ts.asJava))
                      else ts.headOption

          stack.map(s => ImgView.wrap(s, lImgs(0).img.factory))
        }
      }
    }
    for {
      meta <- MetadataUtils.createImageMetadata(lImgs)
      img <- aggImg
    } yield {
      val out = new SCIFIOImgPlus(img)
      out.setImageMetadata(meta)
      out
    }
  }

  /** Writes a companion OME metadata file for a List of processed LLSM Images.
   *
   * Takes a list of processed LLSMImgs and builds an OMEXML data
   * structure that describes the entire dataset.
   * @param path Path to the output file
   * @param imgs List of processed LLSMImgs
   * @param context SciJava context that is used for SCIFIO
   * @return Unit
   */
  def writeOMEMetadata[M[_]](
    path: Path,
    imgs: List[LLSMImg],
    context: Context
  )(
    implicit
    M: ApplicativeError[M, Throwable]
  ): M[Unit] = {
    val outPath: Path = path.getParent
    val (outName, outExt) = WriterUtils.splitExtension(path.getFileName.toString)
    val companionName = s"$outName.companion.ome"

    val omeService = context.getService(classOf[OMEMetadataService])

    val omeString: Option[String] = MetadataUtils.createImageMetadata(imgs).flatMap(meta => {
      val omexml = new OMEXMLMetadataImpl()
      omeService.populateMetadata(omexml, 0, companionName, meta)
      omexml.setUUID(UUID.nameUUIDFromBytes(outName.getBytes).toString)
      outExt match {
        case "ome.tif" => {
          imgs.sorted.foreach {
            case LLSMImg(_, FileMetadata(file, wave, _, _, _)) => {
              (0 until wave.nSlices.toInt).foreach {
                z => {
                  val i = ((file.stack * wave.channels.size * wave.nSlices) +
                           (file.channel * wave.nSlices) + z).toInt

                  omexml.setTiffDataFirstZ(new NonNegativeInteger(z), 0, i)
                  omexml.setTiffDataIFD(new NonNegativeInteger(z), 0, i)
                  omexml.setTiffDataFirstC(new NonNegativeInteger(file.channel), 0, i)
                  omexml.setTiffDataFirstT(new NonNegativeInteger(file.stack), 0, i)
                  omexml.setTiffDataPlaneCount(new NonNegativeInteger(1), 0, i)
                  omexml.setUUIDFileName(file.name, 0, i)
                  omexml.setUUIDValue(file.id.toString, 0, i)
                }
              }
            }
          }
          Some(omexml.dumpXML)
        }
        case _ => {
          val rewriteTransform = new RewriteRule {
            override def transform(n: Node): Seq[Node] = n match {
              case Elem(prefix, "Pixels", att, scope, child @ _*) =>
                Elem(prefix, "Pixels", att, scope, false, child ++ <MetadataOnly/> : _*)
              case other => other
            }
          }
          new RuleTransformer(rewriteTransform).transform(XML.loadString(omexml.dumpXML)).headOption.map(x => x.toString)
        }
      }
    })

    omeString match {
      case Some(ome) => {
        M.catchNonFatal {
          val _ = Files.write(Paths.get(outPath.toString, companionName), ome.getBytes)
        }
      }
      case None => M.raiseError(new Exception("Unable to create OMEXML companion metadata object."))
    }

  }
}
