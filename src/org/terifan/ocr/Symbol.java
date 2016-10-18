package org.terifan.ocr;

import java.awt.Insets;
import java.awt.Polygon;
import java.awt.image.BufferedImage;


public class Symbol
{
	private Bitmap mBitmap;
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
	protected BufferedImage mSymbolBitmap;
	String[][] mSlopesX;
	int[][] mSlopes2;


	public Symbol(TextBox aTextBox)
	{
		mTextBox = aTextBox;
	}


	public String getFontName()
	{
		return mFontName;
	}


	public Bitmap getBitmap()
	{
		return mBitmap;
	}


	public void setBitmap(Bitmap aBitmap)
	{
		mBitmap = aBitmap;
	}


	public String getCharacter()
	{
		return mCharacter;
	}


	@Override
	public String toString()
	{
		return mCharacter;
	}


	public BufferedImage getSymbolBitmap()
	{
		return mSymbolBitmap;
	}


	public void setSymbolBitmap(BufferedImage aSymbolBitmap)
	{
		mSymbolBitmap = aSymbolBitmap;
	}
}