package org.terifan.ocr;

import java.awt.Insets;
import java.awt.Polygon;
import java.awt.image.BufferedImage;


public class Symbol
{
	private BufferedImage mBitmap;
	protected Insets mBorders;
	protected String mFontName;
	protected String mCharacter;
	protected TextBox mTextBox;
	protected double [][] mContour; // 8 orientations, pixel offset
	protected int [][] mSlopes; // 8 orientations, pixel offset
	protected Polygon [][] mCurvature; // 8 orientations
	protected int [][] mCurvatureSlopes;
	protected double [][][] mCurvatureVector; // 8 orientations, 2 types (inclining/declining), 3 zones (upper/center/lower)
	protected int [][] mClosestPixel;
	protected double mCurvatureWeight;
	protected String mDefCharacter;


	public Symbol(TextBox aTextBox)
	{
		mTextBox = aTextBox;
	}


	public BufferedImage getBitmap()
	{
		return mBitmap;
	}


	public void setBitmap(BufferedImage aBitmap)
	{
		mBitmap = aBitmap;
	}


	public String getCharacter()
	{
		return mCharacter;
	}


	public byte getGray(int x, int y)
	{
		if (x < 0 || y < 0 || x >= mBitmap.getWidth() || y >= mBitmap.getHeight())
		{
			return Page.WHITE;
		}

		return (byte)mBitmap.getRGB(x,y);
	}


	@Override
	public String toString()
	{
		return mCharacter;
	}


	public String getFontName()
	{
		return mFontName;
	}
}