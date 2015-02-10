package org.terifan.ocr;

import java.awt.Color;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.ArrayList;
import org.terifan.util.log.Log;


public class PageSegmenter
{
	private Page mPage;
	public boolean mLearning; // TODO: remove


	public PageSegmenter()
	{
	}


	public ArrayList<TextBox> scanPage(double aFromX, double aFromY, double aToX, double aToY, Page aPage, double aCharacterAspectRatio, double aCharacterSpacing, int aMinSymbolWidth, int aMaxSymbolWidth, int aMinSymbolHeight, int aMaxSymbolHeight, int aMaxLineWidth)
	{
		mPage = aPage;

		boolean debug = OCREngine.isDebugEnabled(mPage);

		ArrayList<Rectangle> charRects = findCharacterRectangles(aFromX, aFromY, aToX, aToY, aMinSymbolWidth, aMaxLineWidth, aMinSymbolHeight, aMaxSymbolHeight);

		if (debug)
		{
			for (Rectangle rect : charRects)
			{
				mPage.mDebugGraphics.setColor(new Color(255, 255, 0, 128));
				mPage.mDebugGraphics.fill(rect);
			}
		}

		ArrayList<TextBox> textBoxes = findTextRectangles(charRects, aCharacterSpacing, aMinSymbolHeight, aMaxSymbolHeight);

		if (debug)
		{
			for (TextBox box : textBoxes)
			{
				mPage.mDebugGraphics.setColor(new Color(255, 0, 0, 128));
				mPage.mDebugGraphics.drawRect(box.x - 1, box.y - 1, box.width + 2, box.height + 2);
			}
		}

		for (TextBox box : textBoxes)
		{
			splitTextBox(box, aCharacterAspectRatio, aMinSymbolWidth, aMaxSymbolWidth);
		}

		if (debug)
		{
			for (TextBox box : textBoxes)
			{
				mPage.mDebugGraphics.setColor(new Color(0, 255, 255, 128));
				mPage.mDebugGraphics.drawRect(box.x - 2, box.y - 2, box.width + 4, box.height + 4);
			}
		}

		return textBoxes;
	}


	private void splitTextBox(TextBox aTextBox, double aCharacterAspectRatio, int aMinSymbolWidth, int aMaxSymbolWidth)
	{
		boolean debug = OCREngine.isDebugEnabled(mPage);

		if (debug)
		{
			mPage.mDebugGraphics.setColor(new Color(255, 255, 0, 128));
			mPage.mDebugGraphics.drawRect(aTextBox.x - 1, aTextBox.y - 1, aTextBox.width + 2, aTextBox.height + 2);
		}

		ArrayList<int[]> charRanges = getCharacterRanges(aTextBox);

		int charIndex = 0;

		for (int[] range : charRanges)
		{
			int x = range[0];
			int width = range[1];

			Insets borders = Tools.getBorders(mPage, aTextBox.x + x, aTextBox.y, width, aTextBox.height);

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

		if (debug)
		{
			for (TextBox tb : aTextBox.getChildren())
			{
				mPage.mDebugGraphics.setColor(new Color(0, 255, 0, 128));
				mPage.mDebugGraphics.draw(tb);
			}
		}
	}


	private int splitCharacter(int aWidth, int aSymCount, TextBox aTextBox, int aX, int aMinSymbolWidth, int aCharIndex)
	{
		boolean debug = OCREngine.isDebugEnabled(mPage);

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

				double f = scanLine(aTextBox.x + aX + splitPos, aTextBox.y, aTextBox.height);

				if (f < fill)
				{
					fill = f;
					split = splitPos;
				}
			}

			if (split > prevSplit && split - prevSplit > aMinSymbolWidth)
			{
				if (debug)
				{
					mPage.mDebugGraphics.setColor(new Color(255, 0, 255, 128));
					mPage.mDebugGraphics.drawLine(aTextBox.x+split, aTextBox.y-3, aTextBox.x+split, aTextBox.y + aTextBox.height+4);
				}

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
		boolean debug = OCREngine.isDebugEnabled(mPage);

		ArrayList<int[]> charRanges = new ArrayList<>();

		for (int x = 0; x < aTextBox.width; x++)
		{
			int width = findStartLine(aTextBox.x + x, aTextBox.y, aTextBox.width - x, aTextBox.height); // amount of white space in front of symbol

			if (width == -1)
			{
				break;
			}

			x += width;

			width = findEndLine(aTextBox.x + x, aTextBox.y, aTextBox.width - x, aTextBox.height);

			if (width == -1)
			{
				width = aTextBox.width - x;
			}
			if (width == 0)
			{
				continue;
			}

			charRanges.add(new int[]{x, width});

			if (debug)
			{
				mPage.mDebugGraphics.setColor(new Color(0, 0, 255, 128));
				mPage.mDebugGraphics.drawLine(aTextBox.x + x, aTextBox.y, aTextBox.x + x, aTextBox.y + aTextBox.height + 5);
				mPage.mDebugGraphics.setColor(new Color(255, 0, 0, 128));
				mPage.mDebugGraphics.drawLine(aTextBox.x + x + width, aTextBox.y - 5, aTextBox.x + x + width, aTextBox.y + aTextBox.height);
			}

			x += width;
		}

		return charRanges;
	}


	/**
	 * Find a vertical line with at least one black pixel
	 */
	private int findStartLine(int x, int y, int w, int h)
	{
		for (int i = 0; i < w; i++)
		{
			if (scanLine(x + i, y, h) > 0)
			{
				return i;
			}
		}
		return -1;
	}


	/**
	 * Trace the outline of a symbol until the rightmost pixel is found
	 */
	private int findEndLine(int x, int y, int w, int h)
	{
		for (int i = 0; i < w; i++)
		{
			if (scanLine(x + i, y, h) == 0)
//			if (findPath(x + i, y, h))
			{
				return i - 1;
			}
		}
		return -1;
	}


	private boolean findPath(int x, int y, int h)
	{
		boolean[][] raster = mPage.getRaster();

		for (; h > 0 && y > 0 && x > 0; y++, h--)
		{
			boolean b = raster[y][x] || raster[y - 1][x];

			if (b)
			{
				b = raster[y - 1][x - 1] || raster[y + 1][x - 1];
			}

			if (b)
			{
				b = raster[y - 1][x + 1] || raster[y + 1][x + 1];
			}

			if (b)
			{
				return false;
			}
		}
		return true;
	}


	private double scanLine(int x, int y, int h)
	{
		int s = 0;
		for (int i = 0; i < h; i++)
		{
			s += mPage.getRaster()[y + i][x] ? 1 : 0;
		}
		return s / (double)h;
	}


	private ArrayList<TextBox> findTextRectangles(ArrayList<Rectangle> aCharacterRectangles, double aCharacterSpacing, int aMinSymbolHeight, int aMaxSymbolHeight)
	{
		boolean debug = OCREngine.isDebugEnabled(mPage);

		if (debug)
		{
			mPage.mDebugGraphics.setColor(new Color(0, 255, 0, 128));
			for (Rectangle rect : aCharacterRectangles)
			{
				mPage.mDebugGraphics.draw(rect);
			}
		}

		ArrayList<TextBox> textRects = new ArrayList<>();

		Rectangle r = new Rectangle();
		Rectangle q = new Rectangle();

		int sensorSize = (int)(aCharacterSpacing);

		while (aCharacterRectangles.size() > 0)
		{
			TextBox textBox = new TextBox(aCharacterRectangles.remove(0));
			textRects.add(textBox);

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

					if (debug)
					{
						mPage.mDebugGraphics.setColor(new Color(255, 255, 0, 32));
						mPage.mDebugGraphics.drawRect(r.x, r.y, r.width - 1, r.height - 1);
					}

					if (q.intersects(r))
					{
						textBox.add(aCharacterRectangles.remove(i));
						stop = false;
						break;
					}
				}
			}

//			if (debug)
//			{
//				Rectangle tmp = new Rectangle(textBox);
//
//				int sw = sensorSize;
//				int sh = tmp.height/2;
//
//				tmp.x -= sw/2;
//				tmp.y += tmp.height/2-sh/2;
//				tmp.width += sw;
//				tmp.height = sh;
//
//				mPage.mDebugGraphics.setColor(new Color(0,0,255,128));
//				mPage.mDebugGraphics.draw(tmp);
//			}
		}

		// remove text boxes that violate size requirements
		for (int i = textRects.size(); --i >= 0;)
		{
			TextBox rect = textRects.get(i);

			if (rect.height < aMinSymbolHeight || rect.height > aMaxSymbolHeight || rect.width < 1)
			{
				textRects.remove(i);

				if (debug)
				{
					mPage.mDebugGraphics.setColor(new Color(255, 0, 0));
					mPage.mDebugGraphics.draw(rect);
					mPage.mDebugGraphics.drawLine(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
				}
			}
		}

		if (debug)
		{
			mPage.mDebugGraphics.setColor(new Color(0, 255, 0, 64));
			for (Rectangle rect : textRects)
			{
				mPage.mDebugGraphics.draw(rect);
			}
		}

		return textRects;
	}


	private ArrayList<Rectangle> findCharacterRectangles(double aFromX, double aFromY, double aToX, double aToY, int aMinWidth, int aMaxWidth, int aMinHeight, int aMaxHeight)
	{
		boolean debug = OCREngine.isDebugEnabled(mPage);

		ArrayList<Rectangle> rects = new ArrayList<>();

		int minX = (int)(aFromX * mPage.getWidth()) + 1;
		int minY = (int)(aFromY * mPage.getHeight()) + 1;
		int maxX = (int)(aToX * mPage.getWidth()) - aMaxWidth - 3;
		int maxY = (int)(aToY * mPage.getHeight()) - aMaxHeight - 3;

		for (int iy = minY; iy < maxY; iy++)
		{
			for (int ix = minX; ix < maxX; ix++)
			{
				if (mPage.getRaster(ix, iy) && !mPage.getRaster(ix, iy - 1))
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
//if (debug)
//{
//	if(x>1621+82 && y>2274 && x<1621+107 && y<2328)
//	{
//		mPage.mGraphics.setColor(new Color(255,255,0,128));
//		mPage.mGraphics.drawRect(x, y, w, h);
//		try{ImageIO.write(mPage.getDebugRegion(1621, 2274, 1821, 2328), "png", new File("d:/debug/"+System.nanoTime()+".png"));}catch(Exception e){e.printStackTrace();}
//	}
//}

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

//if (debug)
//{
//	mPage.mGraphics.setColor(new Color(255,255,0,128));
//	mPage.mGraphics.drawRect(x, y, w, h);
//}
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
							if (debug)
							{
								mPage.mDebugGraphics.setColor(new Color(0, 255, 0, 128));
								mPage.mDebugGraphics.draw(r);
							}

							rects.add(r);
						}
					}
					else
					{
						if (debug)
						{
							Log.out.println(w + " >= " + aMinWidth + " && " + w + " <= " + aMaxWidth + " && " + h + " >= " + aMinHeight + " && " + h + " <= " + aMaxHeight);

							mPage.mDebugGraphics.setColor(new Color(255, 0, 0, 128));
							mPage.mDebugGraphics.drawRect(x, y, w, h);
							mPage.mDebugGraphics.drawLine(x, y, x + w, y + h);
						}
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
			if (!top && mPage.getRaster(x + i, y))
			{
				top = true;
			}
			if (!bottom && mPage.getRaster(x + i, y + h))
			{
				bottom = true;
			}
		}
		for (int i = 0; i < h; i++)
		{
			if (!left && mPage.getRaster(x, y + i))
			{
				left = true;
			}
			if (!right && mPage.getRaster(x + w, y + i))
			{
				right = true;
			}
		}

		return (top ? SCAN_TOP : 0) + (right ? SCAN_RIGHT : 0) + (bottom ? SCAN_BOTTOM : 0) + (left ? SCAN_LEFT : 0);
	}

	private final static int SCAN_TOP = 1;
	private final static int SCAN_LEFT = 2;
	private final static int SCAN_BOTTOM = 4;
	private final static int SCAN_RIGHT = 8;
}
