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
import llsm.io.metadata.{
  FileMetadata,
  MetadataUtils,
  WaveformMetadata
}
import llsm.interpreters.WriterUtils
import loci.formats.ome.OMEXMLMetadataImpl
import net.imglib2.RandomAccessibleInterval
import net.imglib2.img.{Img, ImgView}
import net.imglib2.`type`.numeric.integer.UnsignedShortType
import net.imglib2.view.Views
import ome.units.UNITS
import ome.units.quantity.{
  Length,
  Time
}
import ome.xml.meta.MetadataStore
import ome.xml.model.enums.{
  AcquisitionMode,
  ContrastMethod,
  IlluminationType
}
import ome.xml.model.primitives.{
  Color,
  NonNegativeInteger
}
import org.scijava.Context
import org.scijava.util.ColorRGB

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
                cStack.sorted.map(c => c.img)
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

  /**
   * Gets a packed integer, 8 bits per color component, for a wavelength in
   * in the visible spectrum. This is a little complicated because users
   * typically like colours biased toward red. Hence we adjust the wavelength
   * using an offset that scales depending on the magnitude of wavelength i.e.,
   * the higher the wavelength the greater the offset.
   *
   * Note: Adjusted wavelengths outside the below or above the visible spectrum
   * (min and max) are clamped to blue and red.
   *
   * HSB alpha, next is red, then green, and finally blue is LSB.
   *
   */
  private def getColour(wl: Double, min: Double, max: Double, offset: Double): Int = {
    val ratio = (wl - min) / (max - min)
    val adWl = wl + offset * ratio
    val rgb = if (adWl < min)
      ColorRGB.fromHSVColor(0.75, 1, 1)
    else if (adWl > max)
      ColorRGB.fromHSVColor(0, 1, 1)
    else {
      val scaled = 0.75 - ((adWl - min) / (max - min) * 0.75)
      ColorRGB.fromHSVColor(scaled, 1, 1)
    }
    (rgb.getRed << 24) | (rgb.getGreen << 16) | (rgb.getBlue << 8) | rgb.getAlpha
  }

  def addChannelMeta(omexml: MetadataStore): FileMetadata => Unit = {
    case FileMetadata(_, WaveformMetadata(_, _, _, _, channels, _, _, _), _, _, _) =>
        channels.foreach {
          case WaveformMetadata.Channel(_, _, _, _, _, _, WaveformMetadata.Excitation(ch, filter@_, laser, power@_, exposure@_)) => {
            omexml.setChannelID(s"Channel:${ch}", 0, ch)
            omexml.setChannelAcquisitionMode(AcquisitionMode.SPIM, 0, ch)
            omexml.setChannelIlluminationType(IlluminationType.EPIFLUORESCENCE, 0, ch)
            omexml.setChannelContrastMethod(ContrastMethod.FLUORESCENCE, 0, ch)
            omexml.setChannelColor(new Color(getColour(laser.toDouble, 380, 650, 70)), 0, ch)
            omexml.setChannelExcitationWavelength(new Length(laser, UNITS.NANOMETRE), 0, ch)
          }
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
    val companionName = outExt match {
      case "ome.tif"  => s"$outName.companion.ome"
      case _          => s"$outName.ome.xml"
    }

    val omeService = context.getService(classOf[OMEMetadataService])

    val omeString: Option[String] = MetadataUtils.createImageMetadata(imgs).flatMap(meta => {
      val omexml = new OMEXMLMetadataImpl()
      omeService.populateMetadata(omexml, 0, companionName, meta)
      omexml.setUUID(UUID.nameUUIDFromBytes(outName.getBytes).toString)
      imgs.headOption
        .foreach(img => addChannelMeta(omexml)(img.meta))
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

                  omexml.setPlaneTheC(new NonNegativeInteger(file.channel), 0, i)
                  omexml.setPlaneTheZ(new NonNegativeInteger(z), 0, i)
                  omexml.setPlaneTheT(new NonNegativeInteger(file.stack), 0, i)
                  wave.channels.headOption
                    .foreach(c =>
                        omexml.setPlaneExposureTime(new Time(c.excitation.exposure, UNITS.MILLISECOND), 0, i)
                    )
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
          new RuleTransformer(rewriteTransform)
            .transform(XML.loadString(omexml.dumpXML))
            .headOption
            .map(x => x.toString)
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
