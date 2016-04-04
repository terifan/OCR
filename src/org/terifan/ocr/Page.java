package org.terifan.ocr;

import java.awt.image.BufferedImage;


public class Page implements Cloneable
{
	private Bitmap mBitmap;
	

	public Page(Bitmap aBitmap)
	{
		mBitmap = aBitmap;
	}
 

	@Override
	public Page clone()
	{
		try
		{
			return (Page)super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			throw new InternalError();
		}
	}
	
	
	int getWidth()
	{
		return mBitmap.getWidth();
	}
	
	
	int getHeight()
	{
		return mBitmap.getHeight();
	}


	BufferedImage getRegion(int x0, int y0, int x1, int y1)
	{
		return mBitmap.getImage().getSubimage(x0, y0, x1-x0, y1-y0);
	}


	boolean isBlack(int aX, int aY)
	{
		return mBitmap.isBlack(aX, aY);
	}
}