package org.terifan.ocr2;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import javax.imageio.ImageIO;

// https://en.wikipedia.org/wiki/Gabor_filter

public class ConvolutionalClassifier
{
	public static void main(String... args)
	{
		try
		{
			ArrayList<BufferedImage> images = new ArrayList<>();

			BufferedImage image = ImageIO.read(ConvolutionalClassifier.class.getResource("test_string_9.png"));
			for (int x = 0, x0 = 0; x < image.getWidth(); x++)
			{
				boolean split = true;
				for (int y = 0; y < image.getHeight(); y++)
				{
					split &= (image.getRGB(x, y) & 0xffffff) == 0xffffff;
				}
				if (split)
				{
					images.add(image.getSubimage(Math.max(x0 - 1, 0), 0, x - Math.max(x0 - 1, 0), image.getHeight()));
					x0 = x + 2;
				}
			}

			BufferedImage tmp = images.get(0);
//			BufferedImage src = new BufferedImage(10 + tmp.getWidth(), 10 + tmp.getHeight(), BufferedImage.TYPE_INT_RGB);
			BufferedImage src = new BufferedImage(28, 28, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = src.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//			g.setColor(Color.WHITE);
//			g.fillRect(0,0,src.getWidth(), src.getHeight());
//			g.drawImage(tmp, 5, 5, null);
			g.drawImage(tmp, 0, 0, src.getWidth(), src.getHeight(), null);
			g.dispose();

			System.out.println(src.getWidth() + "x" + src.getHeight());

			BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);

//			int[][] kernel =
//			{
//				{-1,-2,-1},
//				{0,0,0},
//				{1,2,1}
//			};
			int[][] kernel =
			{
				{-1,0,1},
				{-2,0,2},
				{-1,0,1}
			};

			filerImage(src, dst, kernel);

			ImageIO.write(src, "png", new File("d:/output-src.png"));
			ImageIO.write(dst, "png", new File("d:/output-dst.png"));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static void filerImage(BufferedImage aSrc, BufferedImage aDst, int[][] aKernel)
	{
		int sw = aSrc.getWidth();
		int sh = aSrc.getHeight();
		int kw = aKernel[0].length;
		int kh = aKernel.length;

		for (int y = 0; y < sh; y++)
		{
			for (int x = 0; x < sw; x++)
			{
				int sum = 0;

				for (int ky = 0; ky < kh; ky++)
				{
					for (int kx = 0; kx < kw; kx++)
					{
						int sx = x + kx - kw / 2;
						int sy = y + ky - kh / 2;
						sx = Math.max(Math.min(sx, sw - 1), 0);
						sy = Math.max(Math.min(sy, sh - 1), 0);

						sum += aKernel[ky][kx] * (aSrc.getRGB(sx, sy) & 0xff);
					}
				}

				int c = 128 + Math.max(-128, Math.min(127, sum / (kw * kh)));

				aDst.setRGB(x, y, (c << 16) + (c << 8) + c);
			}
		}
	}

//	public static void main(String... args)
//	{
//		try
//		{
//			BufferedImage template = ImageIO.read(ConvolutionalClassifier.class.getResource("simple/2.png"));
//
//		}
//		catch (Throwable e)
//		{
//			e.printStackTrace(System.out);
//		}
//	}
//
//
//	private static void loadData() throws IOException
//	{
//		try (DataInputStream in = new DataInputStream(ConvolutionalClassifier.class.getResourceAsStream("train-labels.idx1-ubyte")))
//		{
//			int magic = in.readInt();
//			int count = in.readInt();
//			System.out.println(in.readUnsignedByte());
//			System.out.println(in.readUnsignedByte());
//			System.out.println(in.readUnsignedByte());
//			System.out.println(in.readUnsignedByte());
//			System.out.println(in.readUnsignedByte());
//			System.out.println(in.readUnsignedByte());
//		}
//		try (DataInputStream in = new DataInputStream(ConvolutionalClassifier.class.getResourceAsStream("train-images.idx3-ubyte")))
//		{
//			int magic = in.readInt();
//			int count = in.readInt();
//			int w = in.readInt();
//			int h = in.readInt();
//			int[][] dat = new int[h][w];
//
//			for (int index = 0; index < 15; index++)
//			{
//				BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
//				for (int i = 0; i < w; i++)
//				{
//					for (int j = 0; j < h; j++)
//					{
//						dat[j][i] = in.readUnsignedByte();
//						int c = 0xff & ~dat[j][i];
//						image.setRGB(j, i, (c<<16)+(c<<8)+c);
//					}
//				}
//				ImageIO.write(image, "png", new File("d:/test_"+index+".png"));
//			}
//		}
//	}
}
