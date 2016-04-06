package org.terifan.ocr;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;


public class Bitmap
{
	public final static byte BLACK = 0;
	public final static byte WHITE = (byte)255;
	public final static byte THRESHOLD = (byte)128;

	private BufferedImage mImage;
	private byte[] mRaster;
	private int mWidth;
	private int mHeight;


	public Bitmap(BufferedImage aBufferedImage)
	{
		if (aBufferedImage == null)
		{
			throw new IllegalArgumentException("Provided BufferedImage is null.");
		}

		mWidth = aBufferedImage.getWidth();
		mHeight = aBufferedImage.getHeight();

		mImage = new BufferedImage(mWidth, mHeight, BufferedImage.TYPE_BYTE_GRAY);
		mRaster = ((DataBufferByte)mImage.getRaster().getDataBuffer()).getData();

		Graphics2D g = mImage.createGraphics();
		g.drawImage(aBufferedImage, 0, 0, null);
		g.dispose();
	}


	public BufferedImage getImage()
	{
		return mImage;
	}


	public int getWidth()
	{
		return mWidth;
	}


	public int getHeight()
	{
		return mHeight;
	}


	public boolean isBlack(int x, int y)
	{
		return mRaster[mWidth * y + x] >= 0; // 0-127 black
	}


	public boolean isBlack(int x, int y, boolean aDefault)
	{
		if (x < 0 || x >= mWidth || y < 0 || y >= mHeight)
		{
			return aDefault;
		}

		return mRaster[mWidth * y + x] >= 0; // 0-127 black
	}


	public void adjustPageRotation()
	{
		adjustPageRotation(10, mHeight - 10);
	}


	public void adjustPageRotation(int aFromY, int aToY)
	{
		double angle = findAngle(aFromY, aToY);

		if (Math.abs(angle) > 0.0 && Math.abs(angle) < 90)
		{
			rotate(angle);
		}
	}


	public Bitmap rotate(double aAngleDegrees)
	{
		if (aAngleDegrees == 90 || aAngleDegrees == 180 || aAngleDegrees == 270)
		{
			rotateFixed(mImage, (int)aAngleDegrees);
		}
		else if (aAngleDegrees != 0)
		{
			mImage = ImageRotator.rotate(mImage, aAngleDegrees, 1, 0xFFFFFFFF);
		}

		return this;
	}


	double getRectFillFactor(int aOffsetX, int aOffsetY, int aWidth, int aHeight)
	{
		int c = 0;
		for (int y = 0; y < aHeight; y++)
		{
			for (int x = 0; x < aWidth; x++)
			{
				if (isBlack(aOffsetX + x, aOffsetY + y))
				{
					c++;
				}
			}
		}
		return c / (double)(aWidth * aHeight);
	}


	private BufferedImage rotateFixed(BufferedImage aBufferedImage, int aAngleDegrees)
	{
		int w = aBufferedImage.getWidth();
		int h = aBufferedImage.getHeight();
		int centerx = 0;
		int centery = 0;
		int steps = 0;

		switch (aAngleDegrees)
		{
			case 90:
				{
					int t = w;
					w = h;
					h = t;
					centerx = w / 2;
					centery = w / 2;
					steps = 1;
					break;
				}
			case 180:
				centerx = w / 2;
				centery = h / 2;
				steps = 2;
				break;
			case 270:
				{
					int t = w;
					w = h;
					h = t;
					centerx = h / 2;
					centery = h / 2;
					steps = 3;
					break;
				}
			default:
				throw new IllegalArgumentException("" + aAngleDegrees);
		}

		BufferedImage temp = new BufferedImage(w, h, aBufferedImage.getType());
		Graphics2D g = (Graphics2D)temp.getGraphics();
		g.setTransform(AffineTransform.getQuadrantRotateInstance(steps, centerx, centery));
		g.drawImage(aBufferedImage, 0, 0, null);
		g.dispose();

		return temp;
	}


	private double findAngle(int aFromY, int aToY)
	{
		double skewed = 0;
		int count = 0;

		for (int deviation = 1; deviation < 5 && count < 10000; deviation++)
		{
			for (int y = aFromY; y < aToY && count < 10000; y++)
			{
				for (int x = 10; x < mWidth - 10; x++)
				{
					if (isBlack(x,y) && (isBlack(x+1,y) || isBlack(x+2,y) || isBlack(x+3,y)))
					{
						Point pt = findHorLine(x, y, deviation, 5);

						if (pt.x - x > mWidth / 4)
						{
							if (getLineFillFactor(x, y, pt.x, pt.y, 0) > 0.95)
							{
								//mGraphics.setColor(Color.RED);
								//mGraphics.drawLine(x,y,pt.x,pt.y);
								skewed += (pt.y - y) / (double)(pt.x - x);
								count++;
							}
						}
					}
				}
			}
		}

		if (count == 0)
		{
			return 0;
		}

		skewed /= count;

		double error = Double.MAX_VALUE;
		double corr = 0;

		// TODO: improve
		for (double i = 0; i < 1; i += 0.0001)
		{
			double x = 1000 * Math.cos(Math.PI * 2 * i);
			double y = 1000 * Math.sin(Math.PI * 2 * i);
			double e = Math.abs((y / x) - skewed);
			if (e < error)
			{
				error = e;
				corr = (i * 360) % 90;
			}
		}

		if (corr > 45)
		{
			corr -= 90;
		}

		return -corr;
	}


	/**
	 * Find a horizontal line with gaps a max deviation of 5 and max error of 5.
	 */
	public Point findHorLine(int x, int y)
	{
		return findHorLine(x, y, 5, 5);
	}


	/**
	 * Find a horizontal line with gaps.
	 *
	 * @param x start x
	 * @param y start y
	 * @param aDeviation max deviation up and down from current Y coordinate to scan for a point
	 * @param aMaxErrors max missing points until a line is ended
	 * @return the line end point
	 */
	private Point findHorLine(int x, int y, int aDeviation, int aMaxErrors)
	{
		Point endPoint = new Point(x, y);

		for (int error = 0; error < aMaxErrors && x < mWidth; x++)
		{
			error++;

			for (int i = 1, n = 2 + 2 * aDeviation; i < n; i++)
			{
				int iy = y + (((i & 1) == 0) ? i / 2 : -i / 2);

				if (iy >= 0 && iy < mHeight && isBlack(x, iy))
				{
					endPoint.x = x;
					endPoint.y = iy;
					error = 0;

					// slowly adjust y in the direction of the change
					if (iy < y)
					{
						y--;
					}
					else if (iy > y)
					{
						y++;
					}

					break;
				}
			}
		}

		return endPoint;
	}


	/**
	 * Find a vertical line with gaps.
	 *
	 * @param x start x
	 * @param y start y
	 * @param aDeviation max deviation left and right from current X coordinate to scan for a point
	 * @param aMaxErrors max missing points until a line is ended
	 * @return the line end point
	 */
	private Point findVerLine(int x, int y, int aDeviation, int aMaxErrors)
	{
		Point endPoint = new Point(x, y);

		for (int error = 0, deviation = 0; error < aMaxErrors && Math.abs(deviation) <= aDeviation && y < mHeight; y++)
		{
			error++;

			for (int i = 1, n = 2 + 2 * aDeviation; i < n; i++)
			{
				int ix = x + (((i & 1) == 0) ? i / 2 : -i / 2);

				if (ix >= 0 && ix < mWidth && isBlack(ix,y))
				{
					endPoint.x = ix;
					endPoint.y = y;
					error = 0;

					// slowly adjust y in the direction of the change
					if (ix < x)
					{
						x--;
					}
					else if (ix > x)
					{
						x++;
					}

					break;
				}
			}
		}

		return endPoint;
	}


	private double getLineFillFactor(int x0, int y0, int x1, int y1, int aDeviation)
	{
		double w = Math.abs(x1 - x0);
		double h = Math.abs(y1 - y0);

		if (w >= h)
		{
			return getLineFillFactorHor(x1, x0, y0, y1, w, aDeviation);
		}
		else
		{
			return getLineFillFactorVer(y1, y0, x0, x1, h, aDeviation);
		}
	}


	private double getLineFillFactorVer(int y1, int y0, int x0, int x1, double h, int aDeviation)
	{
		if (y1 < y0)
		{
			int t = x0;
			x0 = x1;
			x1 = t;
			t = y0;
			y0 = y1;
			y1 = t;
		}

		double x = x0 + 0.5;
		double dx = (x1 - x0) / h;
		int sum = 0;

		for (int y = y0; y < y1; y++, x += dx)
		{
			if (y >= 0 && y < mHeight)
			{
				for (int d = 1, ds = 2 + 2 * aDeviation; d < ds; d++)
				{
					int ix = (int)x + (((d & 1) == 0) ? d / 2 : -d / 2);
					if (ix >= 0 && ix < mWidth && isBlack(ix,y))
					{
						sum++;
					}
				}
			}
		}

		return sum / h;
	}


	private double getLineFillFactorHor(int x1, int x0, int y0, int y1, double w, int aDeviation)
	{
		if (x1 < x0)
		{
			int t = x0;
			x0 = x1;
			x1 = t;
			t = y0;
			y0 = y1;
			y1 = t;
		}

		double y = y0 + 0.5;
		double dy = (y1 - y0) / w;
		int sum = 0;

		for (int x = x0; x < x1; x++, y += dy)
		{
			if (x >= 0 && x < mWidth)
			{
				for (int d = 1, ds = 2 + 2 * aDeviation; d < ds; d++)
				{
					int iy = (int)y + (((d & 1) == 0) ? d / 2 : -d / 2);
					if (iy >= 0 && iy < mHeight && isBlack(x,iy))
					{
						sum++;
						break;
					}
				}
			}
		}

		return sum / w;
	}


	/**
	 *
	 * @param aMinInches min length of line in inches
	 * @param aExtra extra pixels erased top, left, bottom and right
	 */
	public void eraseLines(double aMinInches, int aExtra)
	{
		ArrayList<Rectangle> list = new ArrayList<>();
		double min = aMinInches * Math.max(mWidth, mHeight) / 30.0 * 2.54;

		for (int y = 0; y < mHeight; y++)
		{
			for (int x = 0; x < mWidth - min; x++)
			{
				if (isBlack(x,y) && isBlack(x + 1, y))
				{
					Point pt = findHorLine(x, y, 2, 3);

					if (pt.x - x > min)
					{
						int x0 = x;
						int y0 = y;
						int x1 = pt.x;
						int y1 = pt.y;
						list.add(new Rectangle(x0, y0, x1 - x0, y1 - y0));

						x += (x1 - x0) / 2;
					}
				}
			}
		}

		for (int x = 0; x < mWidth; x++)
		{
			for (int y = 0; y < mHeight - min; y++)
			{
				if (isBlack(x,y) && isBlack(x,y+1))
				{
					Point pt = findVerLine(x, y, 2, 3);

					if (pt.y - y > min)
					{
						int x0 = x;
						int y0 = y;
						int x1 = pt.x;
						int y1 = pt.y;
						list.add(new Rectangle(x0, y0, x1 - x0, y1 - y0));

						y += (y1 - y0) / 2;
					}
				}
			}
		}

		Graphics2D g = mImage.createGraphics();
		g.setColor(Color.WHITE);

		for (Rectangle r : list)
		{
			for (int y = -aExtra; y <= aExtra; y++)
			{
				for (int x = -aExtra; x <= aExtra; x++)
				{
					g.drawLine(r.x + x, r.y + y, r.x + r.width + x, r.y + r.height + y);
				}
			}
		}

		g.dispose();
	}


	byte colorAt(int aX, int aY)
	{
		return mRaster[aY * mWidth + aX];
	}


	public Insets getBorders(int x, int y, int w, int h)
	{
		int x0 = x;
		int y0 = y;
		int x1 = x + w;
		int y1 = y + h;

		Insets borders = new Insets(0, 0, 0, 0);

		outer:
		for (y = y0; y < y1; y++)
		{
			for (x = x0; x < x1; x++)
			{
				if (isBlack(x,y))
				{
					borders.top = y - y0;
					break outer;
				}
			}
		}

		outer:
		for (y = y1 + 1; --y >= y0;)
		{
			for (x = x0; x < x1; x++)
			{
				if (isBlack(x,y))
				{
					borders.bottom = y1 - y;
					break outer;
				}
			}
		}

		outer:
		for (x = x0; x < x1; x++)
		{
			for (y = y0; y < y1; y++)
			{
				if (isBlack(x,y))
				{
					borders.left = x - x0;
					break outer;
				}
			}
		}

		outer:
		for (x = x1 + 1; --x >= x0;)
		{
			for (y = y0; y < y1; y++)
			{
				if (isBlack(x,y))
				{
					borders.right = x1 - x;
					break outer;
				}
			}
		}

		return borders;
	}


	public BufferedImage getRegion(int x0, int y0, int x1, int y1)
	{
		return mImage.getSubimage(x0, y0, x1-x0, y1-y0);
	}
}
