package llsm

import net.imglib2.{Interval, RandomAccessible, RandomAccessibleInterval, RealRandomAccessible}
import net.imglib2.img.Img
import net.imglib2.`type`.numeric.RealType
import net.imglib2.interpolation.InterpolatorFactory
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory
import net.imglib2.transform.InverseTransform
import net.imglib2.transform.integer.{BoundingBox}
import net.imglib2.transform.integer.shear.ShearIntervalTransform
import net.imglib2.realtransform.{RealViews, RealShearTransform}
import net.imglib2.view.{IntervalView, TransformView, Views}

object Deskew {
  sealed trait DeskewError
  case class TranslateError(msg: String) extends DeskewError

  def shearedView[A <: RealType[A]](input: RandomAccessible[A], interval: Interval, sd: Int, rd: Int, offset: Int): IntervalView[A] = {
    val shearIT: ShearIntervalTransform = new ShearIntervalTransform(input.numDimensions, sd, rd, offset)
    val bb: BoundingBox = shearIT.transform(new BoundingBox(interval))
    Views.interval(new TransformView[A](input, new InverseTransform(shearIT)), bb.getInterval)
  }

  def realShearedView[A <: RealType[A]](input: RealRandomAccessible[A], interval: Interval, shearDim: Int, referenceDim: Int, shearInterval: Double): IntervalView[A] = {
    val rs: RealShearTransform = new RealShearTransform(input.numDimensions, shearDim, referenceDim, shearInterval)

    val bb: BoundingBox = rs.transform(new BoundingBox(interval))
    Views.interval(RealViews.transform(input, rs.inverse), bb.getInterval)
  }

  def deskewStack[A <: RealType[A]](input: Img[A], shearDim: Int, refDim: Int, slice_offset: Int): RandomAccessibleInterval[A] = {
    shearedView[A](Views.extendZero(input), input, shearDim, refDim, slice_offset)
  }

  def deskewRealStack[A <: RealType[A]](input: Img[A], shearDim: Int, referenceDim: Int, shearInterval: Double, interpolator: InterpolatorFactory[A, RandomAccessible[A]] = new NearestNeighborInterpolatorFactory[A]()): RandomAccessibleInterval[A] = {
    val iv = Views.interpolate[A, RandomAccessible[A]](Views.extendZero(input), interpolator)
    realShearedView[A](iv, input, shearDim, referenceDim, shearInterval)
  }
}
