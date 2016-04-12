package org.terifan.ocr;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;


public class OCREngine
{
	private Page mPage;
	private Resolver mResolver;
	private ArrayList<TextBox> mTextBoxes;
	private ArrayList<TextBox> mScanResult;
	private CurvatureClassifier mCurvatureClassifier;
	private Settings mSettings;


	public OCREngine()
	{
		reset();
	}


	public void reset()
	{
		mCurvatureClassifier = new CurvatureClassifier();
		mResolver = null;
	}


	public void learnAlphabet(String aFontName, Bitmap aBitmap)
	{
		learnAlphabet(aFontName, aBitmap, null);
	}


	public void learnAlphabet(String aFontName, Bitmap aBitmap, String aAlphabet)
	{
		mCurvatureClassifier.learn(aFontName, aBitmap, aAlphabet);
	}


	public void clearAlphabets()
	{
		mCurvatureClassifier.reset();
	}


	public void loadPage(Page aPage, Settings aSettings)
	{
		loadPage(aPage, aSettings, 0, 0, 1, 1);
	}


	public void loadPage(Rectangle2D aRect, Page aPage, Settings aSettings)
	{
		loadPage(aPage, aSettings, aRect.getX(), aRect.getY(), aRect.getWidth(), aRect.getHeight());
	}


	public void loadPage(Page aPage, Settings aSettings, double aFromX, double aFromY, double aToX, double aToY)
	{
		mPage = aPage;
		mSettings = aSettings;
		mTextBoxes = new PageSegmenter().scanPage(aFromX, aFromY, aToX, aToY, mPage, mSettings);
	}


	public boolean scanRelative(TextBox aBox, double aOffsetX, double aOffsetY, double aWidth, double aHeight, Resolver aResolver)
	{
		double x1 = aBox.x / (double)mPage.getWidth()  + aOffsetX;
		double y1 = aBox.y / (double)mPage.getHeight() + aOffsetY;

		double x2 = x1 + aWidth;
		double y2 = y1 + aHeight;

		if (x2 < x1)
		{
			double t = x2; x2 = x1; x1 = t;
		}
		if (y2 < y1)
		{
			double t = y2; y2 = y1; y1 = t;
		}

		return scan(x1, y1, x2, y2, aResolver);
	}


	public boolean scan(Rectangle2D aRect, Resolver aResolver)
	{
		return scan(aRect.getX(), aRect.getY(), aRect.getWidth(), aRect.getHeight(), aResolver);
	}


	public boolean scan(double aFromX, double aFromY, double aToX, double aToY, Resolver aResolver)
	{
		if (mTextBoxes == null)
		{
			throw new IllegalStateException("Page not loaded - call load method first.");
		}

		mResolver = aResolver;

		ArrayList<TextBox> results = new ArrayList<>();

		for (TextBox box : mTextBoxes)
		{
			if (box.x + box.width > mPage.getWidth() * aFromX && box.y + box.height > mPage.getHeight() * aFromY && box.x < mPage.getWidth() * aToX && box.y < mPage.getHeight() * aToY)
			{
				box.mResults.clear();

				scanBox(box, box);

				box.mResolver = aResolver;
				box.mComplete = true;

				if (aResolver.acceptWord(mPage, box))
				{
					results.add(box);
				}
			}
		}

		mScanResult = results;

		return true;
	}


	private void scanBox(TextBox aTextBox, TextBox aRootBox)
	{
		if (aTextBox.mChildren.isEmpty())
		{
			Result result = mCurvatureClassifier.classifySymbol(mPage, aTextBox, mResolver);

			if (result == null || result.mSymbol == null)
			{
				return;
			}

			aTextBox.mResults.add(result);
			aRootBox.mResults.add(result); // ?????
		}
		else
		{
			for (TextBox box : aTextBox.mChildren)
			{
				box.mResults.clear();

				scanBox(box, aRootBox);
			}
		}
	}


	public void setPrintCharacters(boolean aPrintCharacters)
	{
		mCurvatureClassifier.setPrintCharacters(aPrintCharacters);
	}


	public ArrayList<TextBox> getTextBoxes()
	{
		return mTextBoxes;
	}


	public ArrayList<TextBox> getScanResult()
	{
		return mScanResult;
	}
}