package org.terifan.ocr;

import java.awt.Insets;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;


public class Symbol
{
	protected Insets mBorders;
	protected BufferedImage mBitmap;
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


	public Symbol(TextBox aTextBox)
	{
		mTextBox = aTextBox;
	}


	public String getCharacter()
	{
		return mCharacter;
	}


	public int getRGB(int x, int y)
	{
		if (x < 0 || y < 0 || x >= mBitmap.getWidth() || y >= mBitmap.getHeight())
		{
			return -1;
		}

		return mBitmap.getRGB(x,y);
	}


	public int getGray(int x, int y)
	{
		if (x < 0 || y < 0 || x >= mBitmap.getWidth() || y >= mBitmap.getHeight())
		{
			return -1;
		}

		int c = mBitmap.getRGB(x,y);

		return ((0xff & (c >> 16)) + (0xff & (c >> 8)) + (0xff & c)) / 3;
	}


	@Override
	public String toString()
	{
		return mCharacter;
	}


	public void write(String aFile)
	{
		try
		{
			ImageIO.write(mBitmap, "png", new File(aFile));
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}


	public String getFontName()
	{
		return mFontName;
	}
}