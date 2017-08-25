package llsm.io.metadata

import scala.collection.JavaConverters._
import scala.collection.immutable.TreeSet

import io.scif.{ImageMetadata, DefaultImageMetadata}
import io.scif.util.FormatTools
import llsm.Deskew
import llsm.io.LLSMImg
import net.imagej.axis.{Axes, CalibratedAxis, DefaultLinearAxis}

object MetadataUtils {

  /** Convert LLSMImg metadata to SCIFIO ImageMetadata
   *
   * Takes an LLSMImg and converts the metadata it into a default
   * implementation of io.scif.ImageMetadata.
   * @param imgs LLSMImg
   * @return ImageMetadata
   */
  def createImageMetadata(img: LLSMImg): ImageMetadata = {
    val imeta: ImageMetadata = new DefaultImageMetadata

    val fm: FileMetadata = img.meta
    val waveform: WaveformMetadata = fm.waveform
    val sample: SampleStage        = fm.sample

    val xAxis: CalibratedAxis = new DefaultLinearAxis(Axes.X, "um", fm.config.xVoxelSize)
    val yAxis: CalibratedAxis = new DefaultLinearAxis(Axes.Y, "um", fm.config.yVoxelSize)
    val zAxis: CalibratedAxis = new DefaultLinearAxis(Axes.Z, "um", Deskew.calcZInterval(waveform.sPZTInterval, waveform.zPZTInterval, sample.angle))

    val axes: List[CalibratedAxis] = List(xAxis, yAxis, zAxis)
    val dims: Array[Long] = Array.ofDim[Long](img.img.numDimensions)
    img.img.dimensions(dims)

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

  /** Convert List of LLSMImg metadata to SCIFIO ImageMetadata
   *
   * Takes list of LLSMImgs and converts the metadata it into a default
   * implementation of io.scif.ImageMetadata.
   * @param imgs List of LLSMImgs
   * @return ImageMetadata
   */
  def createImageMetadata(imgs: List[LLSMImg]): Option[ImageMetadata] = {
    val meta = new DefaultImageMetadata
    val (name, channelIdxs, times) =
      imgs.foldLeft(
        (imgs(0).meta.filename.name,
         TreeSet.empty[Int],
         TreeSet.empty[Long])) {
        case ((n, c, t),
              LLSMImg(_,
                FileMetadata(
                  FilenameMetadata(_, _, channel, _, _, time, _),
                  _,
                  _,
                  _,
                  _))) =>
          (n, c + channel, t + time)
      }

    imgs.headOption map {
      refImg => {
        val waveform: WaveformMetadata    = refImg.meta.waveform
        val sample: SampleStage           = refImg.meta.sample
        val config: ConfigurableMetadata  = refImg.meta.config

        val xAxis: CalibratedAxis = new DefaultLinearAxis(Axes.X, "um", config.xVoxelSize)
        val yAxis: CalibratedAxis = new DefaultLinearAxis(Axes.Y, "um", config.yVoxelSize)

        // TODO: This is ugly baby
        val axes: List[CalibratedAxis] = List(xAxis, yAxis) ++
            (if (waveform.nSlices > 1) {
             val zVoxelSize: Double = Deskew.calcZInterval(
               waveform.sPZTInterval,
               waveform.zPZTInterval,
               sample.angle
             )
             List(new DefaultLinearAxis(Axes.Z, "um", zVoxelSize))
           } else Nil) ++
            (if (channelIdxs.size > 1) {
             List(new DefaultLinearAxis(Axes.CHANNEL))
           } else Nil) ++
            (if (times.size > 1) {
             val diffs: List[Long] =
               (times.toList.drop(1), times.toList).zipped.map(_ - _)
             val avInterval: Double = diffs.foldRight(0L)(_ + _).toDouble / diffs.size
             List(new DefaultLinearAxis(Axes.TIME, "ms", avInterval))
           } else Nil)

        val stackDim = Array.ofDim[Long](refImg.img.numDimensions)
        refImg.img.dimensions(stackDim)
        val dims: Array[Long] = stackDim ++
            (if (channelIdxs.size > 1)
               Array(channelIdxs.size.toLong)
             else Nil) ++
            (if (times.size > 1)
               Array(times.size.toLong)
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
  }
}
