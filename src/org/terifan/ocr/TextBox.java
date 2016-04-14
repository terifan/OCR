package org.terifan.ocr;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;


public class TextBox extends Rectangle
{
	private ArrayList<TextBox> mChildren = new ArrayList<>();
	private ArrayList<Result> mResults = new ArrayList<>();
	private int mIndex; // relative to first TextBox in root TextBox
	private TextBox mParent;
	private boolean mComplete;
	private Resolver mResolver;
	private BufferedImage mBitmap;
	private BufferedImage mSymbolBitmap;
	private BufferedImage mNormalizedBitmap;


	TextBox(TextBox aParent, int aIndex, int x, int y, int w, int h)
	{
		super(x, y, w, h);

		if (w <= 0 || h <= 0)
		{
			throw new IllegalArgumentException("Illegal size: width: " + w + ", height: "+ h);
		}

		mParent = aParent;
		mIndex = aIndex;
	}


	TextBox(Rectangle rect)
	{
		super(rect);
	}


	public TextBox getParent()
	{
		return mParent;
	}


	public double getScore()
	{
		double score = 0;
		for (Result r : mResults)
		{
			score += r.mScore;
		}
		return score / mResults.size();
	}


	Resolver getResolver()
	{
		return mResolver;
	}


	void setResolver(Resolver aResolver)
	{
		mResolver = aResolver;
	}


	public boolean isComplete()
	{
		return mComplete;
	}


	void setComplete(boolean aComplete)
	{
		mComplete = aComplete;
	}


	public int getIndex()
	{
		return mIndex;
	}


	public void addChild(TextBox aTextBox)
	{
		mChildren.add(aTextBox);
	}


	public ArrayList<TextBox> getChildren()
	{
		return mChildren;
	}


	public ArrayList<Result> getResults()
	{
		return mResults;
	}


	public Rectangle getRectangle()
	{
		return new Rectangle(x, y, width, height);
	}


	public String getDefaultString()
	{
		StringBuilder word = new StringBuilder();
		for (Result r : mResults)
		{
			word.append(r.mSymbol.mDefCharacter);
		}
		return word.toString();
	}


	@Override
	public String toString()
	{
		StringBuilder word = new StringBuilder();
		for (Result r : mResults)
		{
			word.append(r.mSymbol.mCharacter);
		}
		return word.toString();
	}


	public BufferedImage getBitmap()
	{
		return mBitmap;
	}


	public void setBitmap(BufferedImage aBitmap)
	{
		mBitmap = aBitmap;
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