package llsm

import net.imglib2.transform.integer.shear.ShearIntervalTransform
import net.imglib2.realtransform.RealShearTransform
import org.scalatest.Matchers._
import org.scalacheck.Gen

class ShearSpec extends BaseSuite {
  "A ShearIntervalTransform" should "c[shearDimension] += c[referenceDimension] * shearFactor" in forAll(
    (Gen.choose(2, 10), "nDim"),
    (Gen.choose(0, 9), "shearDimension"),
    (Gen.choose(0, 9), "referenceDimension"),
    (Gen.choose(-5, 5), "shearFactor"),
    (Gen.choose(1L, 1000L), "c"),
    maxDiscardedFactor(10)
  ) {
    (nDim: Int, shearDimension: Int, referenceDimension: Int, shearFactor: Int,
     c: Long) =>
      whenever(
        nDim > 1 && shearDimension < nDim && referenceDimension < nDim && shearDimension != referenceDimension && shearFactor != 0) {
        val source: Array[Long] = Array.fill[Long](nDim)(c)
        val target: Array[Long] = Array.ofDim[Long](nDim)

        val st = new ShearIntervalTransform(nDim,
                                            shearDimension,
                                            referenceDimension,
                                            shearFactor)

        st.apply(source, target)
        target(referenceDimension) should equal(c)
        target(shearDimension) should equal(c + c * shearFactor)
      }
  }
  it should "throw an IllegalArgumentException when nDim < 2" in {
    assertThrows[IllegalArgumentException] {
      new ShearIntervalTransform(1, 0, 0, 1)
    }
  }
  it should "throw an IllegalArgumentException when the shearDimension or referenceDimension are >= nDim" in forAll(
    "nDim",
    "shearDimension",
    "referenceDimension") {
    (nDim: Int, shearDimension: Int, referenceDimension: Int) =>
      whenever(shearDimension >= nDim || referenceDimension >= nDim) {
        assertThrows[IllegalArgumentException] {
          new ShearIntervalTransform(nDim,
                                     shearDimension,
                                     referenceDimension,
                                     1)
        }
      }
  }
  it should "throw an IllegalArgumentException when shearDimension == referenceDimension" in {
    assertThrows[IllegalArgumentException] {
      new ShearIntervalTransform(5, 2, 2, 3)
    }
  }
  it should "throw an IllegalArgumentException when the shearFactor == 0" in {
    assertThrows[IllegalArgumentException] {
      new ShearIntervalTransform(5, 1, 2, 0)
    }
  }

  "A RealShearTransform" should "c[shearDimension] += c[referenceDimension] * shearFactor" in forAll(
    (Gen.choose(2, 10), "nDim"),
    (Gen.choose(0, 9), "shearDimension"),
    (Gen.choose(0, 9), "referenceDimension"),
    (Gen.choose(-5.0, 5.0), "shearFactor"),
    (Gen.choose(1.0, 1000.0), "c")
  ) {
    (nDim: Int, shearDimension: Int, referenceDimension: Int,
     shearFactor: Double, c: Double) =>
      whenever(
        nDim > 1 && shearDimension < nDim && referenceDimension < nDim && shearDimension != referenceDimension && shearFactor != 0.0) {
        val source: Array[Double] = Array.fill[Double](nDim)(c)
        val target: Array[Double] = Array.ofDim[Double](nDim)

        val st = new RealShearTransform(nDim,
                                        shearDimension,
                                        referenceDimension,
                                        shearFactor)

        st.apply(source, target)
        target(referenceDimension) should equal(c)
        target(shearDimension) should equal(c + c * shearFactor)
      }
  }
  it should "throw an IllegalArgumentException when nDim < 2" in {
    assertThrows[IllegalArgumentException] {
      new RealShearTransform(1, 0, 0, 1.0)
    }
  }
  it should "throw an IllegalArgumentException when the shearDimension or referenceDimension are >= nDim" in forAll(
    "nDim",
    "shearDimension",
    "referenceDimension") {
    (nDim: Int, shearDimension: Int, referenceDimension: Int) =>
      whenever(shearDimension >= nDim || referenceDimension >= nDim) {
        assertThrows[IllegalArgumentException] {
          new RealShearTransform(nDim, shearDimension, referenceDimension, 1.0)
        }
      }
  }
  it should "throw an IllegalArgumentException when shearDimension == referenceDimension" in {
    assertThrows[IllegalArgumentException] {
      new RealShearTransform(5, 2, 2, 3.0)
    }
  }
  it should "throw an IllegalArgumentException when the shearFactor == 0" in {
    assertThrows[IllegalArgumentException] {
      new RealShearTransform(5, 1, 2, 0.0)
    }
  }
}
