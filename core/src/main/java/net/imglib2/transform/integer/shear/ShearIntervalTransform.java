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
package net.imglib2.transform.integer.shear;

import java.util.Arrays;

import net.imglib2.Localizable;
import net.imglib2.Positionable;
import net.imglib2.transform.InvertibleTransform;
import net.imglib2.transform.integer.BoundingBox;
import net.imglib2.transform.integer.BoundingBoxTransform;

/**
 * Invertible shear transform that shears by whole pixel units.
 * coordinate[shearDimension] += coordinate[referenceDimension] * shearFactor
 *
 * @author Philipp Hanslovsky
 * @author Keith Schulze
 *
 */
public class ShearIntervalTransform implements InvertibleTransform, BoundingBoxTransform
{


	private final int nDim;
	private final int shearDimension;
	private final int referenceDimension;
	private final int shearFactor;


	/**
	 * @param nDim						Number of dimensions (source and target dimensions must be the same)
	 * @param shearDimension			Dimension to be sheared.
	 * @param referenceDimension		Dimension used as reference for shear.
	 * @param shearFactor				Interval for shear i.e., amount to shift shear dimension with respect
	 *									an increment in the reference referenceDimension.
	 * @throws IllegalArgumentException	if nDim < 2, shearDimension >= nDim, referenceDimension >= nDim,
	 *									referenceDimension == shearDimension
	 */
	public ShearIntervalTransform(
			int nDim,
			int shearDimension,
			int referenceDimension,
			int shearFactor)
	{
		if (nDim < 2) throw new IllegalArgumentException("nDim cannot be < 2");
		else if (shearDimension >= nDim) throw new IllegalArgumentException("shearDimension cannot be greater than nDim.");
		else if (referenceDimension >= nDim) throw new IllegalArgumentException("referenceDimension cannot be greater than nDim.");
    	else if (referenceDimension == shearDimension) throw new IllegalArgumentException("referenceDimension cannot be equal to shearDimension.");

		this.nDim = nDim;
		this.shearDimension = shearDimension;
		this.referenceDimension = referenceDimension;
		this.shearFactor = shearFactor;
	}


	/**
	 * Auxillary constructor where the shearFactor defaults to 1.
	 *
	 * @param nDim					Number of dimensions (source and target dimensions must be the same)
	 * @param shearDimension		Dimension to be sheared.
	 * @param referenceDimension	Dimension used as reference for shear.
	 */
	public ShearIntervalTransform(
			int nDim,
			int shearDimension,
			int referenceDimension)
	{
		this( nDim, shearDimension, referenceDimension, 1 );
	}


	@Override
	public int numSourceDimensions()
	{
		return this.nDim;
	}


	@Override
	public int numTargetDimensions()
	{
		return this.nDim;
	}


	@Override
	public void apply( long[] source, long[] target )
	{
		assert source.length == this.nDim && target.length == this.nDim;

		System.arraycopy( source, 0, target, 0, source.length );
		target[this.shearDimension] += source[this.referenceDimension] * this.shearFactor;
	}


	@Override
	public void apply( int[] source, int[] target )
	{
		assert source.length == this.nDim && target.length == this.nDim;

		System.arraycopy( source, 0, target, 0, source.length );
		target[this.shearDimension] += source[this.referenceDimension] * this.shearFactor;
	}


	@Override
	public void apply( Localizable source, Positionable target )
	{
		assert source.numDimensions() == nDim && target.numDimensions() == nDim;

		target.setPosition( source );
		target.setPosition( source.getLongPosition( this.shearDimension ) + source.getLongPosition( this.referenceDimension ) * this.shearFactor, this.shearDimension );
	}


	@Override
	public void applyInverse( long[] source, long[] target )
	{
		assert source.length == this.nDim && target.length == this.nDim;

		System.arraycopy( target, 0, source, 0, target.length );
		source[this.shearDimension] -= target[this.referenceDimension] * this.shearFactor;
	}


	@Override
	public void applyInverse( int[] source, int[] target )
	{
		assert source.length == this.nDim && target.length == this.nDim;

		System.arraycopy( target, 0, source, 0, target.length );
		source[this.shearDimension] -= target[this.referenceDimension] * this.shearFactor;
	}


	@Override
	public void applyInverse( Positionable source, Localizable target )
	{
		assert source.numDimensions() == this.nDim && target.numDimensions() == this.nDim;

		source.setPosition( target );
		source.setPosition( target.getLongPosition( this.shearDimension ) - target.getLongPosition( this.referenceDimension ) * this.shearFactor, this.shearDimension );
	}


	public ShearIntervalTransform copy()
	{
		return new ShearIntervalTransform( this.nDim, this.shearDimension, this.referenceDimension, this.shearFactor );
	}


	@Override
	public ShearIntervalTransform inverse()
	{
		return new ShearIntervalTransform( this.nDim, this.shearDimension, this.referenceDimension, -this.shearFactor );
	}


	@Override
	public BoundingBox transform( BoundingBox bb )
	{
		bb.orderMinMax();
		long[] c2 = Arrays.copyOf(bb.corner2, bb.corner2.length);
		long diff = c2[this.referenceDimension] - bb.corner1[this.referenceDimension];
		c2[this.shearDimension] += diff * this.shearFactor;
		return new BoundingBox( bb.corner1, c2 );
	}


	public int getShearDimension()
	{
		return this.shearDimension;
	}


	public int getReferenceDimension()
	{
		return this.referenceDimension;
	}


	public int getShearFactor()
	{
		return this.shearFactor;
	}
}
