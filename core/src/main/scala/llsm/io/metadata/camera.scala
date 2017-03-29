package llsm.io.metadata

case class CameraMetadata(model: String,
                          serial: Int,
                          frameTransfer: String,
                          trigger: String,
                          exposure: Double,
                          cycleT: Double,
                          cycleR: Double,
                          frameMode: String,
                          readMode: String,
                          roi: CameraMetadata.ROI,
                          binning: CameraMetadata.Coord,
                          pixels: CameraMetadata.Coord,
                          vssSpeed: Double,
                          hssSpeed: Double,
                          vssAmp: String,
                          outAmp: String,
                          temp: Double,
                          adChannel: Int,
                          emGain: Long,
                          hsSpeedIndex: Int,
                          preampGain: Long,
                          bitDepth: Int,
                          baslineClamp: String,
                          spool: String,
                          expNumber: Long,
                          cropped: String,
                          lRoi: CameraMetadata.ROI,
                          rRoi: CameraMetadata.ROI,
                          fovRoi: CameraMetadata.ROI,
                          imgNumber: Long,
                          subRois: String)

object CameraMetadata {
  case class ROI(left: Long, top: Long, right: Long, bottom: Long)
  case class Coord(x: Long, y: Long)
}
