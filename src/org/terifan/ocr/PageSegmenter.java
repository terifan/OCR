package org.terifan.ocr;

import java.awt.Insets;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.function.DoublePredicate;


class PageSegmenter
{
	private final static int SCAN_TOP = 1;
	private final static int SCAN_LEFT = 2;
	private final static int SCAN_BOTTOM = 4;
	private final static int SCAN_RIGHT = 8;

	private Page mPage;
	public boolean mLearning; // TODO: remove


	public PageSegmenter()
	{
	}


	public ArrayList<TextBox> scanPage(double aFromX, double aFromY, double aToX, double aToY, Page aPage, double aCharacterAspectRatio, double aCharacterSpacing, int aMinSymbolWidth, int aMaxSymbolWidth, int aMinSymbolHeight, int aMaxSymbolHeight, int aMaxLineWidth)
	{
		mPage = aPage;

		ArrayList<Rectangle> charRects = findCharacterRectangles(aFromX, aFromY, aToX, aToY, aMinSymbolWidth, aMaxLineWidth, aMinSymbolHeight, aMaxSymbolHeight);

		ArrayList<TextBox> textBoxes = findWordRectangles(charRects, aCharacterSpacing, aMinSymbolHeight, aMaxSymbolHeight);

		for (TextBox box : textBoxes)
		{
			splitTextBox(box, aCharacterAspectRatio, aMinSymbolWidth, aMaxSymbolWidth);
		}

		return textBoxes;
	}


	private void splitTextBox(TextBox aTextBox, double aCharacterAspectRatio, int aMinSymbolWidth, int aMaxSymbolWidth)
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
				symCount = (int)Math.ceil(width / (aCharacterAspectRatio * (aTextBox.height - borders.top - borders.bottom)));
			}

			if (width > 0 && symCount <= 1)
			{
				aTextBox.mChildren.add(new TextBox(aTextBox, charIndex, aTextBox.x + x, aTextBox.y, width, aTextBox.height));
				charIndex++;
			}
			else if (width > 0)
			{
				charIndex = splitCharacter(width, symCount, aTextBox, x, aMinSymbolWidth, charIndex);
			}
		}
	}


	private int splitCharacter(int aWidth, int aSymCount, TextBox aTextBox, int aX, int aMinSymbolWidth, int aCharIndex)
	{
		double sw = aWidth / (double)aSymCount;
		int seekRange = aMinSymbolWidth / 4;
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

			if (split > prevSplit && split - prevSplit > aMinSymbolWidth)
			{
				int tmpX = aTextBox.x + aX + prevSplit - 1;
				int tmpW = split - prevSplit;

				aTextBox.mChildren.add(new TextBox(aTextBox, aCharIndex++, tmpX, aTextBox.y, tmpW, aTextBox.height));
			}

			prevSplit = split;
		}

		if (aWidth - prevSplit > aMinSymbolWidth)
		{
			aTextBox.mChildren.add(new TextBox(aTextBox, aCharIndex++, aTextBox.x + aX + prevSplit, aTextBox.y, aWidth - prevSplit, aTextBox.height));
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


	/**
	 * Combine characters into words
	 */
	private ArrayList<TextBox> findWordRectangles(ArrayList<Rectangle> aCharacterRectangles, double aCharacterSpacing, int aMinSymbolHeight, int aMaxSymbolHeight)
	{
		ArrayList<TextBox> result = new ArrayList<>();

		Rectangle r = new Rectangle();
		Rectangle q = new Rectangle();

		int sensorSize = (int)aCharacterSpacing;

		while (!aCharacterRectangles.isEmpty())
		{
			TextBox textBox = new TextBox(aCharacterRectangles.remove(0));
			result.add(textBox);

			for (boolean stop = false; !stop;)
			{
				q.setBounds(textBox);

				int sw = sensorSize;
				int sh = q.height / 2;

				q.x -= sw / 2;
				q.y += q.height / 2 - sh / 2;
				q.width += sw;
				q.height = sh;

				stop = true;

				for (int i = 0; i < aCharacterRectangles.size(); i++)
				{
					r.setBounds(aCharacterRectangles.get(i));

					sw = sensorSize;
					sh = r.height / 2;

					r.x -= sw / 2;
					r.y += r.height / 2 - sh / 2;
					r.width += sw;
					r.height = sh;

					if (q.intersects(r))
					{
						textBox.add(aCharacterRectangles.remove(i));
						stop = false;
						break;
					}
				}
			}
		}

		// remove text boxes that violate size requirements
		for (int i = result.size(); --i >= 0;)
		{
			TextBox rect = result.get(i);

			if (rect.height < aMinSymbolHeight || rect.height > aMaxSymbolHeight || rect.width == 0)
			{
				result.remove(i);
			}
		}

		return result;
	}


	private ArrayList<Rectangle> findCharacterRectangles(double aFromX, double aFromY, double aToX, double aToY, int aMinWidth, int aMaxWidth, int aMinHeight, int aMaxHeight)
	{
		ArrayList<Rectangle> rects = new ArrayList<>();

		int minX = (int)(aFromX * mPage.getWidth()) + 1;
		int minY = (int)(aFromY * mPage.getHeight()) + 1;
		int maxX = (int)(aToX * mPage.getWidth()) - 1;
		int maxY = (int)(aToY * mPage.getHeight()) - 1;

		for (int iy = minY; iy < maxY; iy++)
		{
			for (int ix = minX; ix < maxX; ix++)
			{
				if (mPage.isBlack(ix, iy) && !mPage.isBlack(ix, iy - 1))
				{
					int x = ix;
					int y = iy;
					int w = 2;
					int h = 2;

					// notice: allow the symbol to grow larger than the rule so that it can fail in the test below
					// this way lines and grahpics can be filtered out.
					while (h < aMaxHeight + 3 && w < aMaxWidth + 3)
					{
						int s = scanBounds(x, y, w, h);

						if ((s & SCAN_TOP) != 0)
						{
							y--;
							h++;
						}
						if ((s & SCAN_LEFT) != 0)
						{
							x--;
							w++;
						}
						if ((s & SCAN_BOTTOM) != 0)
						{
							h++;
						}
						if ((s & SCAN_RIGHT) != 0)
						{
							w++;
						}
						if (s == 0)
						{
							break;
						}
					}

					while (h > aMinHeight && w > aMinWidth)
					{
						int s = scanBounds(x, y, w, h);

						if ((s & SCAN_TOP) == 0)
						{
							y++;
							h--;
						}
						if ((s & SCAN_LEFT) == 0)
						{
							x++;
							w--;
						}
						if ((s & SCAN_BOTTOM) == 0)
						{
							h--;
						}
						if ((s & SCAN_RIGHT) == 0)
						{
							w--;
						}
						if (s == 1 + 2 + 4 + 8)
						{
							break;
						}
					}

					if (w >= aMinWidth && w <= aMaxWidth && h >= aMinHeight && h <= aMaxHeight)
					{
						Rectangle r = new Rectangle(x, y, w, h);

						if (mLearning || !rects.contains(r))
						{
							rects.add(r);
						}
					}
					else
					{
						//TODO: record rejected symbol
					}
				}
			}
		}

		return rects;
	}


	private int scanBounds(int x, int y, int w, int h)
	{
		boolean top = false;
		boolean right = false;
		boolean bottom = false;
		boolean left = false;

		for (int i = 0; i < w; i++)
		{
			if (!top && mPage.isBlack(x + i, y))
			{
				top = true;
			}
			if (!bottom && mPage.isBlack(x + i, y + h))
			{
				bottom = true;
			}
		}
		for (int i = 0; i < h; i++)
		{
			if (!left && mPage.isBlack(x, y + i))
			{
				left = true;
			}
			if (!right && mPage.isBlack(x + w, y + i))
			{
				right = true;
			}
		}

		return (top ? SCAN_TOP : 0) + (right ? SCAN_RIGHT : 0) + (bottom ? SCAN_BOTTOM : 0) + (left ? SCAN_LEFT : 0);
	}
}
