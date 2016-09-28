package org.terifan.ocr;

import java.awt.Rectangle;
import java.util.ArrayList;


class PageSegmenter
{
	private final static int SCAN_TOP = 1;
	private final static int SCAN_LEFT = 2;
	private final static int SCAN_BOTTOM = 4;
	private final static int SCAN_RIGHT = 8;

	private Page mPage;
	private Settings mSettings;
	private ArrayList<Rectangle> mCharacterRectangles;
	private ArrayList<TextBox> mWordRectangles;


	public PageSegmenter()
	{
	}


	public ArrayList<TextBox> scanPage(double aFromX, double aFromY, double aToX, double aToY, Page aPage, Settings aSettings)
	{
		mPage = aPage;
		mSettings = aSettings;

		mCharacterRectangles = new ArrayList<>();
		mWordRectangles = new ArrayList<>();

		findCharacterRectangles(aFromX, aFromY, aToX, aToY);

		findWordRectangles();

		new WordSegmenter().scanPage(aPage, aSettings, mWordRectangles);

		return mWordRectangles;
	}


	/**
	 * Combine characters into words
	 */
	private void findWordRectangles()
	{
		Rectangle r = new Rectangle();
		Rectangle q = new Rectangle();

		int sensorSize = (int)mSettings.getCharacterSpacing();

		while (!mCharacterRectangles.isEmpty())
		{
			TextBox textBox = new TextBox(mCharacterRectangles.remove(0));
			mWordRectangles.add(textBox);

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

				for (int i = 0; i < mCharacterRectangles.size(); i++)
				{
					r.setBounds(mCharacterRectangles.get(i));

					sw = sensorSize;
					sh = r.height / 2;

					r.x -= sw / 2;
					r.y += r.height / 2 - sh / 2;
					r.width += sw;
					r.height = sh;

					if (q.intersects(r))
					{
						textBox.add(mCharacterRectangles.remove(i));
						stop = false;
						break;
					}
				}
			}
		}

		// remove text boxes that violate size requirements
		for (int i = mWordRectangles.size(); --i >= 0;)
		{
			TextBox rect = mWordRectangles.get(i);

			if (rect.height < mSettings.getMinSymbolHeight() || rect.height > mSettings.getMaxSymbolHeight() || rect.width == 0)
			{
				mWordRectangles.remove(i);
			}
		}
	}


	private void findCharacterRectangles(double aFromX, double aFromY, double aToX, double aToY)
	{
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
					while (h < mSettings.getMaxSymbolHeight() + 3 && w < mSettings.getMaxSymbolWidth() + 3)
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

					while (h > mSettings.getMinSymbolHeight() && w > mSettings.getMinSymbolWidth())
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

					if (w >= mSettings.getMinSymbolWidth() && w <= mSettings.getMaxSymbolWidth() && h >= mSettings.getMinSymbolHeight() && h <= mSettings.getMaxSymbolHeight())
					{
						Rectangle r = new Rectangle(x, y, w, h);

						if (!mCharacterRectangles.contains(r))
						{
							mCharacterRectangles.add(r);
						}
					}
					else
					{
						//TODO: record rejected symbol
					}
				}
			}
		}
	}


	private int scanBounds(int x, int y, int w, int h)
	{
		boolean top = false;
		boolean right = false;
		boolean bottom = false;
		boolean left = false;

		w = Math.min(w, mPage.getWidth() - x - 1);
		h = Math.min(h, mPage.getHeight() - y - 1);

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
