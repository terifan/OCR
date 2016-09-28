package org.terifan.ocr;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ConvolutionalClassifier 
{
	public static void main(String... args)
	{
		try
		{
			BufferedImage template = ImageIO.read(ConvolutionalClassifier.class.getResource("simple/2.png"));
			
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static void loadData() throws IOException
	{
		try (DataInputStream in = new DataInputStream(ConvolutionalClassifier.class.getResourceAsStream("train-labels.idx1-ubyte")))
		{
			int magic = in.readInt();
			int count = in.readInt();
			System.out.println(in.readUnsignedByte());
			System.out.println(in.readUnsignedByte());
			System.out.println(in.readUnsignedByte());
			System.out.println(in.readUnsignedByte());
			System.out.println(in.readUnsignedByte());
			System.out.println(in.readUnsignedByte());
		}
		try (DataInputStream in = new DataInputStream(ConvolutionalClassifier.class.getResourceAsStream("train-images.idx3-ubyte")))
		{
			int magic = in.readInt();
			int count = in.readInt();
			int w = in.readInt();
			int h = in.readInt();
			int[][] dat = new int[h][w];
			
			for (int index = 0; index < 15; index++)
			{
				BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
				for (int i = 0; i < w; i++)
				{
					for (int j = 0; j < h; j++)
					{
						dat[j][i] = in.readUnsignedByte();
						int c = 0xff & ~dat[j][i];
						image.setRGB(j, i, (c<<16)+(c<<8)+c);
					}
				}
				ImageIO.write(image, "png", new File("d:/test_"+index+".png"));
			}
		}
	}
}
