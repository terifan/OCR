package org.terifan.ocr;


public class Page
{
	private Bitmap mBitmap;
	

	public Page(Bitmap aBitmap)
	{
		mBitmap = aBitmap;
	}


	Bitmap getBitmap()
	{
		return mBitmap;
	}
 	
	
	int getWidth()
	{
		return mBitmap.getWidth();
	}
	
	
	int getHeight()
	{
		return mBitmap.getHeight();
	}


	boolean isBlack(int aX, int aY)
	{
		return mBitmap.isBlack(aX, aY);
	}
}