package org.terifan.ocr;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;


public class ImageTools
{
	/**
	 * Resizes an image.
	 *
	 * @param aImage
	 *   Source image
	 * @param aWidth
	 *   New width
	 * @param aHeight
	 *   New Height
	 */
	public static BufferedImage resize(BufferedImage aImage, int aWidth, int aHeight)
	{
		return resize(aImage, aWidth, aHeight, InterpolationMode.BILINEAR);
	}


	/**
	 * Resizes an image.
	 *
	 * @param aImage
	 *   Source image
	 * @param aWidth
	 *   New width
	 * @param aHeight
	 *   New Height
	 * @param aInterpolationMode
	 *   Color interpolation mode
	 * @see java.awt.RenderingHints#VALUE_INTERPOLATION_NEAREST_NEIGHBOR
	 * @see java.awt.RenderingHints#VALUE_INTERPOLATION_BILINEAR
	 * @see java.awt.RenderingHints#VALUE_INTERPOLATION_BICUBIC
	 */
	public static BufferedImage resize(BufferedImage aImage, int aWidth, int aHeight, InterpolationMode aInterpolationMode)
	{
		int type = aImage.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : aImage.getType();

		return resize(aImage, aWidth, aHeight, aInterpolationMode, type);
	}


	/**
	 * Resizes an image keeping the ascpect ratio.<p>
	 *
	 * If the output width/height is smaller then the input size then a area
	 * average routine will be used.
	 *
	 * @param aImage
	 *   Source image
	 * @param aWidth
	 *   New width
	 * @param aHeight
	 *   New Height
	 * @param aInterpolationMode
	 *   Color interpolation mode
	 * @param aType
	 *   Is the returned image format type. Use one of the BufferedImage's types.
	 * @see java.awt.RenderingHints#VALUE_INTERPOLATION_NEAREST_NEIGHBOR
	 * @see java.awt.RenderingHints#VALUE_INTERPOLATION_BILINEAR
	 * @see java.awt.RenderingHints#VALUE_INTERPOLATION_BICUBIC
	 * @see java.awt.image.BufferedImage#TYPE_INT_RGB
	 * @see java.awt.image.BufferedImage#TYPE_INT_ARGB
	 * @see java.awt.image.BufferedImage#TYPE_INT_ARGB_PRE
	 * @see java.awt.image.BufferedImage#TYPE_INT_BGR
	 * @see java.awt.image.BufferedImage#TYPE_3BYTE_BGR
	 * @see java.awt.image.BufferedImage#TYPE_4BYTE_ABGR
	 * @see java.awt.image.BufferedImage#TYPE_4BYTE_ABGR_PRE
	 * @see java.awt.image.BufferedImage#TYPE_BYTE_GRAY
	 * @see java.awt.image.BufferedImage#TYPE_BYTE_BINARY
	 * @see java.awt.image.BufferedImage#TYPE_BYTE_INDEXED
	 * @see java.awt.image.BufferedImage#TYPE_USHORT_GRAY
	 * @see java.awt.image.BufferedImage#TYPE_USHORT_565_RGB
	 * @see java.awt.image.BufferedImage#TYPE_USHORT_555_RGB
	 */
	public static BufferedImage resizeAspect(BufferedImage aImage, int aWidth, int aHeight, InterpolationMode aInterpolationMode, int aType)
	{
		if (aImage == null)
		{
			throw new IllegalArgumentException("Image is null");
		}

		double f = Math.max(aImage.getWidth() / (double) aWidth, aImage.getHeight() / (double) aHeight);

		int dw = (int) (aImage.getWidth() / f);
		int dh = (int) (aImage.getHeight() / f);

		// make sure one direction has specified dimension
		if (dw != aWidth && dh != aHeight)
		{
			if (Math.abs(aWidth - dw) < Math.abs(aHeight - dh))
			{
				dw = aWidth;
			}
			else
			{
				dh = aHeight;
			}
		}

		return resize(aImage, Math.max(dw,1), Math.max(dh,1), aInterpolationMode, aType);
	}


	/**
	 * Resizes an image.<p>
	 *
	 * If the output width/height is smaller then the input size then a area
	 * average routine will be used.
	 *
	 * @param aImage
	 *   Source image
	 * @param aWidth
	 *   New width
	 * @param aHeight
	 *   New Height
	 * @param aInterpolationMode
	 *   Color interpolation mode
	 * @param aType
	 *   Is the returned image format type. Use one of the BufferedImage's types.
	 * @see java.awt.RenderingHints#VALUE_INTERPOLATION_NEAREST_NEIGHBOR
	 * @see java.awt.RenderingHints#VALUE_INTERPOLATION_BILINEAR
	 * @see java.awt.RenderingHints#VALUE_INTERPOLATION_BICUBIC
	 * @see java.awt.image.BufferedImage#TYPE_INT_RGB
	 * @see java.awt.image.BufferedImage#TYPE_INT_ARGB
	 * @see java.awt.image.BufferedImage#TYPE_INT_ARGB_PRE
	 * @see java.awt.image.BufferedImage#TYPE_INT_BGR
	 * @see java.awt.image.BufferedImage#TYPE_3BYTE_BGR
	 * @see java.awt.image.BufferedImage#TYPE_4BYTE_ABGR
	 * @see java.awt.image.BufferedImage#TYPE_4BYTE_ABGR_PRE
	 * @see java.awt.image.BufferedImage#TYPE_BYTE_GRAY
	 * @see java.awt.image.BufferedImage#TYPE_BYTE_BINARY
	 * @see java.awt.image.BufferedImage#TYPE_BYTE_INDEXED
	 * @see java.awt.image.BufferedImage#TYPE_USHORT_GRAY
	 * @see java.awt.image.BufferedImage#TYPE_USHORT_565_RGB
	 * @see java.awt.image.BufferedImage#TYPE_USHORT_555_RGB
	 */
	public static BufferedImage resize(BufferedImage aImage, int aWidth, int aHeight, InterpolationMode aInterpolationMode, int aType)
	{
		if (aWidth < aImage.getWidth() || aHeight < aImage.getHeight())
		{
			aImage = resizeDown(aImage, Math.min(aWidth, aImage.getWidth()), Math.min(aHeight, aImage.getHeight()), aInterpolationMode, aType);
		}

		if (aWidth > aImage.getWidth() || aHeight > aImage.getHeight())
		{
			BufferedImage temp = new BufferedImage(aWidth, aHeight, aType);

			Graphics2D g = temp.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, aInterpolationMode.getHint());
			g.drawImage(aImage, 0, 0, aWidth, aHeight, 0, 0, aImage.getWidth(), aImage.getHeight(), null);
			g.dispose();

			aImage = temp;
		}

		return aImage;
	}


	private static BufferedImage resizeDown(BufferedImage aImage, int aTargetWidth, int aTargetHeight, InterpolationMode aInterpolationMode, int aType)
	{
		if (aTargetWidth <= 0 || aTargetHeight <= 0)
		{
			throw new IllegalArgumentException("Width or height is zero or less: width: " + aTargetWidth + ", height: " + aTargetHeight);
		}

		int w = aImage.getWidth();
		int h = aImage.getHeight();
		BufferedImage ret = aImage;

		do
		{
			if (w > aTargetWidth)
			{
				w = Math.max(w / 2, aTargetWidth);
			}
			if (h > aTargetHeight)
			{
				h = Math.max(h / 2, aTargetHeight);
			}

			BufferedImage tmp = new BufferedImage(w, h, aType);
			Graphics2D g = tmp.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, aInterpolationMode.getHint());
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.drawImage(ret, 0, 0, w, h, null);
			g.dispose();

			ret = tmp;
		}
		while (w != aTargetWidth || h != aTargetHeight);

		return ret;
	}


	public static void replaceColor(BufferedImage aImage, int aSourceColor, int aNewColor)
	{
		int[] raster = ((DataBufferInt) aImage.getRaster().getDataBuffer()).getData();
		for (int i = 0; i < raster.length; i++)
		{
			int c = raster[i];
			if (c == aSourceColor)
			{
				c = aNewColor;
			}
			raster[i] = c;
		}
	}


	public static void areaAverageResizeDown(BufferedImage aSrc, BufferedImage aDst)
	{
		int srcWidth = aSrc.getWidth();
		int srcHeight = aSrc.getHeight();
		int dstWidth = aDst.getWidth();
		int dstHeight = aDst.getHeight();

		double xScale = srcWidth / (double) dstWidth;
		double yScale = srcHeight / (double) dstHeight;

		int[] tempRaster = new int[srcWidth];

		for (int dstY = 0; dstY < dstHeight; dstY++)
		{
			for (int dstX = 0; dstX < dstWidth; dstX++)
			{
				int x = (int) (dstX * xScale);
				int y = (int) (dstY * yScale);
				int w = Math.min((int) ((dstX + 1) * xScale), srcWidth) - x;
				int h = Math.min((int) ((dstY + 1) * yScale), srcHeight) - y;
				int cnt = w * h;

				if (cnt > 0)
				{
					int r = 0, g = 0, b = 0, a = 0;

					aSrc.getRGB(x, y, w, h, tempRaster, 0, w);

					for (int i = 0; i < cnt; i++)
					{
						int color = tempRaster[i];
						a += 255 & (color >>> 24);
						r += 255 & (color >> 16);
						g += 255 & (color >> 8);
						b += 255 & (color);
					}

					aDst.setRGB(dstX, dstY, ((a / cnt) << 24) + ((r / cnt) << 16) + ((g / cnt) << 8) + (b / cnt));
				}
			}
		}
	}
}