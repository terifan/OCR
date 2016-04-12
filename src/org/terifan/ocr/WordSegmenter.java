package org.terifan.ocr;

import java.awt.Insets;
import java.util.ArrayList;
import java.util.function.DoublePredicate;


class WordSegmenter
{
	private Page mPage;
	private Settings mSettings;


	public void scanPage(Page aPage, Settings aSettings, ArrayList<TextBox> aTextBoxes)
	{
		mPage = aPage;
		mSettings = aSettings;

		for (TextBox box : aTextBoxes)
		{
			splitTextBox(box);
		}
	}


	private void splitTextBox(TextBox aTextBox)
	{
		ArrayList<int[]> charRanges = getCharacterRanges(aTextBox);

		int charIndex = 0;

		for (int[] range : charRanges)
		{
			int x = range[0];
			int width = range[1];

			Insets borders = mPage.getBitmap().getBorders(aTextBox.x + x, aTextBox.y, width, aTextBox.height);

			int symCount;

			if (aTextBox.height - borders.top - borders.bottom <= 0)
			{
				symCount = 1;
			}
			else
			{
				symCount = (int)Math.ceil(width / (mSettings.getCharacterAspectRatio() * (aTextBox.height - borders.top - borders.bottom)));
			}

			if (width > 0 && symCount <= 1)
			{
				aTextBox.addChild(new TextBox(aTextBox, charIndex, aTextBox.x + x, aTextBox.y, width, aTextBox.height));
				charIndex++;
			}
			else if (width > 0)
			{
				charIndex = splitCharacter(width, symCount, aTextBox, x, charIndex);
			}
		}
	}


	private int splitCharacter(int aWidth, int aSymCount, TextBox aTextBox, int aX, int aCharIndex)
	{
		double sw = aWidth / (double)aSymCount;
		int seekRange = mSettings.getMinSymbolWidth() / 4;
		int prevSplit = 0;

		for (int si = 1; si < aSymCount; si++)
		{
			int split = 0;

			double fill = Integer.MAX_VALUE;

			for (int sx = -seekRange; sx <= seekRange; sx++)
			{
				int splitPos = (int)(sw * si + sx);

				if (splitPos <= prevSplit || splitPos > aWidth)
				{
					continue;
				}

				double f = countVerticalLineFillRatio(aTextBox.x + aX + splitPos, aTextBox.y, aTextBox.height);

				if (f < fill)
				{
					fill = f;
					split = splitPos;
				}
			}

			if (split > prevSplit && split - prevSplit > mSettings.getMinSymbolWidth())
			{
				int tmpX = aTextBox.x + aX + prevSplit - 1;
				int tmpW = split - prevSplit;

				aTextBox.addChild(new TextBox(aTextBox, aCharIndex++, tmpX, aTextBox.y, tmpW, aTextBox.height));
			}

			prevSplit = split;
		}

		if (aWidth - prevSplit > mSettings.getMinSymbolWidth())
		{
			aTextBox.addChild(new TextBox(aTextBox, aCharIndex++, aTextBox.x + aX + prevSplit, aTextBox.y, aWidth - prevSplit, aTextBox.height));
		}

		return aCharIndex;
	}


	private ArrayList<int[]> getCharacterRanges(TextBox aTextBox)
	{
		ArrayList<int[]> charRanges = new ArrayList<>();

		for (int x = 0; x < aTextBox.width; x++)
		{
			int width = findVerticalLineFillRatio(aTextBox.x + x, aTextBox.y, aTextBox.width - x, aTextBox.height, ratio->ratio>0); // amount of white space in front of symbol

			if (width == -1)
			{
				break;
			}

			x += width;

			width = findVerticalLineFillRatio(aTextBox.x + x, aTextBox.y, aTextBox.width - x, aTextBox.height, ratio->ratio==0);

			if (width == -1)
			{
				width = aTextBox.width - x;
			}

			if (width > 0)
			{
				charRanges.add(new int[]{x, width});

				x += width;
			}
		}

		return charRanges;
	}


	private int findVerticalLineFillRatio(int aX, int aY, int aWidth, int aHeight, DoublePredicate aPredicate)
	{
		for (int i = 0; i < aWidth; i++)
		{
			if (aPredicate.test(countVerticalLineFillRatio(aX + i, aY, aHeight)))
			{
				return i;
			}
		}

		return -1;
	}


	private double countVerticalLineFillRatio(int aX, int aY, int aHeight)
	{
		int iw = mPage.getWidth();
		int ih = mPage.getHeight();
		int n = 0;
		int s = 0;

		if (aX < 0 || aX >= iw)
		{
			return 0;
		}

		for (int y = Math.max(aY, 0), maxY = Math.min(aY + aHeight, ih); y < maxY; y++)
		{
			if (mPage.isBlack(aX, y))
			{
				s++;
			}
			n++;
		}

		return s / (double)n;
	}
}
