package org.terifan.ocr;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;


@Deprecated
class ImageTools
{
	public static BufferedImage resize(BufferedImage aImage, int aWidth, int aHeight, Object aRenderingHints, int aType)
	{
		if (aWidth < aImage.getWidth() || aHeight < aImage.getHeight())
		{
			aImage = resizeDown(aImage, Math.min(aWidth, aImage.getWidth()), Math.min(aHeight, aImage.getHeight()), aRenderingHints, aType);
		}

		if (aWidth > aImage.getWidth() || aHeight > aImage.getHeight())
		{
			BufferedImage temp = new BufferedImage(aWidth, aHeight, aType);

			Graphics2D g = temp.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, aRenderingHints);
			g.drawImage(aImage, 0, 0, aWidth, aHeight, 0, 0, aImage.getWidth(), aImage.getHeight(), null);
			g.dispose();

			aImage = temp;
		}

		return aImage;
	}


	private static BufferedImage resizeDown(BufferedImage aImage, int aTargetWidth, int aTargetHeight, Object aRenderingHints, int aType)
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
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, aRenderingHints);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.drawImage(ret, 0, 0, w, h, null);
			g.dispose();

			ret = tmp;
		}
		while (w != aTargetWidth || h != aTargetHeight);

		return ret;
	}
}