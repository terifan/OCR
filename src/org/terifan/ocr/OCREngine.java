package org.terifan.ocr;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;


public class OCREngine
{
	public final static int DEBUG_COMPONENT = 1;

	private CurvatureClassifier mCurvatureClassifier;
	private Page mPage;
	private Resolver mResolver;
	private ArrayList<TextBox> mTextBoxes;

	private double mCharacterAspectRatio;
	private double mCharacterSpacing;

	private double mCharacterSpacingFraction;
	private double mCharacterSpacingExact;
	private int mMinSymbolWidth;
	private int mMaxSymbolWidth;
	private int mMaxSymbolHeight;
	private int mMinSymbolHeight;
	private int mMaxLineWidth;


	public OCREngine()
	{
		reset();
	}


	public void reset()
	{
		mCurvatureClassifier = new CurvatureClassifier();
		mResolver = null;
		mCharacterAspectRatio = 1.4;
		mMinSymbolWidth = 1;
		mMaxSymbolWidth = 100;
		mMinSymbolHeight = 1;
		mMaxSymbolHeight = 75;
		mMaxLineWidth = 100;
	}


	public void learnAlphabet(String aFontName, Page aPage)
	{
		learnAlphabet(aFontName, aPage, null);
	}


	public void learnAlphabet(String aFontName, Page aPage, String aAlphabet)
	{
		Page tmp = mPage;
		mCurvatureClassifier.learn(aFontName, aPage, aAlphabet);
		mCurvatureClassifier.init(tmp);
	}


	public void clearAlphabets()
	{
		mCurvatureClassifier.clearAlphabets();
	}


	public void setMaxLineWidth(int aMaxLineWidth)
	{
		mMaxLineWidth = aMaxLineWidth;
	}


	public int getMaxLineWidth()
	{
		return mMaxLineWidth;
	}


	public void setMinSymbolWidth(int aMinSymbolWidth)
	{
		mMinSymbolWidth = aMinSymbolWidth;
	}


	public int getMinSymbolWidth()
	{
		return mMinSymbolWidth;
	}


	public void setMaxSymbolWidth(int aMaxSymbolWidth)
	{
		mMaxSymbolWidth = aMaxSymbolWidth;
	}


	public int getMaxSymbolWidth()
	{
		return mMaxSymbolWidth;
	}


	public void setMinSymbolHeight(int aMinSymbolHeight)
	{
		mMinSymbolHeight = aMinSymbolHeight;
	}


	public void setMaxSymbolHeight(int aMaxSymbolHeight)
	{
		mMaxSymbolHeight = aMaxSymbolHeight;
	}


	public int getMaxRowHeight()
	{
		return mMaxSymbolHeight;
	}


	/**
	 * Sets the width / height aspect ration of a font. Default is 1.4 which
	 * indicate a font being 40% heigher than wide.
	 *
	 * @param aCharacterAspectRatio
	 */
	public void setCharacterAspectRatio(double aCharacterAspectRatio)
	{
		mCharacterAspectRatio = aCharacterAspectRatio;
	}


	/**
	 * Sets the character spacing limit. Must be set before a page is loaded to
	 * have any effect.
	 *
	 * When aSpacing equals zero (0.0) then a default value is calculated using this formula:
	 * <code>
	 * spacing = (8 * PageWidth / 2480.0)
	 * </code>
	 *
	 * If aSpacing is less than 1.0 then a relative value is calculated using this formula:
	 * <code>
	 * aSpacing * PageWidth / 100
	 * </code>
	 *
	 * If aSpacing is equal or greater than 1.0 then that value is used, measure in pixels.
	 *
	 * @param aSpacing
	 *   a spacing value in pixels
	 */
	public void setCharacterSpacingFraction(double aSpacing)
	{
		if (aSpacing < 0)
		{
			throw new IllegalArgumentException("aSpacing < 0");
		}

		mCharacterSpacingFraction = aSpacing;
		mCharacterSpacingExact = 0;
	}


	/**
	 * Sets the maximum spacing for characters measured in pixels.
	 *
	 * @param aSpacing
	 *   the spacing in pixels
	 */
	public void setCharacterSpacingExact(int aSpacing)
	{
		if (aSpacing < 0)
		{
			throw new IllegalArgumentException("aSpacing < 0");
		}

		mCharacterSpacingFraction = 0;
		mCharacterSpacingExact = aSpacing;
	}


	public void loadPage(Page aPage)
	{
		loadPage(0, 0, 1, 1, aPage);
	}


	public void loadPage(Rectangle2D aRect, Page aPage)
	{
		loadPage(aRect.getX(), aRect.getY(), aRect.getWidth(), aRect.getHeight(), aPage);
	}


	public void loadPage(double aFromX, double aFromY, double aToX, double aToY, Page aPage)
	{
		// TODO: image might already be up-to-date
		aPage.updateImage();

		mPage = aPage;
		mCurvatureClassifier.init(aPage);

		if (mCharacterSpacingExact != 0)
		{
			mCharacterSpacing = mCharacterSpacingExact;
		}
		else if (mCharacterSpacingFraction != 0)
		{
			mCharacterSpacing = mCharacterSpacingFraction * mPage.getWidth() / 100;
		}
		else
		{
			mCharacterSpacing = 8 * mPage.getWidth() / 2480.0;
		}

		mTextBoxes = new PageSegmenter().scanPage(aFromX, aFromY, aToX, aToY, mPage, mCharacterAspectRatio, mCharacterSpacing, mMinSymbolWidth, mMaxSymbolWidth, mMinSymbolHeight, mMaxSymbolHeight, mMaxLineWidth);
	}


	public ArrayList<TextBox> scanRelative(TextBox aBox, double aOffsetX, double aOffsetY, double aWidth, double aHeight, Resolver aResolver)
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


	public ArrayList<TextBox> scan(Rectangle2D aRect, Resolver aResolver)
	{
		return scan(aRect.getX(), aRect.getY(), aRect.getWidth(), aRect.getHeight(), aResolver);
	}


	public ArrayList<TextBox> scan(double aFromX, double aFromY, double aToX, double aToY, Resolver aResolver)
	{
		boolean debug = OCREngine.isDebugEnabled(mPage);

		if (mTextBoxes == null)
		{
			throw new IllegalStateException("Page not loaded - call load method first.");
		}

		mResolver = aResolver;

//		if (debug)
//		{
//			mPage.mDebugGraphics.setColor(new Color(128,128,128,96));
//			mPage.mDebugGraphics.fillRect((int)(aFromX*mPage.getWidth()), (int)(aFromY*mPage.getHeight()), (int)((aToX-aFromX)*mPage.getWidth()), (int)((aToY-aFromY)*mPage.getHeight()));
//		}

		ArrayList<TextBox> results = new ArrayList<>();

		boolean debugFoundBoxes = false;

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

					if (debug)
					{
						debugFoundBoxes = true;
						mPage.mDebugGraphics.setColor(new Color(0,0,255,128));
						mPage.mDebugGraphics.draw(box);
					}
				}
				else
				{
					if (debug)
					{
						debugFoundBoxes = true;
						mPage.mDebugGraphics.setColor(new Color(255,0,0,128));
						mPage.mDebugGraphics.drawLine(box.x, box.y, box.x+box.width, box.y+box.height);
						mPage.mDebugGraphics.draw(box);
					}
				}
			}
		}

		if (debug && !debugFoundBoxes)
		{
			for (TextBox box : mTextBoxes)
			{
				mPage.mDebugGraphics.setColor(new Color(0,255,0,128));
				mPage.mDebugGraphics.drawRect(box.x, box.y, box.width-1, box.height-1);
			}
		}

		return results;
	}


	private void scanBox(TextBox aTextBox, TextBox aRootBox)
	{
		boolean debug = OCREngine.isDebugEnabled(mPage);

		if (aTextBox.mChildren.size() > 0)
		{
			for (TextBox box : aTextBox.mChildren)
			{
				box.mResults.clear();

				scanBox(box, aRootBox);
			}
		}
		else
		{
			Result result = mCurvatureClassifier.classifySymbol(aTextBox, mResolver);

			if (result == null || result.mSymbol == null)
			{
				if (debug)
				{
					mPage.mDebugGraphics.setFont(new Font("arial",Font.PLAIN, aTextBox.height));
					mPage.mDebugGraphics.setColor(Color.MAGENTA);
					mPage.mDebugGraphics.drawString("\u00bf", aTextBox.x, aTextBox.y+aTextBox.height*2/3);
				}

				return;
			}

			aTextBox.mResults.add(result);
			aRootBox.mResults.add(result);
		}
	}


	public static boolean isDebugEnabled(Page aPage)
	{
		if (aPage.mDebugGraphics == null)
		{
			return false;
		}

		StackTraceElement[] stackTrace = new Exception().getStackTrace();
		String methodName = stackTrace[1].getMethodName();

		switch (methodName)
		{
			case "-":
			case "scan":
			case "classifySymbol":

//				return true;

			case "extractCurvature":
			case "findCharacterRectangles":
			case "findTextRectangles":
			case "scanBox":
			case "splitTextBox":
			case "scanPage":
			case "getCharacterRanges":
			case "splitCharacter":
			case "extractContour":
			case "extractSlopes":
			case "extractCurvatureVector":
			case "classifySymbolByCurvature":
			case "classifySymbolByTemplate":
			case "classifySymbolByContour":
			case "learnSymbol":
				return false;
			default:
				System.out.println("debugable method: " + methodName);
				return false;
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
}