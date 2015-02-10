package org.terifan.ocr;

import java.awt.RenderingHints;


public enum InterpolationMode
{
	NEAREST(1, "nearest", RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR),
	BILINEAR(2, "bilinear", RenderingHints.VALUE_INTERPOLATION_BILINEAR),
	BICUBIC(3, "bicubic", RenderingHints.VALUE_INTERPOLATION_BICUBIC);

	private final int mIndex;
	private final String mName;
	private final Object mHint;


	private InterpolationMode(int aIndex, String aName, Object aHint)
	{
		mIndex = aIndex;
		mName = aName;
		mHint = aHint;
	}


	/**
	 * Return the filter enumeration index (1 for nearest, 2 for bilinear and 3 for bicubic)
	 */
	public int getIndex()
	{
		return mIndex;
	}


	/**
	 * The name of the filter in lower case letters.
	 */
	@Override
	public String toString()
	{
		return mName;
	}


	/**
	 * The RenderingHints value object for this filter.
	 */
	public Object getHint()
	{
		return mHint;
	}
}
