/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2016 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package net.imglib2.realtransform;

import java.lang.Math;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.transform.integer.BoundingBox;
import net.imglib2.transform.integer.BoundingBoxTransform;

/**
 * Invertible shear transform that is performed in real space.
 * coordinate[shearDimension] += coordinate[referenceDimension] * shearFactor
 *
 * Inverse is calculated by taking the inverse of the shearFactor.
 *
 * @author Philip Hanslovsky
 * @author Keith Schulze
 */
public class RealShearTransform implements InvertibleRealTransform, BoundingBoxTransform {

  private int nDim;
  private int shearDimension;
  private int referenceDimension;
  private double shearFactor;

  public RealShearTransform(int nDim, int shearDimension, int referenceDimension) {
    this(nDim, shearDimension, referenceDimension, 1.0);
  }

  public RealShearTransform(int nDim, int shearDimension, int referenceDimension, double shearFactor) {
    if (nDim < 2) throw new IllegalArgumentException("nDim cannot be < 2");
    else if (shearDimension >= nDim) throw new IllegalArgumentException("shearDimension cannot be greater than nDim.");
    else if (referenceDimension >= nDim) throw new IllegalArgumentException("referenceDimension cannot be greater than nDim.");
    else if (referenceDimension == shearDimension) throw new IllegalArgumentException("referenceDimension cannot be equal to shearDimension.");
    else if (shearFactor == 0.0) throw new IllegalArgumentException("shearFactor cannot be zero.");

    this.nDim = nDim;
    this.shearDimension = shearDimension;
    this.referenceDimension = referenceDimension;
    this.shearFactor = shearFactor;
  }

  public int numSourceDimensions() {
    return this.nDim;
  }

  public int numTargetDimensions() {
    return this.nDim;
  }

  public void apply(double[] source, double[] target) {
    if (source.length != this.nDim || target.length != this.nDim)
      throw new IllegalArgumentException("Source and target coordinates must be of the same dimensionality as the transform.");

    System.arraycopy(source, 0, target, 0, source.length);
    target[this.shearDimension] += target[this.referenceDimension] * this.shearFactor;
  }

  public void apply(float[] source, float[] target) {
    if (source.length != this.nDim || target.length != this.nDim)
      throw new IllegalArgumentException("Source and target coordinates must be of the same dimensionality as the transform.");

    System.arraycopy(source, 0, target, 0, source.length);
    target[this.shearDimension] += target[this.referenceDimension] * this.shearFactor;
  }

  public void apply(RealLocalizable source, RealPositionable target) {
    if (source.numDimensions() != this.nDim || target.numDimensions() != this.nDim)
      throw new IllegalArgumentException("Source and target coordinates must be of the same dimensionality as the transform.");

    target.setPosition(source);
    target.setPosition(source.getDoublePosition(this.shearDimension) + source.getDoublePosition(this.referenceDimension) * this.shearFactor, this.shearDimension);
  }

  public void applyInverse(double[] source, double[] target) {
    if (source.length != this.nDim || target.length != this.nDim)
      throw new IllegalArgumentException("Source and target coordinates must be of the same dimensionality as the transform.");

    System.arraycopy(target, 0, source, 0, target.length);
    source[this.shearDimension] -= source[this.referenceDimension] * this.shearFactor;
  }

  public void applyInverse(float[] source, float[] target) {
    if (source.length != this.nDim || target.length != this.nDim)
      throw new IllegalArgumentException("Source and target coordinates must be of the same dimensionality as the transform.");

    System.arraycopy(target, 0, source, 0, target.length);
    source[this.shearDimension] -= source[this.referenceDimension] * this.shearFactor;
  }

  public void applyInverse(RealPositionable source, RealLocalizable target) {
    if (source.numDimensions() != this.nDim || target.numDimensions() != this.nDim)
      throw new IllegalArgumentException("Source and target coordinates must be of the same dimensionality as the transform.");

    source.setPosition(target);
    source.setPosition(target.getDoublePosition(this.shearDimension) + target.getDoublePosition(this.referenceDimension) * this.shearFactor, this.shearDimension);
  }

  public RealShearTransform inverse() {
    return new RealShearTransform(this.nDim, this.shearDimension, this.referenceDimension, -this.shearFactor);
  }

  public RealShearTransform copy() {
    return new RealShearTransform(this.nDim, this.shearDimension, this.referenceDimension, this.shearFactor);
  }

  public BoundingBox transform(BoundingBox bb) {
    bb.orderMinMax();
    long[] c = bb.corner2;
    long diff = c[this.referenceDimension] - bb.corner1[this.referenceDimension];
    c[this.shearDimension] += (long) Math.round(diff * this.shearFactor);
    return bb;
  }

  public int numDimensions() {
    return this.nDim;
  }

  public void setNumDimensions(int nDim) {
    this.nDim = nDim;
  }

  public int getShearDimension() {
    return this.shearDimension;
  }

  public void setShearDimension(int shearDimension) {
    if (shearDimension >= this.nDim)
      throw new IllegalArgumentException("Shear dimension is outside the dimension range of this transform: numDimensions = " + this.nDim + " and shearDimension = " + shearDimension);
    else if (shearDimension == this.referenceDimension)
      throw new IllegalArgumentException("shearDimension and referenceDimension cannot be equal.");

    this.shearDimension = shearDimension;
  }

  public int getReferenceDimension() {
    return this.referenceDimension;
  }

  public void setReferenceDimension(int referenceDimension) {
    if (referenceDimension >= this.nDim)
      throw new IllegalArgumentException("Reference dimension is outside the dimension range of this transform: numDimensions = " + this.nDim + " and referenceDimension = " + referenceDimension);
    else if (referenceDimension == this.shearDimension)
      throw new IllegalArgumentException("shearDimension and referenceDimension cannot be equal.");

    this.referenceDimension = referenceDimension;
  }

  public double getShearInterval() {
    return this.shearFactor;
  }

  public void setShearInterval(double shearFactor) {
    if (shearFactor == 0) throw new IllegalArgumentException("shearFactor cannot be zero.");
    this.shearFactor = shearFactor;
  }
}
