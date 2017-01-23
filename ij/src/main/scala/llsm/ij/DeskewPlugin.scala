package llsm
package ij

import llsm.Deskew
import java.lang.Math
import net.imagej.Dataset
import net.imagej.axis.{Axes, CalibratedAxis, DefaultLinearAxis}
import net.imagej.DatasetService
import net.imglib2.img.Img
import net.imglib2.img.display.imagej.ImageJFunctions
import net.imglib2.interpolation.randomaccess.{
  NearestNeighborInterpolatorFactory,
  NLinearInterpolatorFactory,
  LanczosInterpolatorFactory
}
import net.imglib2.`type`.numeric.RealType
import org.scijava.ItemIO
import org.scijava.command.Command
import org.scijava.ui.UIService
import org.scijava.plugin.{Plugin, Parameter}
import _root_.ij.ImagePlus
import _root_.ij.measure.Calibration
import scala.language.existentials

@Plugin(`type` = classOf[Command],
        headless = true,
        menuPath = "Plugins>LLSM>Deskew Single Stack")
class DeskewPlugin extends Command {

  @Parameter(`type` = ItemIO.INPUT)
  var input: Dataset = _

  @Parameter(label = "X/Y voxel size (um)", required = true)
  var pixelSize: Double = 0.1018

  @Parameter(label = "Sample piezo increment", required = true)
  var sampleIncrement: Double = 0.3000

  @Parameter(label = "Interpolation scheme",
             choices = Array("None", "Nearest Neighbour", "Linear", "Lanczos"),
             required = false)
  var interpolation: String = "None"

  @Parameter(label = "Output Virtual Stack")
  var virtual: Boolean = false

  @Parameter
  var ui: UIService = _

  @Parameter
  var ds: DatasetService = _

  // @Parameter(`type` = ItemIO.OUTPUT)
  // var out: ImagePlus = _

  /**
    * Entry point to running a plugin.
    */
  override def run(): Unit = {
    val ip: Img[T] forSome { type T <: RealType[T] } = input.getImgPlus.getImg
      .asInstanceOf[Img[T] forSome { type T <: RealType[T] }]

    val shearFactor: Double = (Math.cos(Math.toRadians(31.8)) * sampleIncrement) / pixelSize

    val zInterval: Double = Math.sin(Math.toRadians(31.8)) * sampleIncrement

    var axes: Array[CalibratedAxis] =
      Array.ofDim[CalibratedAxis](input.numDimensions)

    input.axes(axes)

    axes = axes.map(a => {
      val axisType = a.`type`()
      if (axisType == Axes.X)
        new DefaultLinearAxis(Axes.X, "um", pixelSize)
      else if (axisType == Axes.Y)
        new DefaultLinearAxis(Axes.Y, "um", pixelSize)
      else if (axisType == Axes.Z)
        new DefaultLinearAxis(Axes.Z, "um", zInterval)
      else a
    })

    val xIndex = input.dimensionIndex(Axes.X)
    val zIndex = input.dimensionIndex(Axes.Z)

    // out = ds.create(Deskew.deskewStack(ip, xIndex, zIndex, shearFactor))
    // out.setAxes(axes)
    val rai = interpolation match {
      case "None" =>
        Deskew.deskewStack(ip, xIndex, zIndex, Math.round(shearFactor).toInt)
      case "Nearest Neighbour" =>
        Deskew.deskewRealStack(ip,
                               xIndex,
                               zIndex,
                               shearFactor,
                               new NearestNeighborInterpolatorFactory())
      case "Linear" =>
        Deskew.deskewRealStack(ip,
                               xIndex,
                               zIndex,
                               shearFactor,
                               new NLinearInterpolatorFactory())
      case "Lanczos" =>
        Deskew.deskewRealStack(ip,
                               xIndex,
                               zIndex,
                               shearFactor,
                               new LanczosInterpolatorFactory())
    }
    val outputName: String =
      s"${input.getImgPlus().getName().split('.').head}_deskewed_$interpolation"

    if (virtual) {
      val out: ImagePlus = ImageJFunctions.wrap(rai, outputName)
      val cal            = new Calibration(out)
      cal.setUnit("um")
      cal.pixelWidth = pixelSize
      cal.pixelHeight = pixelSize
      cal.pixelDepth = zInterval

      out.setCalibration(cal)
      out.setTitle(outputName)
      out.setDimensions(input.dimension(Axes.CHANNEL).toInt,
                        input.dimension(Axes.Z).toInt,
                        input.dimension(Axes.TIME).toInt)

      ui.show(out)
    } else {
      val out: Dataset = ds.create(rai)
      out.setAxes(axes)
      out.getImgPlus().setName(outputName)
      ui.show(out)
    }
  }
}
