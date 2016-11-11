package org.terifan.ocr;


public class Settings
{
	private double mCharacterAspectRatio;
	private double mCharacterSpacing;
	private int mMinSymbolWidth;
	private int mMaxSymbolWidth;
	private int mMaxSymbolHeight;
	private int mMinSymbolHeight;
	private int mMaxLineWidth;


	public Settings()
	{
		mCharacterAspectRatio = 1.4;
		mMinSymbolWidth = 1;
		mMaxSymbolWidth = 100;
		mMinSymbolHeight = 1;
		mMaxSymbolHeight = 75;
		mMaxLineWidth = 100;
	}


	public double getCharacterAspectRatio()
	{
		return mCharacterAspectRatio;
	}


	public void setCharacterAspectRatio(double aCharacterAspectRatio)
	{
		mCharacterAspectRatio = aCharacterAspectRatio;
	}


	public double getCharacterSpacing()
	{
		return mCharacterSpacing;
	}


	public void setMaxCharacterSpacing(double aCharacterSpacing)
	{
		mCharacterSpacing = aCharacterSpacing;
	}


	public int getMinSymbolWidth()
	{
		return mMinSymbolWidth;
	}


	public void setMinSymbolWidth(int aMinSymbolWidth)
	{
		mMinSymbolWidth = aMinSymbolWidth;
	}


	public int getMaxSymbolWidth()
	{
		return mMaxSymbolWidth;
	}


	public void setMaxSymbolWidth(int aMaxSymbolWidth)
	{
		mMaxSymbolWidth = aMaxSymbolWidth;
	}


	public int getMaxSymbolHeight()
	{
		return mMaxSymbolHeight;
	}


	public void setMaxSymbolHeight(int aMaxSymbolHeight)
	{
		mMaxSymbolHeight = aMaxSymbolHeight;
	}


	public int getMinSymbolHeight()
	{
		return mMinSymbolHeight;
	}


	public void setMinSymbolHeight(int aMinSymbolHeight)
	{
		mMinSymbolHeight = aMinSymbolHeight;
	}


	public int getMaxLineWidth()
	{
		return mMaxLineWidth;
	}


	public void setMaxLineWidth(int aMaxLineWidth)
	{
		mMaxLineWidth = aMaxLineWidth;
	}


	public void setMaxCharacterSpacingFraction(double aFraction, Page aPage)
	{
		mCharacterSpacing = aFraction * aPage.getWidth() / 100;
	}
}
