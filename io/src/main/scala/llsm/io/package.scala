package llsm

import java.io.{File}
import java.lang.Math

import scala.collection.JavaConverters._
import scala.collection.immutable.{ListMap, TreeSet}
import scala.util.Try
import scala.util.matching.Regex

import cats.implicits._
import llsm.io.metadata.{CameraMetadata, FilenameMetadata, Metadata}
import net.imagej.axis.{Axes, CalibratedAxis, DefaultLinearAxis}
import net.imglib2.{RandomAccessibleInterval}
import net.imglib2.`type`.numeric.integer.UnsignedShortType
import net.imglib2.view.Views
import _root_.io.scif.{ImageMetadata, DefaultImageMetadata}
import _root_.io.scif.config.SCIFIOConfig
import _root_.io.scif.img.ImgOpener
import _root_.io.scif.util.FormatTools

sealed trait LLSMIOError
object LLSMIOError {
  case class MetadataIOError(error: Metadata.MetadataError) extends LLSMIOError
  case class ImgIOError(message: String)      extends LLSMIOError
}

/**
  * Tools for reading, parsing and writing LLSM images and metadata
  * Starting point is typically a directory containing LLSM image stacks
  * and a raw metadata file.
  *  img_dir __ readFileNameMeta __ ImageMetadata
  *          \_ readWaveformMeta _/
  */
package object io {

  def convertMetadata(
      metadata: Metadata): Either[LLSMIOError, ImageMetadata] = {
    val meta = new DefaultImageMetadata
    val (name, channelIdxs, stackIdxs, wavelengths, times, timesAbs) =
      metadata.filename match {
        case FilenameMetadata(n, c, s, wl, ts, ats) :: tail => {
          metadata.filename.foldLeft(
            (n,
             TreeSet[Int](c),
             TreeSet[Int](s),
             TreeSet[Int](wl),
             TreeSet[Long](ts),
             TreeSet[Long](ats))) {
            case ((n, c1, s1, w1, t1, ta1),
                  FilenameMetadata(_,
                                   channel,
                                   stack,
                                   wavelength,
                                   time,
                                   timeA)) =>
              (n,
               c1 + channel,
               s1 + stack,
               w1 + wavelength,
               t1 + time,
               ta1 + timeA)
          }
        }
        case Nil => throw new Exception("No files were found.")
      }

    val xAxis: CalibratedAxis = new DefaultLinearAxis(Axes.X, "um", 0.1018)
    val yAxis: CalibratedAxis = new DefaultLinearAxis(Axes.Y, "um", 0.1018)

    // TODO: This is ugly baby
    val axes: List[CalibratedAxis] = List(xAxis, yAxis) ++
        (if (metadata.waveform.nSlices > 1) {
         val so: Double         = metadata.waveform.sPZTOffset
         val zVoxelSize: Double = Math.sin(Math.toRadians(31.8)) * so + metadata.waveform.zPZTOffset
         List(new DefaultLinearAxis(Axes.Z, "um", zVoxelSize))
       } else Nil) ++
        (if (channelIdxs.size > 1) {
         List(new DefaultLinearAxis(Axes.CHANNEL))
       } else Nil) ++
        (if (times.size > 1) {
         val diffs: List[Long] =
           (times.toList.drop(1), times.toList).zipped.map(_ - _)
         val avInterval: Double = diffs.reduce(_ + _).toDouble / diffs.size
         List(new DefaultLinearAxis(Axes.TIME, "ms", avInterval))
       } else Nil)

    val roi: CameraMetadata.ROI = metadata.camera.roi
    val xLength: Long           = roi.right - roi.left + 1
    val yLength: Long           = roi.bottom - roi.top + 1
    val dims: List[Long] = List(xLength, yLength) ++
        (if (metadata.waveform.nSlices > 1)
           List(metadata.waveform.nSlices)
         else Nil) ++
        (if (channelIdxs.size > 1)
           List(channelIdxs.size.toLong)
         else Nil) ++
        (if (times.size > 1)
           List(times.size.toLong)
         else Nil)

    meta.populate(name,
                  axes.asJava,
                  dims.toArray,
                  FormatTools.UINT16,
                  false,
                  true,
                  false,
                  false,
                  false)
    Either.right(meta)
  }

  /**
    * Takes a list of files and groups them based on a Regex.
    * Groups are sorted by key.
    * @return Try[List[List[File]]]
    */
  def groupImgsByRegex(imgPaths: List[File],
                       regex: Regex): Try[List[List[File]]] =
    Try {
      val groupD = imgPaths.groupBy(p => {
        val regex(m) = p.getName
        m
      })
      ListMap(groupD.toSeq.sortBy(_._1): _*).values.toList
    }

  def readSplitImgs(imgPaths: List[File],
                    regex: List[Regex],
                    imgOpener: ImgOpener,
                    sc: SCIFIOConfig)
    : Either[LLSMIOError, RandomAccessibleInterval[UnsignedShortType]] = {
    regex match {
      case Nil =>
        Either
          .fromTry(
            imgPaths
              .map(
                p =>
                  Try(
                    imgOpener
                      .openImgs(p.getPath, new UnsignedShortType, sc)
                      .asScala
                      .head))
              .sequenceU)
          .map((listImgs: List[RandomAccessibleInterval[UnsignedShortType]]) =>
            if (listImgs.size < 2) listImgs.head
            else Views.stack[UnsignedShortType](listImgs.asJava))
          .leftMap(e =>
              LLSMIOError.ImgIOError(s"Unable to read Images: \n${e.getMessage}"))
      case h :: t => {
        Either
          .fromTry(groupImgsByRegex(imgPaths, h))
          .leftMap[LLSMIOError](e => LLSMIOError.ImgIOError(e.getMessage))
          .flatMap(limgs => {
            limgs.map(readSplitImgs(_, t, imgOpener, sc)).sequenceU
          })
          .map(limgs => Views.stack[UnsignedShortType](limgs.asJava))
      }
    }
  }

  def readImgs(imgPaths: List[File], meta: ImageMetadata)
    : Either[LLSMIOError, RandomAccessibleInterval[UnsignedShortType]] = {
    val imgOpener = new ImgOpener()
    val sc = new SCIFIOConfig()
      .imgOpenerSetComputeMinMax(true)
      .imgOpenerSetImgModes(SCIFIOConfig.ImgMode.CELL)

    val timeRegex = ".*_stack(\\d+)_.*".r
    val chRegex   = ".*_ch(\\d+)_.*".r
    val regexs = List[Regex]() ++
        (if (meta.getAxisLength(Axes.TIME) > 1) List(timeRegex) else Nil) // ++
    // (if (meta.getAxisLength(Axes.CHANNEL) > 1) List(chRegex) else Nil)

    readSplitImgs(imgPaths.sorted, regexs, imgOpener, sc)
  }
}
