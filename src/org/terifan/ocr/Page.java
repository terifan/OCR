package org.terifan.ocr;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import javax.imageio.ImageIO;


public class Page implements Cloneable
{
	public enum RasterFilter
	{
		BLOCK,
		GAUSSIAN
	}

	public final static boolean BLACK = true;
	public final static boolean WHITE = false;

	private BufferedImage mImage;
	private boolean [][] mRaster;
	private int mWidth;
	private int mHeight;

	private BufferedImage mDebugImage;
	public Graphics2D mDebugGraphics;


	public Page(String aFile) throws IOException
	{
		this(new File(aFile));
	}


	public Page(File aFile) throws IOException
	{
		this(ImageIO.read(aFile));
	}


	public Page(byte[] aImageData) throws IOException
	{
		if (aImageData == null)
		{
			throw new IllegalArgumentException("Image data is null.");
		}
		init(ImageIO.read(new ByteArrayInputStream(aImageData)));
	}


	public Page(InputStream aInputStream) throws IOException
	{
		this(ImageIO.read(aInputStream));
	}


	public Page(BufferedImage aBufferedImage)
	{
		init(aBufferedImage);
	}


	private void init(BufferedImage aBufferedImage)
	{
		if (aBufferedImage == null)
		{
			throw new IllegalArgumentException("Provided BufferedImage is null.");
		}

		mWidth = aBufferedImage.getWidth();
		mHeight = aBufferedImage.getHeight();

		// ensure the image has correct color format
		mImage = new BufferedImage(mWidth, mHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = mImage.createGraphics();
		g.drawImage(aBufferedImage,0,0,null);
		g.dispose();

		updateRaster(RasterFilter.BLOCK);
	}


	private void init(boolean [][] aImage)
	{
		mWidth = aImage[0].length;
		mHeight = aImage.length;

		BufferedImage image = new BufferedImage(mWidth,mHeight,BufferedImage.TYPE_BYTE_BINARY);
		for (int y = 0; y < mHeight; y++)
		{
			for (int x = 0; x < mWidth; x++)
			{
				image.setRGB(x, y, aImage[y][x] ? 0x000000 : 0xFFFFFF);
			}
		}

		if (mDebugGraphics != null)
		{
			mDebugGraphics.drawImage(image, 0, 0, null);
		}

		updateRaster(RasterFilter.BLOCK);
	}


	public BufferedImage getImage()
	{
		return mImage;
	}


	public void setImage(BufferedImage aImage)
	{
		mImage = aImage;
		mWidth = mImage.getWidth();
		mHeight = mImage.getHeight();

		updateRaster(RasterFilter.BLOCK);
	}


	public BufferedImage getDebugImage()
	{
		return mDebugImage;
	}


	public boolean [][] getRaster()
	{
		return mRaster;
	}


	public boolean getRaster(int x, int y)
	{
		try
		{
			return mRaster[y][x];
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			return false;
		}
	}


	public void setRaster(boolean [][] aRaster)
	{
		mRaster = aRaster;
		mWidth = mRaster[0].length;
		mHeight = mRaster.length;
	}


//	public Page resize(int aWidth, int aHeight, boolean aKeepAspectRatio)
//	{
//		if (aKeepAspectRatio)
//		{
//			mImage = ImageTools.resizeAspect(mImage, aWidth, aHeight);
//		}
//		else
//		{
//			mImage = ImageTools.resize(mImage, aWidth, aHeight);
//		}
//
//		mWidth = mImage.getWidth();
//		mHeight = mImage.getHeight();
//
//		updateRaster(RasterFilter.BLOCK);
//
//		if (mDebugGraphics != null)
//		{
//			initDebug();
//		}
//
//		return this;
//	}


	public void adjustPageRotation()
	{
		adjustPageRotation(10, mHeight-10);
	}


	public void adjustPageRotation(int aFromY, int aToY)
	{
		double angle = getAngle(aFromY, aToY);

//		Log.out.println("Adjust page rotation by " + angle);

		if (Math.abs(angle) > 0.0 && Math.abs(angle) < 90)
		{
			rotate(angle);
		}
	}


	public void initDebug()
	{
		if (mDebugGraphics != null)
		{
			mDebugGraphics.dispose();
		}

		mDebugImage = new BufferedImage(mWidth, mHeight, BufferedImage.TYPE_INT_RGB);

		BufferedImage bw = new BufferedImage(mWidth, mHeight, BufferedImage.TYPE_BYTE_BINARY);
		Graphics g = bw.getGraphics();
		g.drawImage(mImage,0,0,null);
		g.dispose();

		mDebugGraphics = (Graphics2D)mDebugImage.getGraphics();
		mDebugGraphics.drawImage(bw,0,0,null);
	}


	/**
	 * Rotate the Page.
	 *
	 * @param aAngle
	 *   angle in degrees
	 * @return
	 *   this Page
	 */
	public Page rotate(double aAngle)
	{
		if (aAngle == 90)
		{
			return rotateFixed("right");
		}
		if (aAngle == 180)
		{
			return rotateFixed("full");
		}
		if (aAngle == 270)
		{
			return rotateFixed("left");
		}

		if (aAngle != 0)
		{
			mImage = ImageRotator.rotate(mImage, aAngle, 1, 0xFFFFFFFF);

			updateRaster(RasterFilter.BLOCK);

			if (mDebugGraphics != null)
			{
				mDebugGraphics.drawImage(mImage, 0, 0, null);
			}
		}

		return this;
	}


	/**
	 * Update the raster by downsampling every pixel in the image.
	 */
	public void updateRaster(RasterFilter aRasterFilter)
	{
		int [] source = ((DataBufferInt)mImage.getRaster().getDataBuffer()).getData();
		mWidth = mImage.getWidth();
		mHeight = mImage.getHeight();
		mRaster = new boolean[mHeight][mWidth];

		switch (aRasterFilter)
		{
			case BLOCK:
				for (int y = 0, i = 0; y < mHeight; y++)
				{
					for (int x = 0; x < mWidth; x++, i++)
					{
						int c = source[i];

						c = (255 & (c >> 16)) + (255 & (c >> 8)) + (255 & c);

						mRaster[y][x] = c < (3*128) ? BLACK : WHITE;
					}
				}
				break;
			case GAUSSIAN:
				for (int y = 0, i = 0; y < mHeight; y++)
				{
					for (int x = 0; x < mWidth; x++, i++)
					{
						int c00 = lum(source, x-1, y-1);
						int c01 = lum(source, x  , y-1);
						int c02 = lum(source, x+1, y-1);
						int c10 = lum(source, x-1, y  );
						int c11 = lum(source, x  , y  );
						int c12 = lum(source, x+1, y  );
						int c20 = lum(source, x-1, y+1);
						int c21 = lum(source, x  , y+1);
						int c22 = lum(source, x+1, y+1);

						int c = (16*c11+2*(c01+c21+c10+c12)+1*(c00+c02+c20+c22)); // / (9+15+4);

						mRaster[y][x] = c < 3584 ? BLACK : WHITE; // (9+15+4)*128
					}
				}
				break;
			default:
				throw new IllegalArgumentException();
		}
	}


	private int lum(int [] pixels, int x, int y)
	{
		int c = pixels[mWidth*y+x];
		return ((255 & (c >> 16)) + (255 & (c >> 8)) + (255 & c)) / 3;
	}


	/**
	 * Update the image by every pixel in the raster.
	 */
	public void updateImage()
	{
		int [] source = ((DataBufferInt)mImage.getRaster().getDataBuffer()).getData();

		for (int y = 0, i = 0; y < mHeight; y++)
		{
			for (int x = 0; x < mWidth; x++, i++)
			{
				source[i] = mRaster[y][x] == BLACK ? 0xFF000000 : 0xFFFFFFFF;
			}
		}
	}


	public void dispose()
	{
		mImage = null;
		mDebugImage = null;
		mRaster = null;

		if (mDebugGraphics != null)
		{
			mDebugGraphics.dispose();
			mDebugGraphics = null;
		}
	}


	public void filterNoise()
	{
		for (int y = 1; y < mHeight-3; y++)
		{
			for (int x = 1; x < mWidth-3; x++)
			{
				// clear black noise

				if (mRaster[y-1][x-1] == WHITE && mRaster[y-1][x] == WHITE && mRaster[y-1][x+1] == WHITE
				 && mRaster[y  ][x-1] == WHITE && mRaster[y  ][x] == BLACK && mRaster[y  ][x+1] == WHITE
				 && mRaster[y+1][x-1] == WHITE && mRaster[y+1][x] == WHITE && mRaster[y+1][x+1] == WHITE)
				{
					mRaster[y][x] = WHITE;
				}

				if ( mRaster[y-1][x-1] == BLACK &&  mRaster[y-1][x] == BLACK &&  mRaster[y-1][x+1] == BLACK
				 && !mRaster[y  ][x-1] == WHITE &&  mRaster[y  ][x] == BLACK && !mRaster[y  ][x+1] == WHITE
				 && !mRaster[y+1][x-1] == WHITE && !mRaster[y+1][x] == WHITE && !mRaster[y+1][x+1] == WHITE)
				{
					mRaster[y][x] = WHITE;
				}

				if (!mRaster[y-1][x-1] && !mRaster[y-1][x] && !mRaster[y-1][x+1]
				 && !mRaster[y  ][x-1] &&  mRaster[y  ][x] && !mRaster[y  ][x+1]
				 &&  mRaster[y+1][x-1] &&  mRaster[y+1][x] &&  mRaster[y+1][x+1])
				{
					mRaster[y][x] = WHITE;
				}

				if ( mRaster[y-1][x-1] && !mRaster[y-1][x] && !mRaster[y-1][x+1]
				 &&  mRaster[y  ][x-1] &&  mRaster[y  ][x] && !mRaster[y  ][x+1]
				 &&  mRaster[y+1][x-1] && !mRaster[y+1][x] && !mRaster[y+1][x+1])
				{
					mRaster[y][x] = WHITE;
				}

				if (!mRaster[y-1][x-1] && !mRaster[y-1][x] &&  mRaster[y-1][x+1]
				 && !mRaster[y  ][x-1] &&  mRaster[y  ][x] &&  mRaster[y  ][x+1]
				 && !mRaster[y+1][x-1] && !mRaster[y+1][x] &&  mRaster[y+1][x+1])
				{
					mRaster[y][x] = WHITE;
				}

				// fill white holes

				if ( mRaster[y-1][x-1] &&  mRaster[y-1][x] &&  mRaster[y-1][x+1]
				 &&  mRaster[y  ][x-1] && !mRaster[y  ][x] &&  mRaster[y  ][x+1]
				 &&  mRaster[y+1][x-1] &&  mRaster[y+1][x] &&  mRaster[y+1][x+1])
				{
					mRaster[y][x] = BLACK;
				}

				if ( mRaster[y-1][x-1] &&  mRaster[y-1][x] &&  mRaster[y-1][x+1]
				 &&  mRaster[y  ][x-1] && !mRaster[y  ][x] &&  mRaster[y  ][x+1]
				 && !mRaster[y+1][x-1] && !mRaster[y+1][x] && !mRaster[y+1][x+1])
				{
					mRaster[y][x] = BLACK;
				}

				if (!mRaster[y-1][x-1] && !mRaster[y-1][x] && !mRaster[y-1][x+1]
				 &&  mRaster[y  ][x-1] && !mRaster[y  ][x] &&  mRaster[y  ][x+1]
				 &&  mRaster[y+1][x-1] &&  mRaster[y+1][x] &&  mRaster[y+1][x+1])
				{
					mRaster[y][x] = BLACK;
				}

				if (!mRaster[y-1][x-1] &&  mRaster[y-1][x] &&  mRaster[y-1][x+1]
				 && !mRaster[y  ][x-1] && !mRaster[y  ][x] &&  mRaster[y  ][x+1]
				 && !mRaster[y+1][x-1] &&  mRaster[y+1][x] &&  mRaster[y+1][x+1])
				{
					mRaster[y][x] = BLACK;
				}

				if ( mRaster[y-1][x-1] &&  mRaster[y-1][x] && !mRaster[y-1][x+1]
				 &&  mRaster[y  ][x-1] && !mRaster[y  ][x] && !mRaster[y  ][x+1]
				 &&  mRaster[y+1][x-1] &&  mRaster[y+1][x] && !mRaster[y+1][x+1])
				{
					mRaster[y][x] = BLACK;
				}
			}
		}
	}


	public void filterRepair()
	{
		boolean [][] r = mRaster;

		for (int y = 1; y < mHeight-3; y++)
		{
			for (int x = 1; x < mWidth-3; x++)
			{
				// fill white holes

				if ( r[y-1][x-1] &&  r[y-1][x] &&  r[y-1][x+1]
				 &&  r[y  ][x-1] && !r[y  ][x] &&  r[y  ][x+1]
				 &&  r[y+1][x-1] &&  r[y+1][x] &&  r[y+1][x+1])
				{
					r[y][x] = BLACK;
				}

				if ( r[y-1][x-1] &&  r[y-1][x] &&  r[y-1][x+1]
				 &&  r[y  ][x-1] && !r[y  ][x] &&  r[y  ][x+1]
				 && !r[y+1][x-1] && !r[y+1][x] && !r[y+1][x+1])
				{
					r[y][x] = BLACK;
				}

				if (!r[y-1][x-1] && !r[y-1][x] && !r[y-1][x+1]
				 &&  r[y  ][x-1] && !r[y  ][x] &&  r[y  ][x+1]
				 &&  r[y+1][x-1] &&  r[y+1][x] &&  r[y+1][x+1])
				{
					r[y][x] = BLACK;
				}

				if (!r[y-1][x-1] &&  r[y-1][x] &&  r[y-1][x+1]
				 && !r[y  ][x-1] && !r[y  ][x] &&  r[y  ][x+1]
				 && !r[y+1][x-1] &&  r[y+1][x] &&  r[y+1][x+1])
				{
					r[y][x] = BLACK;
				}

				if ( r[y-1][x-1] &&  r[y-1][x] && !r[y-1][x+1]
				 &&  r[y  ][x-1] && !r[y  ][x] && !r[y  ][x+1]
				 &&  r[y+1][x-1] &&  r[y+1][x] && !r[y+1][x+1])
				{
					r[y][x] = BLACK;
				}
			}
		}
	}


	public void filterRepairAggressive(int aNeighbourCount)
	{
		boolean [][] raster = new boolean[mHeight][];
		for (int y = 0; y < mHeight; y++)
		{
			raster[y] = mRaster[y].clone();
		}

		for (int y = 1; y < mHeight-1; y++)
		{
			for (int x = 1; x < mWidth-1; x++)
			{
				if (raster[y][x] == WHITE)
				{
					int neigbours = (raster[y-1][x-1] == BLACK ? 1 : 0)
							      + (raster[y-1][x  ] == BLACK ? 1 : 0)
								  + (raster[y-1][x+1] == BLACK ? 1 : 0)
							      + (raster[y  ][x-1] == BLACK ? 1 : 0)
								  + (raster[y  ][x+1] == BLACK ? 1 : 0)
							      + (raster[y+1][x-1] == BLACK ? 1 : 0)
								  + (raster[y+1][x  ] == BLACK ? 1 : 0)
								  + (raster[y+1][x+1] == BLACK ? 1 : 0);

					if (neigbours >= aNeighbourCount)
					{
						mRaster[y][x] = BLACK;
					}
				}
			}
		}
	}


	public void filterMedian()
	{
		boolean [][] raster = new boolean[mHeight][];
		for (int y = 0; y < mHeight; y++)
		{
			raster[y] = mRaster[y].clone();
		}

		for (int y = 1; y < mHeight-1; y++)
		{
			for (int x = 1; x < mWidth-1; x++)
			{
				int c = (raster[y-1][x-1] ? 0 : 1)
				      + (raster[y-1][x  ] ? 0 : 1)
					  + (raster[y-1][x+1] ? 0 : 1)
					  + (raster[y  ][x-1] ? 0 : 1)
				      + (raster[y  ][x  ] ? 0 : 1)
					  + (raster[y  ][x+1] ? 0 : 1)
					  + (raster[y+1][x-1] ? 0 : 1)
				      + (raster[y+1][x  ] ? 0 : 1)
					  + (raster[y+1][x+1] ? 0 : 1);

				mRaster[y][x] = c < 5 ? BLACK : WHITE;
			}
		}
	}


	public void filterErode()
	{
		boolean [][] raster = new boolean[mHeight][];
		for (int y = 0; y < mHeight; y++)
		{
			raster[y] = mRaster[y].clone();
		}

		for (int y = 1; y < mHeight-1; y++)
		{
			for (int x = 1; x < mWidth-1; x++)
			{
				if (raster[y][x])
				{
					mRaster[y-1][x-1] = BLACK;
					mRaster[y-1][x  ] = BLACK;
					mRaster[y-1][x+1] = BLACK;
					mRaster[y  ][x-1] = BLACK;
					mRaster[y  ][x+1] = BLACK;
					mRaster[y+1][x-1] = BLACK;
					mRaster[y+1][x  ] = BLACK;
					mRaster[y+1][x+1] = BLACK;
				}
			}
		}
	}


	public void filterErodeVertical()
	{
		boolean [][] raster = new boolean[mHeight][];
		for (int y = 0; y < mHeight; y++)
		{
			raster[y] = mRaster[y].clone();
		}

		for (int y = 3; y < mHeight-3; y++)
		{
			for (int x = 0; x < mWidth; x++)
			{
				if (raster[y][x] == BLACK)
				{
					mRaster[y-1][x] = BLACK;
					mRaster[y-2][x] = BLACK;
					mRaster[y-3][x] = BLACK;

					mRaster[y+1][x] = BLACK;
					mRaster[y+2][x] = BLACK;
					mRaster[y+3][x] = BLACK;
				}
			}
		}
	}


	public void filterErodeHorizontal()
	{
		boolean [][] raster = new boolean[mHeight][];
		for (int y = 0; y < mHeight; y++)
		{
			raster[y] = mRaster[y].clone();
		}

		for (int y = 0; y < mHeight; y++)
		{
			for (int x = 3; x < mWidth-3; x++)
			{
				if (raster[y][x] == BLACK)
				{
					mRaster[y][x-1] = BLACK;
					mRaster[y][x-2] = BLACK;
					mRaster[y][x-3] = BLACK;

					mRaster[y][x+1] = BLACK;
					mRaster[y][x+2] = BLACK;
					mRaster[y][x+3] = BLACK;
				}
			}
		}
	}


	/**
	 * Removes dirt with a certain dimension
	 */
	public void filterSpeckle(int aDimension)
	{
		for (int y = aDimension; y < mHeight-aDimension; y++)
		{
			for (int x = aDimension; x < mWidth-aDimension; x++)
			{
				if (mRaster[y][x] == BLACK)
				{
					for (int d = 1; d < aDimension; d++)
					{
						boolean f = false;
						for (int iy = 1; iy <= 2*d && !f; iy++)
						{
							f |= mRaster[y-d+iy][x-d];
							f |= mRaster[y-d+iy][x+d];
							for (int ix = 1; ix <= 2*d && !f; ix++)
							{
								f |= mRaster[y-d][x-d+ix];
								f |= mRaster[y+d][x-d+ix];
							}
						}

						if (!f)
						{
							fillRect(x-d, y-d, 2*d, 2*d, WHITE);
//							for (int i = 0; i < dim; i++)
//							{
//								for (int j = 0; j < dim; j++)
//								{
//									mRaster[y+i][x+j] = WHITE;
//								}
//							}
						}
					}
				}
			}
		}
	}


	public void filterBlobs()
	{
		filterBlobs(50, 50, 0.8);
	}


	public void filterBlobs(int aSize, int aStep, double aFillRation)
	{
		if (aStep > aSize)
		{
			throw new IllegalArgumentException();
		}

		ArrayList<Point> list = new ArrayList<>();

		for (int y = 0; y < mHeight-aSize; y+=aStep)
		{
			for (int x = 0; x < mWidth-aSize; x+=aStep)
			{
				if (getRectFillFactor(x, y, aSize, aSize) > aFillRation)
				{
					list.add(new Point(x,y));
					x += aSize - aStep;
				}
			}
		}

		for (Point pt : list)
		{
			fillRect(pt.x, pt.y, aSize, aSize, WHITE);
		}
	}


	public void filterExtendShortSegments()
	{
		for (int y = 50; y < mHeight-50; y++)
		{
			for (int x = 50; x < mWidth-50; x++)
			{
				if (getRaster(x, y) && getRaster(x+1, y) && getRaster(x+2, y) && getRaster(x+3, y))
				{
					Point pt = findHorLine(x, y, 0, 30);

					if (getRectFillFactor(x, y-3, pt.x-x, 1) < 0.1 && getRectFillFactor(x, y+3, pt.x-x, 1) < 0.1)
					{
						fillRect(x, y, pt.x-x, 1, BLACK);
					}
				}
			}
		}

//		for (int sh = 1; sh < 6; sh++)
//		{
//			for (int sw = 5; sw < 15; sw++)
//			{
//				for (int y = sh; y < mHeight-2*sh; y++)
//				{
//					for (int x = 0; x < mWidth-3*sw; x++)
//					{
//						if (getRectFillFactor(x, y, sw, sh) > 0.1
//						&& getRectFillFactor(x+sw, y, sw, sh) < 0.1
//						&& getRectFillFactor(x+sw+sw, y, sw, sh) > 0.1
//						&& getRectFillFactor(x, y-sh, 3*sw, sh) < 0.1
//						&& getRectFillFactor(x, y+sh, 3*sw, sh) < 0.1)
//						{
//							fillRect(x, y, 3*sw, sh, BLACK);
//						}
//					}
//				}
//			}
//		}
	}


	public void fillRect(int aOffsetX, int aOffsetY, int aWidth, int aHeight, boolean aColor)
	{
		for (int by = 0; by < aHeight; by++)
		{
			for (int bx = 0; bx < aWidth; bx++)
			{
				mRaster[aOffsetY+by][aOffsetX+bx] = aColor;
			}
		}

		if (mDebugGraphics != null)
		{
			mDebugGraphics.setColor(aColor ? Color.BLACK : Color.WHITE);
			mDebugGraphics.fillRect(aOffsetX, aOffsetY, aWidth, aHeight);
		}
	}


	static class ImageIOException extends RuntimeException
	{
		private static final long serialVersionUID = 1L;
		public ImageIOException()
		{
		}
	}


	public double getRectFillFactor(Rectangle r)
	{
		return getRectFillFactor(r.x, r.y, r.width, r.height);
	}


	public double getRectFillFactor(int aOffsetX, int aOffsetY, int aWidth, int aHeight)
	{
		int c = 0;
		for (int y = 0; y < aHeight; y++)
		{
			for (int x = 0; x < aWidth; x++)
			{
				if (mRaster[aOffsetY+y][aOffsetX+x])
				{
					c++;
				}
			}
		}
		return c / (double)(aWidth * aHeight);
	}


	private Page rotateFixed(String aDirection)
	{
		int w = mWidth;
		int h = mHeight;
		int centerx = 0;
		int centery = 0;
		int steps = 0;

		if      (aDirection.equals("right")) {int t = w; w = h; h = t; centerx = w/2; centery = w/2; steps = 1;}
		else if (aDirection.equals("full"))  {                         centerx = w/2; centery = h/2; steps = 2;}
		else if (aDirection.equals("left"))  {int t = w; w = h; h = t; centerx = h/2; centery = h/2; steps = 3;}
		else throw new IllegalArgumentException(aDirection);

		BufferedImage temp = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D)temp.getGraphics();
		g.setTransform(AffineTransform.getQuadrantRotateInstance(steps, centerx, centery));
		g.drawImage(mImage, 0, 0, null);
		g.dispose();

		mImage = temp;

		updateRaster(RasterFilter.BLOCK);

		return this;
	}


	private double getAngle(int aFromY, int aToY)
	{
		double skewed = 0;
		int count = 0;

		for (int deviation = 1; deviation < 5 && count < 10000; deviation++)
		{
			for (int y = aFromY; y < aToY && count < 10000; y++)
			{
				for (int x = 10; x < mWidth - 10; x++)
				{
					if (mRaster[y][x] && (mRaster[y][x + 1] || mRaster[y][x + 2] || mRaster[y][x + 3]))
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
	 * @param x
	 *   start x
	 * @param y
	 *   start y
	 * @param aDeviation
	 *   max deviation up and down from current Y coordinate to scan for a point
	 * @param aMaxErrors
	 *   max missing points until a line is ended
	 * @return
	 *   the line end point
	 */
	public Point findHorLine(int x, int y, int aDeviation, int aMaxErrors)
	{
		Point endPoint = new Point(x, y);
		boolean [][] raster = mRaster;

		for (int error = 0; error < aMaxErrors && x < mWidth; x++)
		{
			error++;

			for (int i = 1, n = 2+2*aDeviation; i < n; i++)
			{
				int iy = y + (((i&1)==0) ? i/2 : -i/2);

				if (iy >= 0 && iy < mHeight && raster[iy][x])
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
	 * Find a vertical line with gaps with a max deviation of 5 and max error of 5.
	 */
	public Point findVerLine(int x, int y)
	{
		return findVerLine(x, y, 5, 5);
	}


	/**
	 * Find a vertical line with gaps.
	 *
	 * @param x
	 *   start x
	 * @param y
	 *   start y
	 * @param aDeviation
	 *   max deviation left and right from current X coordinate to scan for a point
	 * @param aMaxErrors
	 *   max missing points until a line is ended
	 * @return
	 *   the line end point
	 */
	public Point findVerLine(int x, int y, int aDeviation, int aMaxErrors)
	{
		Point endPoint = new Point(x, y);
		boolean [][] raster = mRaster;

		for (int error = 0, deviation = 0; error < aMaxErrors && Math.abs(deviation) <= aDeviation && y < mHeight; y++)
		{
			error++;

			for (int i = 1, n = 2+2*aDeviation; i < n; i++)
			{
				int ix = x + (((i&1)==0) ? i/2 : -i/2);

				if (ix >= 0 && ix < mWidth && raster[y][ix])
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


	public double getLineFillFactor(int x0, int y0, int x1, int y1, int aDeviation)
	{
		double w = Math.abs(x1-x0);
		double h = Math.abs(y1-y0);
		boolean [][] raster = mRaster;

		if (w >= h)
		{
			return getLineFillFactorHor(x1, x0, y0, y1, w, aDeviation, raster);
		}
		else
		{
			return getLineFillFactorVer(y1, y0, x0, x1, h, aDeviation, raster);
		}
	}


	private double getLineFillFactorVer(int y1, int y0, int x0, int x1, double h, int aDeviation, boolean[][] raster)
	{
		if (y1 < y0)
		{
			int t = x0; x0 = x1; x1 = t;
			t = y0; y0 = y1; y1 = t;
		}

		double x = x0+0.5;
		double dx = (x1-x0)/h;
		int sum = 0;

		for (int y = y0; y < y1; y++, x+=dx)
		{
			if (y >= 0 && y < mHeight)
			{
				for (int d = 1, ds = 2+2*aDeviation; d < ds; d++)
				{
					int ix = (int)x + (((d&1)==0) ? d/2 : -d/2);
					if (ix >= 0 && ix < mWidth && raster[y][ix])
					{
						sum++;
					}
				}
			}
		}

		return sum / h;
	}


	private double getLineFillFactorHor(int x1, int x0, int y0, int y1, double w, int aDeviation, boolean[][] raster)
	{
		if (x1 < x0)
		{
			int t = x0; x0 = x1; x1 = t;
			t = y0; y0 = y1; y1 = t;
		}

		double y = y0+0.5;
		double dy = (y1-y0)/w;
		int sum = 0;

		for (int x = x0; x < x1; x++, y+=dy)
		{
			if (x >= 0 && x < mWidth)
			{
				for (int d = 1, ds = 2+2*aDeviation; d < ds; d++)
				{
					int iy = (int)y + (((d&1)==0) ? d/2 : -d/2);
					if (iy >= 0 && iy < mHeight && raster[iy][x])
					{
						sum++;
						break;
					}
				}
			}
		}

		return sum / w;
	}


	public double getLineFillFactor(int x0, int y0, int x1, int y1)
	{
		double w = Math.abs(x1-x0);
		double h = Math.abs(y1-y0);
		boolean [][] raster = mRaster;

		if (w >= h)
		{
			return getLineFillFactorHor(x1, x0, y0, y1, w, raster);
		}
		else
		{
			return getLineFillFactorVer(y1, y0, x0, x1, h, raster);
		}
	}


	private double getLineFillFactorVer(int y1, int y0, int x0, int x1, double h, boolean[][] raster)
	{
		if (y1 < y0)
		{
			int t = x0; x0 = x1; x1 = t;
			t = y0; y0 = y1; y1 = t;
		}

		double x = x0;
		double dx = (x1-x0)/h;
		int sum = 0;

		for (int y = y0; y < y1; y++, x+=dx)
		{
			int ix = (int)x;
			if (ix >= 0 && ix < mWidth && raster[y][ix])
			{
				sum++;
			}
		}

		return sum / h;
	}


	private double getLineFillFactorHor(int x1, int x0, int y0, int y1, double w, boolean[][] raster)
	{
		if (x1 < x0)
		{
			int t = x0; x0 = x1; x1 = t;
			t = y0; y0 = y1; y1 = t;
		}

		double y = y0;
		double dy = (y1-y0)/w;
		int sum = 0;

		for (int x = x0; x < x1; x++, y+=dy)
		{
			int iy = (int)y;
			if (iy >= 0 && iy < mHeight && raster[iy][x])
			{
				sum++;
			}
		}

		return sum / w;
	}


	/**
	 * Writes the black & white clone to the file specified.
	 */
	public void writeRaster(String aFile)
	{
		try
		{
			ImageIO.write(getBinaryImage(), "png", new File(aFile));
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}


	public BufferedImage getBinaryImage()
	{
		BufferedImage image = new BufferedImage(mWidth,mHeight,BufferedImage.TYPE_BYTE_BINARY);
		for (int y = 0; y < mHeight; y++)
		{
			for (int x = 0; x < mWidth; x++)
			{
				image.setRGB(x, y, mRaster[y][x] ? 0x000000 : 0xFFFFFF);
			}
		}
		return image;
	}


	/**
	 * Writes the color clone to the file specified.
	 */
	public void write(String aFile)
	{
		write(new File(aFile));
	}


	/**
	 * Writes the color clone to the file specified.
	 */
	public void write(File aFile)
	{
		try
		{
			ImageIO.write(mImage, "png", aFile);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}


	/**
	 * Writes the color clone to the file specified.
	 */
	public void writeDebug(String aFile)
	{
		writeDebug(new File(aFile));
	}


	/**
	 * Writes the color clone to the file specified.
	 */
	public void writeDebug(File aFile)
	{
		try
		{
			ImageIO.write(getDebugImage(), "png", aFile);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}


    /**
     * Clones this Page
     * @return
     *   a new Page instance.
     */
    @Override
	public Page clone()
	{
		try
		{
			Page other = (Page)super.clone();
			other.init(mImage);
			return other;
		}
		catch (CloneNotSupportedException e)
		{
			throw new InternalError();
		}
	}


	public int getWidth()
	{
		return mWidth;
	}


	public int getHeight()
	{
		return mHeight;
	}


	public boolean isPortraitOrientation()
	{
		return mHeight > mWidth;
	}


	public boolean isLandscapeOrientation()
	{
		return !isPortraitOrientation();
	}


	public void drawPoint(int x, int y, boolean b)
	{
		mRaster[y][x] = b;

		if (mDebugGraphics != null)
		{
			mDebugGraphics.setColor(b ? Color.WHITE : Color.BLACK);
			mDebugGraphics.drawLine(x, y, x, y);
		}
	}


	public void drawLine(int x0, int y0, int x1, int y1, boolean b)
	{
		int w = x1-x0;
		int h = y1-y0;

		if (Math.abs(w) >= Math.abs(h))
		{
			drawLineHor(x1, x0, y0, y1, w, b);
		}
		else
		{
			drawLineVer(y1, y0, x0, x1, h, b);
		}
	}


	private void drawLineVer(int y1, int y0, int x0, int x1, int h, boolean b)
	{
		if (y1 < y0)
		{
			int t = x0; x0 = x1; x1 = t;
			t = y0; y0 = y1; y1 = t;
		}

		double x = x0+0.5;
		double dx = (x1-x0)/(double)h;

		for (int y = y0; y < y1; y++)
		{
			if (mRaster[y][(int)x])
			{
				drawPoint((int)x, y, b);
			}
			x += dx;
		}
	}


	private void drawLineHor(int x1, int x0, int y0, int y1, int w, boolean b)
	{
		if (x1 < x0)
		{
			int t = x0; x0 = x1; x1 = t;
			t = y0; y0 = y1; y1 = t;
		}

		double y = y0+0.5;
		double dy = (y1-y0)/(double)w;

		for (int x = x0; x < x1; x++)
		{
			if (mRaster[(int)y][x])
			{
				drawPoint(x, (int)y, b);
			}
			y += dy;
		}
	}


	public Point getLineEndPoint(int x0, int y0, int x1, int y1, int aMaxGap)
	{
		int w = x1 - x0;
		int h = y1 - y0;

		if (Math.abs(w) >= Math.abs(h))
		{
			return getLineEndPointHor(x1, x0, y0, y1, w, aMaxGap);
		}
		else
		{
			return getLineEndPointVer(y1, y0, x0, x1, h, aMaxGap);
		}
	}


	private Point getLineEndPointVer(int y1, int y0, int x0, int x1, int h, int aMaxGap)
	{
		if (y1 < y0)
		{
			int t = x0; x0 = x1; x1 = t;
		}

		double x = x0+0.5;
		double dx = (x1-x0)/(double)h;
		int gap = 0;

		Point p = new Point(x0, y0);

		for (int y = y0; x < mWidth && y < mHeight; y++)
		{
			if (mRaster[y][(int)x])
			{
				p.x = (int)x;
				p.y = y;
			}
			else
			{
				gap++;
				if (gap > aMaxGap)
				{
					break;
				}
			}
			x += dx;
		}

		return p;
	}


	private Point getLineEndPointHor(int x1, int x0, int y0, int y1, int w, int aMaxGap)
	{
		if (x1 < x0)
		{
			int t = y0; y0 = y1; y1 = t;
		}

		double y = y0+0.5;
		double dy = (y1-y0)/(double)w;
		int gap = 0;

		Point p = new Point(x0, y0);

		for (int x = x0; x < mWidth && y < mHeight; x++)
		{
			if (mRaster[(int)y][x])
			{
				p.x = x;
				p.y = (int)y;
			}
			else
			{
				gap++;
				if (gap > aMaxGap)
				{
					break;
				}
			}
			y += dy;
		}

		return p;
	}


	public BufferedImage getRegion(int x0, int y0, int x1, int y1)
	{
		BufferedImage image = new BufferedImage(x1-x0, y1-y0, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		g.drawImage(mImage, 0, 0, image.getWidth(), image.getHeight(), x0, y0, x1, y1, null);
		g.dispose();

		return image;
	}


	public BufferedImage getDebugRegion(int x0, int y0, int x1, int y1)
	{
		BufferedImage image = new BufferedImage(x1-x0, y1-y0, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		g.drawImage(mDebugImage, 0, 0, image.getWidth(), image.getHeight(), x0, y0, x1, y1, null);
		g.dispose();

		return image;
	}


	public boolean raster(int x, int y)
	{
		if (x < 0 || y < 0) return false;
		if (y >= mRaster.length) return false;
		if (x >= mRaster[y].length) return false;
		return mRaster[y][x];
	}


	public ArrayList<Rectangle> findLines(double aMinWidthInches, double aMinHeightInches)
	{
		ArrayList<Rectangle> lines = new ArrayList<>();
		int MAX_GAP = 3;

		for (int y = 0; y < mHeight; y++)
		{
			for (int x = 0; x < mWidth; x++)
			{
				if (mRaster[y][x])
				{
					Point pt = getLineEndPoint(x, y, mWidth, y, MAX_GAP);
					int w = pt.x-x;

					if (w > aMinWidthInches * getDPI())
					{
						lines.add(new Rectangle(x, y, w, 1));
						x += w;
					}
				}
			}
		}

		for (int x = 0; x < mWidth; x++)
		{
			for (int y = 0; y < mHeight; y++)
			{
				if (mRaster[y][x])
				{
					Point pt = getLineEndPoint(x, y, x, mHeight, MAX_GAP);
					int h = pt.y-y;

					if (h > aMinHeightInches * getDPI())
					{
						lines.add(new Rectangle(x, y, 1, h));
						y += h;
					}
				}
			}
		}

		return lines;
	}


	/**
	 *
	 * @param aMinInches
	 *   min length of line in inches
	 * @param aExtra
	 *   extra pixels erased top, left, bottom and right
	 */
	public void eraseLines(double aMinInches, int aExtra)
	{
		ArrayList<Rectangle> list = new ArrayList<>();
		double min = aMinInches * getDPI();

		for (int y = 0; y < mHeight; y++)
		{
			for (int x = 0; x < mWidth - min; x++)
			{
				if (mRaster[y][x] && mRaster[y][x + 1])
				{
					Point pt = findHorLine(x, y, 2, 3);

					if (pt.x-x > min)
					{
						int x0 = x;
						int y0 = y;
						int x1 = pt.x;
						int y1 = pt.y;
						list.add(new Rectangle(x0, y0, x1-x0, y1-y0));

						x += (x1-x0)/2;
					}
				}
			}
		}

		for (int x = 0; x < mWidth; x++)
		{
			for (int y = 0; y < mHeight - min; y++)
			{
				if (mRaster[y][x] && mRaster[y + 1][x])
				{
					Point pt = findVerLine(x, y, 2, 3);

					if (pt.y-y > min)
					{
						int x0 = x;
						int y0 = y;
						int x1 = pt.x;
						int y1 = pt.y;
						list.add(new Rectangle(x0, y0, x1-x0, y1-y0));

						y += (y1-y0)/2;
					}
				}
			}
		}

		for (Rectangle r : list)
		{
			for (int y = -aExtra; y <= aExtra; y++)
			{
				for (int x = -aExtra; x <= aExtra; x++)
				{
					try
					{
						drawLine(r.x + x, r.y + y, r.x + r.width + x, r.y + r.height + y, WHITE);

//						if (mDebugGraphics != null)
//						{
//							mDebugGraphics.setColor(Color.CYAN);
//							mDebugGraphics.drawLine(r.x + x, r.y + y, r.x + r.width + x, r.y + r.height + y);
//						}
					}
					catch (ArrayIndexOutOfBoundsException e)
					{
					}
				}
			}
		}
	}


	public void eraseLines(double aFromX, double aFromY, double aToX, double aToY, int aMinLineLengthPixels)
	{
		Page page = new Page(mImage);

		double segmentFillRatio = 1;

		for (int y = (int)(aFromY*mHeight); y < aToY*mHeight; y++)
		{
			for (int x = (int)(aFromX*mWidth); x < aToX*mWidth; x++)
			{
				if (page.mRaster[y][x])
				{
					for (int delta = -aMinLineLengthPixels; delta <= aMinLineLengthPixels; delta++)
					{
						if (getLineFillFactor(x, y, x+aMinLineLengthPixels, y+delta, 0) >= segmentFillRatio)
						{
							page.drawLine(x, y, x+aMinLineLengthPixels, y+delta, false);
						}
						if (getLineFillFactor(x, y, x-aMinLineLengthPixels, y+delta, 0) >= segmentFillRatio)
						{
							page.drawLine(x, y, x-aMinLineLengthPixels, y+delta, false);
						}
						if (getLineFillFactor(x, y, x+delta, y+aMinLineLengthPixels, 0) >= segmentFillRatio)
						{
							page.drawLine(x, y, x+delta, y+aMinLineLengthPixels, false);
						}
						if (getLineFillFactor(x, y, x+delta, y-aMinLineLengthPixels, 0) >= segmentFillRatio)
						{
							page.drawLine(x, y, x+delta, y-aMinLineLengthPixels, false);
						}
					}
				}
			}
		}

		init(page.getBinaryImage());
	}


	/**
	 * Get the page as a byte array.
	 *
	 * @param aImageFormat
	 *   file format, such as "png" or "jpeg".
	 * @return
	 *   the encoded image
	 */
	public byte [] getData(String aImageFormat) throws IOException
	{
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		ImageIO.write(mImage, aImageFormat, buffer);
		return buffer.toByteArray();
	}


	public double getDPI()
	{
		return Math.max(mWidth, mHeight) / 30.0 * 2.54;
	}


	/**
	 * Resize the image to the specified DPI. The page aspect ratio will be maintained.
	 */
//	public void resizeDPI(double aNewDPI)
//	{
//		double dpi = getDPI();
//
//		if (Math.abs(dpi - aNewDPI) > 5)
//		{
//			double f = aNewDPI / dpi;
//			int w = (int)Math.round(mWidth * f);
//			int h = (int)Math.round(mHeight * f);
//
//			resize(w, h, true);
//		}
//	}
//
//
//	public int t(int x, int y)
//	{
//		boolean [][] r = mRaster;
//		int t = 0;
//		if (!r[y-1][x  ] && r[y-1][x+1]) t++;
//		if (!r[y-1][x+1] && r[y  ][x-1]) t++;
//		if (!r[y  ][x+1] && r[y+1][x-1]) t++;
//		if (!r[y+1][x+1] && r[y-1][x-1]) t++;
//		if (!r[y+1][x  ] && r[y+1][x+1]) t++;
//		if (!r[y+1][x-1] && r[y+1][x  ]) t++;
//		if (!r[y  ][x-1] && r[y  ][x+1]) t++;
//		if (!r[y-1][x-1] && r[y-1][x  ]) t++;
//		return t;
//	}


//	public int n(int x, int y)
//	{
//		boolean [][] r = mRaster;
//		int n = 0;
//		if (r[y-1][x  ]) n++;
//		if (r[y-1][x+1]) n++;
//		if (r[y  ][x+1]) n++;
//		if (r[y+1][x+1]) n++;
//		if (r[y+1][x  ]) n++;
//		if (r[y+1][x-1]) n++;
//		if (r[y  ][x-1]) n++;
//		if (r[y-1][x-1]) n++;
//		return n;
//	}
//
//
//	public boolean f1(int x, int y)
//	{
//		boolean [][] r = mRaster;
//		return r[y][x] && r[y+1][x-1] && r[y+1][x] && !r[y+1][x+1] && !r[y-1][x-1] && !r[y-1][x] && !r[y-1][x+1];
//	}
//
//
//	public boolean f2(int x, int y)
//	{
//		boolean [][] r = mRaster;
//		return r[y][x] && r[y][x-1] && r[y+1][x] && !r[y-1][x] && !r[y-1][x+1] && !r[y][x+1];
//	}
//
//
//	public boolean f3(int x, int y)
//	{
//		return n(x,y) == 8;
//	}
//
//
//	public boolean matchKernel(boolean [][] aRaster, int x, int y, int k1, int k2, int k3, int k4, int k5, int k6, int k7, int k8)
//	{
//		if (k1 != -1 && aRaster[y-1][x  ] == (k1 == 1)) return false;
//		if (k2 != -1 && aRaster[y-1][x+1] == (k2 == 1)) return false;
//		if (k3 != -1 && aRaster[y  ][x+1] == (k3 == 1)) return false;
//		if (k4 != -1 && aRaster[y+1][x+1] == (k4 == 1)) return false;
//		if (k5 != -1 && aRaster[y+1][x  ] == (k5 == 1)) return false;
//		if (k6 != -1 && aRaster[y+1][x-1] == (k6 == 1)) return false;
//		if (k7 != -1 && aRaster[y  ][x-1] == (k7 == 1)) return false;
//		if (k8 != -1 && aRaster[y-1][x-1] == (k8 == 1)) return false;
//		return true;
//	}
//
//
//	public boolean matchKernel(boolean [][] aRaster, int x, int y, int [] aKernel)
//	{
//		if (aKernel[0] != -1 && aRaster[y-1][x  ] == (aKernel[0] == 1)) return false;
//		if (aKernel[1] != -1 && aRaster[y-1][x+1] == (aKernel[1] == 1)) return false;
//		if (aKernel[2] != -1 && aRaster[y  ][x+1] == (aKernel[2] == 1)) return false;
//		if (aKernel[3] != -1 && aRaster[y+1][x+1] == (aKernel[3] == 1)) return false;
//		if (aKernel[4] != -1 && aRaster[y+1][x  ] == (aKernel[4] == 1)) return false;
//		if (aKernel[5] != -1 && aRaster[y+1][x-1] == (aKernel[5] == 1)) return false;
//		if (aKernel[6] != -1 && aRaster[y  ][x-1] == (aKernel[6] == 1)) return false;
//		if (aKernel[7] != -1 && aRaster[y-1][x-1] == (aKernel[7] == 1)) return false;
//		return true;
//	}
//
//
//	public boolean [][] filterKernel(boolean [][] aRaster, int [] aKernel)
//	{
//		boolean [][] in = aRaster;
//		boolean [][] out = new boolean[aRaster.length][];
//		for (int y = 0; y < aRaster.length; y++)
//		{
//			out[y] = aRaster[y].clone();
//		}
//		for (int y = 1, h = in.length; y < h-1; y++)
//		{
//			for (int x = 1, w = in[0].length; x < w-1; x++)
//			{
//				if (in[y][x] == BLACK && matchKernel(in, x, y, aKernel))
//				{
//					out[y][x] = WHITE;
//				}
//			}
//		}
//		return out;
//	}
//
//
//	public void filterThinning()
//	{
////		{0,0,-1,1,1,1,-1,0},
////		{0,0,0,-1,1,-1,1,-1}
////		{0,0,0,-1,-1,0,0,0},
////		{0,0,0,0,-1,-1,0,0}
//
//		for (int iter = 0; iter < 10; iter++)
//		{
//			for (int dir = 0; dir < 4; dir++)
//			{
//				boolean [][] in = mRaster;
//				boolean [][] out = new boolean[in[0].length][in.length];
//
//				for (int y = 1, h = in.length; y < h-1; y++)
//				{
//					for (int x = 1, w = in[0].length; x < w-1; x++)
//					{
//						if (in[y][x])
//						{
//							out[w-1-x][y] = BLACK;
//							if (matchKernel(in, x, y, 0,0,-1,1,1,1,-1,0) || matchKernel(in, x, y, 0,0,0,-1,1,-1,1,-1))
//							{
//								out[w-1-x][y] = WHITE;
//							}
//						}
//					}
//				}
//
//				mRaster = out;
//			}
//		}
//
//		init(getBinaryImage());
//	}


	public void filterNoneLineSegments()
	{
		for (boolean changed = true; changed;)
		{
			changed = false;

			double thresholdH = 0.5;
			double thresholdV = 0.7;
			int scanRangeH = 64;
			int scanRangeV = 16;
			int scanH = 16;
			int scanV = 8;
			boolean [][] map = new boolean[mHeight][mWidth];

			for (int y = scanRangeV; y < mHeight-scanRangeV; y++)
			{
				for (int x = scanRangeH; x < mWidth-scanRangeH; x++)
				{
					if (mRaster[y][x] == BLACK)
					{
						for (int scan = -scanH/2; !map[y][x] && scan <= scanH/2; scan++)
						{
							if (getLineFillFactor(x, y, x-scanRangeH, y+scan, 0) > thresholdH) // left
							{
								map[y][x] = BLACK;
							}
							else if (getLineFillFactor(x, y, x+scanRangeH, y+scan, 0) > thresholdH) // right
							{
								map[y][x] = BLACK;
							}
						}
						for (int scan = -scanV/2; !map[y][x] && scan <= scanV/2; scan++)
						{
							if (getLineFillFactor(x, y, x+scan, y-scanRangeV, 0) > thresholdV) // up
							{
								map[y][x] = BLACK;
							}
							else if (getLineFillFactor(x, y, x+scan, y+scanRangeV, 0) > thresholdV) // down
							{
								map[y][x] = BLACK;
							}
						}
						if (!map[y][x])
						{
							changed = true;
						}
					}
				}
			}

			mRaster = map;
		}

		init(mRaster);
	}


	public Rectangle findBlobBounds(int aFromX, int aFromY, int aMaxWidth, int aMaxHeight)
	{
		int x = aFromX;
		int y = aFromY;
		int w = 3;
		int h = 3;

		while (h < aMaxHeight)
		{
			int s = scanBounds(x, y, w, h);

			if ((s & 1) != 0)
			{
				y--;
				h++;
			}
			if ((s & 2) != 0)
			{
				w++;
			}
			if ((s & 4) != 0)
			{
				h++;
			}
			if ((s & 8) != 0)
			{
				x--;
				w++;
			}
			if (s == 0)
			{
				break;
			}
		}

		while (h < aMaxHeight)
		{
			int s = scanBounds(x, y, w, h);

			if ((s & 1) == 0)
			{
				y++;
				h--;
			}
			if ((s & 2) == 0)
			{
				w--;
			}
			if ((s & 4) == 0)
			{
				h--;
			}
			if ((s & 8) == 0)
			{
				x++;
				w--;
			}
			if (s == 1+2+4+8)
			{
				break;
			}
		}

		return new Rectangle(x, y, w, h);
	}


	private int scanBounds(int x, int y, int w, int h)
	{
		int s = 0;
		boolean [][] raster = mRaster;

		for (int i = 0; s != 5 && i < w; i++)
		{
			if (raster[y][x+i]) s |= 1;
			if (raster[y+h-1][x+i]) s |= 4;
		}
		for (int i = 0; s != 10 && s != 15 && i < h; i++)
		{
			if (raster[y+i][x]) s |= 8;
			if (raster[y+i][x+w-1]) s |= 2;
		}

		return s;
	}


	/**
	 * Remove white borders from the Page.
	 *
	 * @param threshold
	 *   the amount of black allowed before considered stop cropping (0-1)
	 */
	public void cropBorders(double threshold)
	{
		int cropLeft = 0;
		int cropRight = 0;
		int cropTop = 0;
		int cropBottom = 0;

		// left
		for (int i = 0; i < mWidth; i++)
		{
			if (getLineFillFactor(i, 0, i, mHeight) > threshold)
			{
				cropLeft = i;
				break;
			}
		}
		// right
		for (int i = mWidth; --i >= 0;)
		{
			if (getLineFillFactor(i, 0, i, mHeight) > threshold)
			{
				cropRight = i;
				break;
			}
		}
		// top
		for (int i = 0; i < mHeight; i++)
		{
			if (getLineFillFactor(0, i, mWidth, i) > threshold)
			{
				cropTop = i;
				break;
			}
		}
		// bottom
		for (int i = mHeight; --i >= 0;)
		{
			if (getLineFillFactor(0, i, mWidth, i) > threshold)
			{
				cropBottom = i;
				break;
			}
		}

		init(getRegion(cropLeft, cropTop, cropRight+1, cropBottom+1));
	}


	public int[] getPixels(int [] aOutputBuffer)
	{
		boolean [][] r = mRaster;
		for (int y = 0, i = 0; y < mHeight; y++)
		{
			for (int x = 0; x < mWidth; x++, i++)
			{
				aOutputBuffer[i] = r[y][x] == BLACK ? 0xff000000 : 0xffffffff;
			}
		}
		return aOutputBuffer;
	}
}