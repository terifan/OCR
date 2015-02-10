package org.terifan.ocr;

import java.awt.Rectangle;
import java.util.ArrayList;


public class TextBox extends Rectangle
{
	protected ArrayList<TextBox> mChildren = new ArrayList<>();
	protected ArrayList<Result> mResults = new ArrayList<>();
	protected int mIndex; // relative to first TextBox in root TextBox
	protected TextBox mParent;
	protected boolean mComplete;
	protected Resolver mResolver;


	public TextBox(TextBox aParent, int aIndex, int x, int y, int w, int h)
	{
		super(x, y, w, h);

		if (w <= 0 || h <= 0)
		{
			throw new IllegalArgumentException("Illegal size: width: " + w + ", height: "+ h);
		}

		mParent = aParent;
		mIndex = aIndex;
	}


	public TextBox(Rectangle rect)
	{
		super(rect);
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


	public double getScore()
	{
		double score = 0;
		for (Result r : mResults)
		{
			score += r.mScore;
		}
		return score / mResults.size();
	}


	public int getIndex()
	{
		return mIndex;
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
}