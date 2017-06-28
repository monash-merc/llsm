package llsm.io.metadata

import java.lang.Math

import scala.collection.JavaConverters._
import scala.collection.immutable.TreeSet

import io.scif.{ImageMetadata, DefaultImageMetadata}
import io.scif.util.FormatTools
import llsm.Deskew
import net.imagej.axis.{Axes, CalibratedAxis, DefaultLinearAxis}
import net.imglib2.img.Img
import net.imglib2.`type`.numeric.integer.UnsignedShortType

object MetadataUtils {

  def populateImageMetadata(fm: FileMetadata): ImageMetadata = {
    val imeta: ImageMetadata = new DefaultImageMetadata

    val waveform: WaveformMetadata = fm.waveform
    val camera: CameraMetadata     = fm.camera
    val sample: SampleStage        = fm.sample

    val xAxis: CalibratedAxis = new DefaultLinearAxis(Axes.X, "um", fm.config.xVoxelSize)
    val yAxis: CalibratedAxis = new DefaultLinearAxis(Axes.Y, "um", fm.config.yVoxelSize)
    val zAxis: CalibratedAxis = new DefaultLinearAxis(Axes.Z, "um", Deskew.calcZInterval(waveform.sPZTInterval, waveform.zPZTInterval, sample.angle, fm.config.xVoxelSize))

    val axes: List[CalibratedAxis] = List(xAxis, yAxis, zAxis)
    val imgWidth: Long = fm.camera.roi.right - fm.camera.roi.left + 1
    val imgHeight: Long = fm.camera.roi.bottom - fm.camera.roi.top + 1
    val dims: Array[Long] = Array[Long](imgWidth, imgHeight, fm.waveform.nSlices)

    imeta.populate(fm.filename.name,
                  axes.asJava,
                  dims.toArray,
                  FormatTools.UINT16,
                  true,
                  false,
                  false,
                  false,
                  false)

    imeta
  }

  /** Convert LLSM metadata to SCIFIO ImageMetadata
   *
   * Takes an LLSM metadata heirachy and compiles it into a default
   * implementation of io.scif.ImageMetadata.
   * @param metadata List of LLSM FileMetadata objects
   * @return ImageMetadata
   */
  def convertMetadata(metadata: List[FileMetadata]): ImageMetadata = {
    val meta = new DefaultImageMetadata
    val (name, channelIdxs, stackIdxs, wavelengths, times, timesAbs) =
      metadata.foldLeft(
        (metadata(0).filename.name,
         TreeSet.empty[Int],
         TreeSet.empty[Int],
         TreeSet.empty[Int],
         TreeSet.empty[Long],
         TreeSet.empty[Long])) {
        case ((n, c, s, w, t, ta),
              FileMetadata(
                FilenameMetadata(_, _, channel, stack, wavelength, time, timeAbs),
                _,
                _,
                _,
                _,
                _)) =>
          (n, c + channel, s + stack, w + wavelength, t + time, ta + timeAbs)
      }
    val waveform: WaveformMetadata = metadata(0).waveform
    val camera: CameraMetadata     = metadata(0).camera
    val sample: SampleStage        = metadata(0).sample

    val xAxis: CalibratedAxis = new DefaultLinearAxis(Axes.X, "um", 0.1018)
    val yAxis: CalibratedAxis = new DefaultLinearAxis(Axes.Y, "um", 0.1018)

    // TODO: This is ugly baby
    val axes: List[CalibratedAxis] = List(xAxis, yAxis) ++
        (if (waveform.nSlices > 1) {
         val so: Double         = waveform.sPZTInterval
         val zVoxelSize: Double = Math.sin(Math.toRadians(sample.angle)) * so + waveform.zPZTInterval
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

    val roi: CameraMetadata.ROI = camera.roi
    val xLength: Long           = roi.right - roi.left + 1
    val yLength: Long           = roi.bottom - roi.top + 1
    val dims: List[Long] = List(xLength, yLength) ++
        (if (waveform.nSlices > 1)
           List(waveform.nSlices)
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
    meta
  }
}
