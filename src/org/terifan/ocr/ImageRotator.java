package org.terifan.ocr;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Arrays;



public class ImageRotator
{
	public static BufferedImage rotate(BufferedImage aImage, double aAngle)
	{
		return rotate(aImage, aAngle, 1, 0xff000000);
	}


	public static BufferedImage rotate(BufferedImage aImage, double aAngle, int aQuality, int aBackgroundColor)
	{
		if (aAngle < 0 || aAngle >= 360)
		{
			throw new IllegalArgumentException("Angle not supported: " + aAngle);
		}

		int sw = aImage.getWidth();
		int sh = aImage.getHeight();

		if (aAngle == 90)
		{
			aImage = rotateFast(aImage, sh, sw, sh / 2, sh / 2, 1);
		}
		else if (aAngle == 180)
		{
			aImage = rotateFast(aImage, sw, sh, sw / 2, sh / 2, 2);
		}
		else if (aAngle == 270)
		{
			aImage = rotateFast(aImage, sh, sw, sw / 2, sw / 2, 3);
		}
		else if (aAngle != 0)
		{
			aImage = new ImageRotatorBuffer(aImage).rotate(aAngle, aQuality, aBackgroundColor).getImage();
		}

		return aImage;
	}


	private static BufferedImage rotateFast(BufferedImage aImage, int dw, int dh, int centerx, int centery, int steps)
	{
		BufferedImage dest = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = dest.createGraphics();
		g.setTransform(AffineTransform.getQuadrantRotateInstance(steps, centerx, centery));
		g.drawImage(aImage, 0, 0, null);
		g.dispose();

		return dest;
	}
}


class ImageRotatorBuffer
{
	private final static int RBLOCK = 64;

	private int [] mBuffer;
	private int mWidth;
	private int mHeight;


	public ImageRotatorBuffer(int aWidth, int aHeight )
	{
		mWidth = aWidth;
		mHeight = aHeight;
		mBuffer = new int[mWidth * mHeight];
	}


	public ImageRotatorBuffer(int aWidth, int aHeight, int[] aBuffer)
	{
		mWidth = aWidth;
		mHeight = aHeight;
		mBuffer = aBuffer;
	}


	public ImageRotatorBuffer(BufferedImage aImage)
	{
		mWidth = aImage.getWidth();
		mHeight = aImage.getHeight();
		mBuffer = new int[mWidth * mHeight];
		aImage.getRGB(0, 0, mWidth, mHeight, mBuffer, 0, mWidth);
	}


	public int getWidth()
	{
		return mWidth;
	}


	public int getHeight()
	{
		return mHeight;
	}


	public int[] getBuffer()
	{
		return mBuffer;
	}


	public BufferedImage getImage()
	{
		BufferedImage image = new BufferedImage(mWidth, mHeight, BufferedImage.TYPE_INT_RGB);
		image.setRGB(0, 0, mWidth, mHeight, mBuffer, 0, mWidth);
		return image;
	}


	private void rotateByShears(double aAngle, int aBackgroundColor)
	{
		while (aAngle >= 360) // Bring angle to range of (-INF .. 360)
		{
			aAngle -= 360;
		}
		while (aAngle < 0) // Bring angle to range of [0 .. 360)
		{
			aAngle += 360;
		}

		if ((aAngle > 45) && (aAngle <= 135))
		{
			// Angle in (45 .. 135]
			// Rotate image by 90 degrees into temporary image,
			// so it requires only an extra rotation angle
			// of -45 .. +45 to complete rotation.
			rotate90();
			aAngle -= 90;
		}
		else if ((aAngle > 135) && (aAngle <= 225))
		{
			// Angle in (135 .. 225]
			// Rotate image by 180 degrees into temporary image,
			// so it requires only an extra rotation angle
			// of -45 .. +45 to complete rotation.
			rotate180();
			aAngle -= 180;
		}
		else if ((aAngle > 225) && (aAngle <= 315))
		{
			// Angle in (225 .. 315]
			// Rotate image by 270 degrees into temporary image,
			// so it requires only an extra rotation angle
			// of -45 .. +45 to complete rotation.
			rotate270();
			aAngle -= 270;
		}

		// If we got here, angle is in (-45 .. +45]

		if (aAngle != 0)
		{
			// Perform last rotation
			rotate45(aAngle, aBackgroundColor);
		}
	}


	private void rotate90()
	{
		int srcWidth  = mWidth;
		int srcHeight = mHeight;
		int dstWidth  = srcHeight;
		int dstHeight = srcWidth;
		int srcPitch = srcWidth;
		int dstPitch = dstWidth;

		int [] bsrc = mBuffer;
		int [] bdest = new int[dstWidth * dstHeight];

		for (int xs = 0; xs < dstWidth; xs += RBLOCK)
		{
			for (int ys = 0; ys < dstHeight; ys += RBLOCK)
			{
				for (int y = ys; y < Math.min(dstHeight, ys + RBLOCK); y++)
				{
					int y2 = dstHeight - y - 1;
					int srcBits = (xs * srcPitch) + (y2 * 1);
					int dstBits = (y * dstPitch) + (xs * 1);
					for  (int x = xs; x < Math.min(dstWidth, xs + RBLOCK); x++)
					{
						bdest[dstBits] = bsrc[srcBits];

						dstBits += 1;
						srcBits += srcPitch;
					}
				}
			}
		}

		mWidth = dstWidth;
		mHeight = dstHeight;
		mBuffer = bdest;
	}


	private void rotate180()
	{
		int srcWidth  = mWidth;
		int srcHeight = mHeight;
		int dstWidth  = srcWidth;
		int dstHeight = srcHeight;

		int [] src = mBuffer;
		int [] dst = new int[dstWidth * dstHeight];

		for (int y = 0; y < srcHeight; y++)
		{
			int srcBits = srcWidth * y;
			int dstBits = dstWidth * (dstHeight - y - 1) + (dstWidth - 1);
			for (int x = 0; x < srcWidth; x++)
			{
				dst[dstBits] = src[srcBits];
				srcBits += 1;
				dstBits -= 1;
			}
		}

		mWidth = dstWidth;
		mHeight = dstHeight;
		mBuffer = dst;
	}


	private void rotate270()
	{
		int srcWidth  = mWidth;
		int srcHeight = mHeight;
		int dstWidth  = srcHeight;
		int dstHeight = srcWidth;
		int srcPitch = srcWidth;
		int dstPitch = dstWidth;

		int [] bsrc = mBuffer;
		int [] bdest = new int[dstWidth * dstHeight];

		for (int xs = 0; xs < dstWidth; xs += RBLOCK)
		{
			for (int ys = 0; ys < dstHeight; ys += RBLOCK)
			{
				for (int x = xs; x < Math.min(dstWidth, xs + RBLOCK); x++)
				{
					int x2 = dstWidth - x - 1;
					int srcBits = (x2 * srcPitch) + ys;
					int dstBits = (ys * dstPitch) + x;

					for (int y = ys; y < Math.min(dstHeight, ys + RBLOCK); y++)
					{
						bdest[dstBits] = bsrc[srcBits];
						srcBits += 1;
						dstBits += dstPitch;
					}
				}
			}
		}

		mWidth = dstWidth;
		mHeight = dstHeight;
		mBuffer = bdest;
	}


	private void rotate45(double dAngle, int aBackgroundColor)
	{
		int u;

		double dRadAngle = Math.toRadians(dAngle);
		double dSinE = Math.sin(dRadAngle);
		double dTan = Math.tan(dRadAngle / 2);

		int srcWidth  = mWidth;
		int srcHeight = mHeight;

		// Calc first shear (horizontal) destination image dimensions
		int width_1  = srcWidth + (int)((double)srcHeight * Math.abs(dTan) + 0.5);
		int height_1 = srcHeight;

		//******* Perform 1st shear (horizontal) ******

		// Allocate image for 1st shear
		ImageRotatorBuffer dst1 = new ImageRotatorBuffer(width_1, height_1);

		for(u = 0; u < height_1; u++)
		{
			double dShear;

			if(dTan >= 0)
			{
				// Positive angle
				dShear = ((double)u + 0.5) * dTan;
			}
			else
			{
				// Negative angle
				dShear = ((double)((int)u - height_1) + 0.5) * dTan;
			}
			int iShear = (int)dShear;
			horizontalSkew(this, dst1, u, iShear, (int)(255 * (dShear - (double)iShear) + 1), aBackgroundColor);
		}

		//******* Perform 2nd shear  (vertical) ******

		// Calc 2nd shear (vertical) destination image dimensions
		int width_2  = width_1;
		int height_2 = (int)((double)srcWidth * Math.abs(dSinE) + (double)srcHeight * Math.cos(dRadAngle) + 0.5) + 1;

		// Allocate image for 2nd shear
		ImageRotatorBuffer dst2 = new ImageRotatorBuffer(width_2,  height_2);

		double dOffset;     // Variable skew offset
		if(dSinE > 0)
		{
			// Positive angle
			dOffset = ((double)srcWidth - 1) * dSinE;
		}
		else
		{
			// Negative angle
			dOffset = -dSinE * (double)(srcWidth - width_2);
		}

		for(u = 0; u < width_2; u++, dOffset -= dSinE)
		{
			int iShear = (int)dOffset;
			verticalSkew(dst1, dst2, u, iShear, (int)(255 * (dOffset - (double)iShear) + 1), aBackgroundColor);
		}

		//******* Perform 3rd shear (horizontal) ******

		// Calc 3rd shear (horizontal) destination image dimensions
		int width_3  = (int)((double)srcHeight * Math.abs(dSinE) + (double)srcWidth * Math.cos(dRadAngle) + 0.5) + 1;
		int height_3 = height_2;

		// Allocate image for 3rd shear
		ImageRotatorBuffer dst3 = new ImageRotatorBuffer(width_3, height_3);

		if(dSinE >= 0)
		{
			// Positive angle
			dOffset = (double)(srcWidth - 1) * dSinE * -dTan;
		}
		else
		{
			// Negative angle
			dOffset = dTan * ((double)(srcWidth - 1) * -dSinE + (double)(1 - height_3));
		}

		for(u = 0; u < height_3; u++, dOffset += dTan)
		{
			int iShear = (int)dOffset;
			horizontalSkew(dst2, dst3, u, iShear, (int)(255 * (dOffset - (double)iShear) + 1), aBackgroundColor);
		}


		mWidth = dst3.mWidth;
		mHeight = dst3.mHeight;
		mBuffer = dst3.mBuffer;
	}


	private static void horizontalSkew(ImageRotatorBuffer aSrcBuffer, ImageRotatorBuffer aDstBuffer, int row, int iOffset, int Weight, int aBackgroundColor)
	{
		int i, j;
		int iXPos;

		int srcWidth  = aSrcBuffer.getWidth();
		int srcHeight = aSrcBuffer.getHeight();
		int dstWidth  = aDstBuffer.getWidth();
		int dstHeight = aDstBuffer.getHeight();

		int [] pxlSrc = new int[4];
		int [] pxlLeft = new int[4];
		int [] pxlOldLeft = new int[4];

		int bytespp = 4;

		int [] src = aSrcBuffer.getBuffer();
		int [] dst = aDstBuffer.getBuffer();

		int srcBits = srcWidth * row;
		int dstBits = dstWidth * row;

		// fill gap left of skew with background
		if(iOffset > 0)
		{
			memset(dst, dstBits, iOffset, aBackgroundColor);
		}
		pxlOldLeft[0] = 255&(aBackgroundColor>>>24);
		pxlOldLeft[1] = 255&(aBackgroundColor>>16);
		pxlOldLeft[2] = 255&(aBackgroundColor>>8);
		pxlOldLeft[3] = 255&(aBackgroundColor);
		for(j = 0; j < bytespp; j++)
		{
			pxlOldLeft[j] = Math.max(Math.min((pxlOldLeft[j] * Weight) / 256,255),0);
		}

		for(i = 0; i < srcWidth; i++)
		{
			// loop through row pixels
			pxlSrc[0] = 255&(src[srcBits]>>>24);
			pxlSrc[1] = 255&(src[srcBits]>>16);
			pxlSrc[2] = 255&(src[srcBits]>>8);
			pxlSrc[3] = 255&(src[srcBits]);

			// calculate weights
			for(j = 0; j < bytespp; j++)
			{
				pxlLeft[j] = Math.max(Math.min((pxlSrc[j] * Weight) / 256,255),0);
			}
			// check boundaries
			iXPos = i + iOffset;
			if((iXPos >= 0) && (iXPos < dstWidth))
			{
				// update left over on source
				for(j = 0; j < bytespp; j++)
				{
					pxlSrc[j] = pxlSrc[j] - (pxlLeft[j] - pxlOldLeft[j]);
				}
				dst[dstBits+iXPos] = (pxlSrc[0]<<24)|(pxlSrc[1]<<16)+(pxlSrc[2]<<8)+pxlSrc[3];
			}
			// save leftover for next pixel in scan
			System.arraycopy(pxlLeft, 0, pxlOldLeft, 0, 4);

			// next pixel in scan
			srcBits += 1;
		}

		// go to rightmost point of skew
		iXPos = srcWidth + iOffset;

		if(iXPos < dstWidth)
		{
			dstBits = dstWidth*row + iXPos;

			pxlSrc[0] = 255&(aBackgroundColor>>>24);
			pxlSrc[1] = 255&(aBackgroundColor>>16);
			pxlSrc[2] = 255&(aBackgroundColor>>8);
			pxlSrc[3] = 255&(aBackgroundColor);

			for(j = 0; j < bytespp; j++)
			{
				pxlLeft[j] = Math.max(Math.min((pxlSrc[j] * Weight) / 256,255),0);
			}

			for(j = 0; j < bytespp; j++)
			{
				pxlSrc[j] = pxlSrc[j] - (pxlLeft[j] - pxlOldLeft[j]);
			}

			// If still in image bounds, put leftovers there
			dst[dstBits] = (pxlSrc[0]<<24)|(pxlSrc[1]<<16)+(pxlSrc[2]<<8)+pxlSrc[3];

			// clear to the right of the skewed line with background
			dstBits += 1;
			memset(dst, dstBits, (dstWidth - iXPos - 1), aBackgroundColor);
		}
	}


	private static void verticalSkew(ImageRotatorBuffer aSrcBuffer, ImageRotatorBuffer aDstBuffer, int col, int iOffset, int Weight, int aBackgroundColor)
	{
		int i, j, iYPos;

		int srcWidth  = aSrcBuffer.getWidth();
		int srcHeight = aSrcBuffer.getHeight();
		int dstWidth  = aDstBuffer.getWidth();
		int dstHeight = aDstBuffer.getHeight();

		int [] src = aSrcBuffer.getBuffer();
		int [] dst = aDstBuffer.getBuffer();

		int [] pxlSrc = new int[4];
		int [] pxlLeft = new int[4];
		int [] pxlOldLeft = new int[4];

		int bytespp = 4;

		int src_pitch = srcWidth;
		int dst_pitch = dstWidth;
		int index = col;

		int srcBits = index;
		int dstBits = index;

		// fill gap above skew with background
		if(iOffset > 0)
		{
			for(i = 0; i < iOffset; i++)
			{
				dst[dstBits] = aBackgroundColor;
				dstBits += dst_pitch;
			}
		}
		pxlOldLeft[0] = 255&(aBackgroundColor>>>24);
		pxlOldLeft[1] = 255&(aBackgroundColor>>16);
		pxlOldLeft[2] = 255&(aBackgroundColor>>8);
		pxlOldLeft[3] = 255&(aBackgroundColor);
		for(j = 0; j < bytespp; j++)
		{
			pxlOldLeft[j] = Math.max(Math.min((pxlOldLeft[j] * Weight) / 256,255),0);
		}

		for(i = 0; i < srcHeight; i++)
		{
			// loop through column pixels
			pxlSrc[0] = 255&(src[srcBits]>>>24);
			pxlSrc[1] = 255&(src[srcBits]>>16);
			pxlSrc[2] = 255&(src[srcBits]>>8);
			pxlSrc[3] = 255&(src[srcBits]);

			// calculate weights
			for(j = 0; j < bytespp; j++)
			{
				pxlLeft[j] = Math.max(Math.min((pxlSrc[j] * Weight) / 256, 255),0);
			}
			// check boundaries
			iYPos = i + iOffset;
			if((iYPos >= 0) && (iYPos < dstHeight))
			{
				// update left over on source
				for(j = 0; j < bytespp; j++)
				{
					pxlSrc[j] = pxlSrc[j] - (pxlLeft[j] - pxlOldLeft[j]);
				}
				dstBits = dstWidth*iYPos + index;
				dst[dstBits] = (pxlSrc[0]<<24)|(pxlSrc[1]<<16)+(pxlSrc[2]<<8)+pxlSrc[3];
			}
			// save leftover for next pixel in scan
			System.arraycopy(pxlLeft, 0, pxlOldLeft, 0, 4);

			// next pixel in scan
			srcBits += src_pitch;
		}
		// go to bottom point of skew
		iYPos = srcHeight + iOffset;

		if(iYPos < dstHeight)
		{
			dstBits = dstWidth * iYPos + index;

			pxlSrc[0] = 255&(aBackgroundColor>>>24);
			pxlSrc[1] = 255&(aBackgroundColor>>16);
			pxlSrc[2] = 255&(aBackgroundColor>>8);
			pxlSrc[3] = 255&(aBackgroundColor);

			for(j = 0; j < bytespp; j++)
			{
				pxlLeft[j] = Math.max(Math.min((pxlSrc[j] * Weight) / 256,255),0);
			}

			for(j = 0; j < bytespp; j++)
			{
				pxlSrc[j] = pxlSrc[j] - (pxlLeft[j] - pxlOldLeft[j]);
			}

			// if still in image bounds, put leftovers there
			dst[dstBits] = (pxlSrc[0]<<24)|(pxlSrc[1]<<16)+(pxlSrc[2]<<8)+pxlSrc[3];

			// clear below skewed line with background
			while(++iYPos < dstHeight)
			{
				dstBits += dst_pitch;
				dst[dstBits] = aBackgroundColor;
			}
		}
	}


	private static void memset(int [] dstBits, int aOffset, int aLength, int aValue)
	{
		while (aLength-- > 0)
		{
			dstBits[aOffset++] = aValue;
		}
	}


	private void convertToInterpolationCoefficients(double [] c, int DataLength, double [] z, int NbPoles, double Tolerance)
	{
		double	Lambda = 1;

		// special case required by mirror boundaries
		if(DataLength == 1)
		{
			return;
		}
		// compute the overall gain
		for(int k = 0; k < NbPoles; k++)
		{
			Lambda = Lambda * (1.0 - z[k]) * (1.0 - 1.0 / z[k]);
		}
		// apply the gain
		for (int n = 0; n < DataLength; n++)
		{
			c[n] *= Lambda;
		}
		// loop over all poles
		for (int k = 0; k < NbPoles; k++)
		{
			// causal initialization
			c[0] = InitialCausalCoefficient(c, DataLength, z, k, Tolerance);
			// causal recursion
			for (int n = 1; n < DataLength; n++)
			{
				c[n] += z[k] * c[n - 1];
			}
			// anticausal initialization
			c[DataLength - 1] = InitialAntiCausalCoefficient(c, DataLength, z[k]);
			// anticausal recursion
			for (int n = DataLength - 2; 0 <= n; n--)
			{
				c[n] = z[k] * (c[n + 1] - c[n]);
			}
		}
	}

	private double InitialCausalCoefficient(double [] c, int DataLength, double [] z, int zoff, double Tolerance)
	{
		// this initialization corresponds to mirror boundaries
		int Horizon = DataLength;
		if(Tolerance > 0)
		{
			Horizon = (int)Math.ceil(Math.log(Tolerance) / Math.log(Math.abs(z[zoff])));
		}
		if(Horizon < DataLength)
		{
			// accelerated loop
			double zn = z[zoff];
			double Sum = c[0];
			for (int n = 1; n < Horizon; n++)
			{
				Sum += zn * c[n];
				zn *= z[zoff];
			}
			return(Sum);
		}
		else {
			// full loop
			double zn = z[zoff];
			double iz = 1.0 / z[zoff];
			double z2n = Math.pow(z[zoff], (double)(DataLength - 1));
			double Sum = c[0] + z2n * c[DataLength - 1];
			z2n *= z2n * iz;
			for (int n = 1; n <= DataLength - 2; n++)
			{
				Sum += (zn + z2n) * c[n];
				zn *= z[zoff];
				z2n *= iz;
			}
			return(Sum / (1.0 - zn * zn));
		}
	}


	private void GetColumn(double [] Image, int offset, int Width, int x, double [] Line, int Height)
	{
		offset = offset + x;
		for(int y = 0; y < Height; y++)
		{
			Line[y] = (double)Image[offset];
			offset += Width;
		}
	}


	private void GetRow(double [] Image, int offset, int y, double [] Line, int Width)
	{
		offset = offset + (y * Width);
		for(int x = 0; x < Width; x++)
		{
			Line[x] = (double)Image[offset++];
		}
	}


	private double InitialAntiCausalCoefficient(double [] c, int DataLength, double	z)
	{
		// this initialization corresponds to mirror boundaries
		return((z / (z * z - 1.0)) * (z * c[DataLength - 2] + c[DataLength - 1]));
	}


	private void PutColumn(double [] Image, int offset, int Width, int x, double [] Line, int Height)
	{
		offset = offset + x;
		for(int y = 0; y < Height; y++)
		{
			Image[offset] = (double)Line[y];
			offset += Width;
		}
	}


	private void PutRow(double [] Image, int offset, int y, double [] Line, int Width)
	{
		offset = offset + (y * Width);
		for(int x = 0; x < Width; x++)
		{
			Image[offset++] = (double)Line[x];
		}
	}


	private void SamplesToCoefficients(double [] Image, int Width, int Height, int spline_degree)
	{
		double	[] Line;
		double	[] Pole = new double[2];
		int	NbPoles;

		// recover the poles from a lookup table
		switch (spline_degree)
		{
			case 2:
				NbPoles = 1;
				Pole[0] = Math.sqrt(8.0) - 3.0;
				break;
			case 3:
				NbPoles = 1;
				Pole[0] = Math.sqrt(3.0) - 2.0;
				break;
			case 4:
				NbPoles = 2;
				Pole[0] = Math.sqrt(664.0 - Math.sqrt(438976.0)) + Math.sqrt(304.0) - 19.0;
				Pole[1] = Math.sqrt(664.0 + Math.sqrt(438976.0)) - Math.sqrt(304.0) - 19.0;
				break;
			case 5:
				NbPoles = 2;
				Pole[0] = Math.sqrt(135.0 / 2.0 - Math.sqrt(17745.0 / 4.0)) + Math.sqrt(105.0 / 4.0) - 13.0 / 2.0;
				Pole[1] = Math.sqrt(135.0 / 2.0 + Math.sqrt(17745.0 / 4.0)) - Math.sqrt(105.0 / 4.0) - 13.0 / 2.0;
				break;
			default:
				throw new RuntimeException("Invalid spline degree");
		}

		// convert the image samples into interpolation coefficients

		// in-place separable process, aint x
		Line = new double[Width];
		for (int y = 0; y < Height; y++)
		{
			GetRow(Image, 0, y, Line, Width);
			convertToInterpolationCoefficients(Line, Width, Pole, NbPoles, 1E-9);
			PutRow(Image, 0, y, Line, Width);
		}

		// in-place separable process, aint y
		Line = new double[Height];
		for (int x = 0; x < Width; x++)
		{
			GetColumn(Image, 0, Width, x, Line, Height);
			convertToInterpolationCoefficients(Line, Height, Pole, NbPoles, 1E-9);
			PutColumn(Image, 0, Width, x, Line, Height);
		}
	}

	private double InterpolatedValue(double [] Bcoeff, int Width, int Height, double x, double y, int spline_degree)
	{
		double	[] xWeight = new double[6];
		double  [] yWeight = new double[6];
		double	interpolated;
		double	w, w2, w4, t, t0, t1;
		int	[] xIndex = new int[6];
		int    [] yIndex = new int[6];
		int	Width2 = 2 * Width - 2, Height2 = 2 * Height - 2;

		// compute the interpolation indexes
		if ((spline_degree & 1) != 0)
		{
			int i = (int)Math.floor(x) - spline_degree / 2;
			int j = (int)Math.floor(y) - spline_degree / 2;
			for(int k = 0; k <= spline_degree; k++)
			{
				xIndex[k] = i++;
				yIndex[k] = j++;
			}
		}
		else
		{
			int i = (int)Math.floor(x + 0.5) - spline_degree / 2;
			int j = (int)Math.floor(y + 0.5) - spline_degree / 2;
			for (int k = 0; k <= spline_degree; k++)
			{
				xIndex[k] = i++;
				yIndex[k] = j++;
			}
		}

		// compute the interpolation weights
		switch (spline_degree)
		{
			case 2:
				/* x */
				w = x - (double)xIndex[1];
				xWeight[1] = 3.0 / 4.0 - w * w;
				xWeight[2] = (1.0 / 2.0) * (w - xWeight[1] + 1.0);
				xWeight[0] = 1.0 - xWeight[1] - xWeight[2];
				/* y */
				w = y - (double)yIndex[1];
				yWeight[1] = 3.0 / 4.0 - w * w;
				yWeight[2] = (1.0 / 2.0) * (w - yWeight[1] + 1.0);
				yWeight[0] = 1.0 - yWeight[1] - yWeight[2];
				break;
			case 3:
				/* x */
				w = x - (double)xIndex[1];
				xWeight[3] = (1.0 / 6.0) * w * w * w;
				xWeight[0] = (1.0 / 6.0) + (1.0 / 2.0) * w * (w - 1.0) - xWeight[3];
				xWeight[2] = w + xWeight[0] - 2.0 * xWeight[3];
				xWeight[1] = 1.0 - xWeight[0] - xWeight[2] - xWeight[3];
				/* y */
				w = y - (double)yIndex[1];
				yWeight[3] = (1.0 / 6.0) * w * w * w;
				yWeight[0] = (1.0 / 6.0) + (1.0 / 2.0) * w * (w - 1.0) - yWeight[3];
				yWeight[2] = w + yWeight[0] - 2.0 * yWeight[3];
				yWeight[1] = 1.0 - yWeight[0] - yWeight[2] - yWeight[3];
				break;
			case 4:
				/* x */
				w = x - (double)xIndex[2];
				w2 = w * w;
				t = (1.0 / 6.0) * w2;
				xWeight[0] = 1.0 / 2.0 - w;
				xWeight[0] *= xWeight[0];
				xWeight[0] *= (1.0 / 24.0) * xWeight[0];
				t0 = w * (t - 11.0 / 24.0);
				t1 = 19.0 / 96.0 + w2 * (1.0 / 4.0 - t);
				xWeight[1] = t1 + t0;
				xWeight[3] = t1 - t0;
				xWeight[4] = xWeight[0] + t0 + (1.0 / 2.0) * w;
				xWeight[2] = 1.0 - xWeight[0] - xWeight[1] - xWeight[3] - xWeight[4];
				/* y */
				w = y - (double)yIndex[2];
				w2 = w * w;
				t = (1.0 / 6.0) * w2;
				yWeight[0] = 1.0 / 2.0 - w;
				yWeight[0] *= yWeight[0];
				yWeight[0] *= (1.0 / 24.0) * yWeight[0];
				t0 = w * (t - 11.0 / 24.0);
				t1 = 19.0 / 96.0 + w2 * (1.0 / 4.0 - t);
				yWeight[1] = t1 + t0;
				yWeight[3] = t1 - t0;
				yWeight[4] = yWeight[0] + t0 + (1.0 / 2.0) * w;
				yWeight[2] = 1.0 - yWeight[0] - yWeight[1] - yWeight[3] - yWeight[4];
				break;
			case 5:
				/* x */
				w = x - (double)xIndex[2];
				w2 = w * w;
				xWeight[5] = (1.0 / 120.0) * w * w2 * w2;
				w2 -= w;
				w4 = w2 * w2;
				w -= 1.0 / 2.0;
				t = w2 * (w2 - 3.0);
				xWeight[0] = (1.0 / 24.0) * (1.0 / 5.0 + w2 + w4) - xWeight[5];
				t0 = (1.0 / 24.0) * (w2 * (w2 - 5.0) + 46.0 / 5.0);
				t1 = (-1.0 / 12.0) * w * (t + 4.0);
				xWeight[2] = t0 + t1;
				xWeight[3] = t0 - t1;
				t0 = (1.0 / 16.0) * (9.0 / 5.0 - t);
				t1 = (1.0 / 24.0) * w * (w4 - w2 - 5.0);
				xWeight[1] = t0 + t1;
				xWeight[4] = t0 - t1;
				/* y */
				w = y - (double)yIndex[2];
				w2 = w * w;
				yWeight[5] = (1.0 / 120.0) * w * w2 * w2;
				w2 -= w;
				w4 = w2 * w2;
				w -= 1.0 / 2.0;
				t = w2 * (w2 - 3.0);
				yWeight[0] = (1.0 / 24.0) * (1.0 / 5.0 + w2 + w4) - yWeight[5];
				t0 = (1.0 / 24.0) * (w2 * (w2 - 5.0) + 46.0 / 5.0);
				t1 = (-1.0 / 12.0) * w * (t + 4.0);
				yWeight[2] = t0 + t1;
				yWeight[3] = t0 - t1;
				t0 = (1.0 / 16.0) * (9.0 / 5.0 - t);
				t1 = (1.0 / 24.0) * w * (w4 - w2 - 5.0);
				yWeight[1] = t0 + t1;
				yWeight[4] = t0 - t1;
				break;
			default:
				throw new RuntimeException("Invalid spline degree");
		}

		// apply the mirror boundary conditions
		for(int k = 0; k <= spline_degree; k++)
		{
			xIndex[k] = (Width == 1) ? (0) : ((xIndex[k] < 0) ? (-xIndex[k] - Width2 * ((-xIndex[k]) / Width2)) : (xIndex[k] - Width2 * (xIndex[k] / Width2)));
			if (Width <= xIndex[k])
			{
				xIndex[k] = Width2 - xIndex[k];
			}
			yIndex[k] = (Height == 1) ? (0) : ((yIndex[k] < 0) ? (-yIndex[k] - Height2 * ((-yIndex[k]) / Height2)) : (yIndex[k] - Height2 * (yIndex[k] / Height2)));
			if (Height <= yIndex[k])
			{
				yIndex[k] = Height2 - yIndex[k];
			}
		}

		// perform interpolation
		interpolated = 0.0;
		for(int j = 0; j <= spline_degree; j++)
		{
			int p = yIndex[j] * Width;
			w = 0.0;
			for(int i = 0; i <= spline_degree; i++)
			{
				w += xWeight[i] * Bcoeff[p+xIndex[i]];
			}
			interpolated += yWeight[j] * w;
		}

		return interpolated;
	}

	private ImageRotatorBuffer Rotate8Bit(ImageRotatorBuffer dib, double angle, double x_shift, double y_shift, double x_origin, double y_origin, int spline, boolean use_mask, int aBackgroundColor)
	{
		double [] imageBuffer;
		double p;
		double a11, a12, a21, a22;
		double x0, y0, x1, y1;
		boolean bResult;

		int width = dib.getWidth();
		int height = dib.getHeight();

		// allocate output image
		ImageRotatorBuffer dst = new ImageRotatorBuffer(width, height);

		// allocate a temporary array
		imageBuffer = new double[width * height];

		// copy data samples
		for(int y = 0; y < height; y++)
		{
			for(int x = 0; x < width; x++)
			{
				imageBuffer[y*width+x] = (double)dib.mBuffer[dib.mWidth * (height-1-y) + x];
			}
		}

		// convert between a representation based on image samples
		// and a representation based on image B-spline coefficients
		SamplesToCoefficients(imageBuffer, width, height, spline);

		// prepare the geometry
		angle *= Math.PI / 180.0;
		a11 = Math.cos(angle);
		a12 = -Math.sin(angle);
		a21 = Math.sin(angle);
		a22 = Math.cos(angle);
		x0 = a11 * (x_shift + x_origin) + a12 * (y_shift + y_origin);
		y0 = a21 * (x_shift + x_origin) + a22 * (y_shift + y_origin);
		x_shift = x_origin - x0;
		y_shift = y_origin - y0;

		// visit all pixels of the output image and assign their value
		for(int y = 0; y < height; y++)
		{
			int dst_bits = dst.mWidth * (height-1-y);

			x0 = a12 * (double)y + x_shift;
			y0 = a22 * (double)y + y_shift;

			for(int x = 0; x < width; x++)
			{
				x1 = x0 + a11 * (double)x;
				y1 = y0 + a21 * (double)x;
				if(use_mask)
				{
					if((x1 <= -0.5) || (((double)width - 0.5) <= x1) || (y1 <= -0.5) || (((double)height - 0.5) <= y1))
					{
						p = aBackgroundColor;
					}
					else
					{
						p = (double)InterpolatedValue(imageBuffer, width, height, x1, y1, spline);
					}
				}
				else
				{
					p = (double)InterpolatedValue(imageBuffer, width, height, x1, y1, spline);
				}

				dst.mBuffer[dst_bits+x] = Math.min(Math.max(0, (int)(p + 0.5)), 255);
			}
		}

		return dst;
	}

	private void rotate(double angle, double x_shift, double y_shift, double x_origin, double y_origin, boolean use_mask, int aBackgroundColor, int aQuality)
	{
		int channel, nb_channels;

		// allocate dst image
		int width  = mWidth;
		int height = mHeight;
		int [] dst = new int[width * height];

		// allocate a temporary 8-bit dib (no need to build a palette)
		ImageRotatorBuffer tmp = new ImageRotatorBuffer(width, height);
		int [] src8 = tmp.mBuffer;

		nb_channels = 4;

		for(channel = 0; channel < nb_channels; channel++)
		{
			for (int i = 0; i < mBuffer.length; i++)
			{
				src8[i] = 255 & (mBuffer[i] >>> (channel<<3));
			}

			ImageRotatorBuffer dst8 = Rotate8Bit(tmp, angle, x_shift, y_shift, x_origin, y_origin, aQuality, use_mask, 255 & (aBackgroundColor >>> (channel<<3)));

			for (int i = 0; i < mBuffer.length; i++)
			{
				dst[i] |= dst8.mBuffer[i] << (channel<<3);
			}
		}

		mBuffer = dst;
	}


	private static Dimension getBoundingBox(int width, int height, double degrees)
	{
		// if no rotation or 180 degrees, the size won't change
		if (degrees == 0 || degrees == 180)
		{
			return new Dimension(width, height);
		}
		// if 90 or 270 (quarter or 3-quarter rotations) the width becomes
		// the height, and vice versa
		if (degrees == 90 || degrees == 270)
		{
			return new Dimension(height, width);
		}

		// for any other rotation, we need to do some trigonometry,
		// derived from description found at:
		// http://www.codeproject.com/csharp/rotateimage.asp
		double radians = Math.toRadians(degrees);
		double aW = Math.abs(Math.cos(radians) * width);
		double oW = Math.abs(Math.sin(radians) * width);
		double aH = Math.abs(Math.cos(radians) * height);
		double oH = Math.abs(Math.sin(radians) * height);
		// use Math.ceil() to round up to an int value
		int w = (int)Math.ceil(aW + oH);
		int h = (int)Math.ceil(oW + aH);
		return new Dimension(w, h);
	}


	/**
	 * Rotates the image by a given angle (given in degree) clockwise.<p>
	 *
	 * Code used to rotate images comes from the FreeImage project at
	 * http://freeimage.sourceforge.net/
     *
	 * @param aAngle
	 *    Rotation angle
	 * @param aQuality
	 *    A quality value between 1 and 5. A 1 (one) indicates that a method
	 *    using three shears should be used. This method is very fast and the
	 *    result is acceptable. A value between 2 and 5 indicates that a BSpline
	 *    method should be used. This method is approx. 8 times slower than
	 *    shearing. The BSpline method however results in some ghost shadows
	 *    along the image border. These ghost shadows are reduced when a higher
	 *    quality settings is used.
	 * @param aBackgroundColor
	 *    Background color.
	 */
	ImageRotatorBuffer rotate(double aAngle, int aQuality, int aBackgroundColor)
	{
		if (aQuality == 1)
		{
			rotateByShears(-aAngle, aBackgroundColor);
		}
		else if (aQuality >= 2 && aQuality <= 5)
		{
			Dimension d = getBoundingBox(mWidth, mHeight, aAngle);
			d.width = Math.max(mWidth, d.width);
			d.height = Math.max(mHeight, d.height);

			int [] tmp = new int[d.width * d.height];
			Arrays.fill(tmp, aBackgroundColor);

			for (int i = 0; i < mHeight; i++)
			{
				System.arraycopy(mBuffer, i*mWidth, tmp, (i+(d.height-mHeight)/2)*d.width+(d.width-mWidth)/2, mWidth);
			}

			ImageRotatorBuffer buffer = new ImageRotatorBuffer(d.width, d.height, tmp);

			buffer.rotate(aAngle, 0, 0, buffer.getWidth()/2.0-0.5, buffer.getHeight()/2.0-0.5, true, aBackgroundColor, aQuality);

			d = getBoundingBox(mWidth, mHeight, aAngle);

			tmp = new int[d.width * d.height];

			for (int i = 0; i < d.height; i++)
			{
				System.arraycopy(buffer.mBuffer, i*buffer.getWidth()+(buffer.getWidth()-d.width)/2, tmp, (i+(buffer.getHeight()-d.height)/2)*d.width, d.width);
			}

			mBuffer = tmp;
			mWidth = d.width;
			mHeight = d.height;
		}
		else
		{
			throw new IllegalArgumentException("Illegal value, must be 1..5: aQuality: " + aQuality);
		}

		return this;
	}
}