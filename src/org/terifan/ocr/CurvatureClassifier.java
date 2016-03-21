package org.terifan.ocr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;


class CurvatureClassifier
{
	private final static int BILEVEL_THRESHOLD = 128;
	private final static int MATRIX_SIZE = 16;

	private ArrayList<Symbol> mSymbols;
	private Page mPage;


	public CurvatureClassifier()
	{
		clearAlphabets();
	}


	public void init(Page aPage)
	{
		mPage = aPage;
	}


	public void learn(String aFontName, Page aPage)
	{
		init(aPage);

		PageSegmenter segmenter = new PageSegmenter();

		segmenter.mLearning = true;

//		ArrayList<TextBox> textBoxes = segmenter.scanPage(0, 0, 1, 1, aPage, 1.4, 0, 0, 100, 75);
		ArrayList<TextBox> textBoxes = new ArrayList<>();
		for (int y = 0; y < 6; y++)
		{
			for (int x = 0; x < 13; x++)
			{
				textBoxes.add(new TextBox(new Rectangle(1 + 71 * x, 1 + 69 * y, 69, 67)));
			}
		}

		for (TextBox box : textBoxes)
		{
			learnSymbol(aFontName, box);
		}
	}


	public void clearAlphabets()
	{
		mSymbols = new ArrayList<>();
	}


	private void extractBitmap(Symbol aSymbol)
	{
		TextBox box = aSymbol.mTextBox;

		Insets borders = Tools.getBorders(mPage, box.x, box.y, box.width, box.height);

		aSymbol.mBorders = borders;

		BufferedImage tmp = mPage.getRegion(box.x + borders.left, box.y + borders.top, box.x + box.width - borders.right + 1, box.y + box.height - borders.bottom + 1);

		BufferedImage tmp2 = ImageTools.resize(tmp, MATRIX_SIZE, MATRIX_SIZE, InterpolationMode.BICUBIC, BufferedImage.TYPE_INT_RGB);

		aSymbol.mBitmap = tmp2;

//		if (box.x == 1283 && box.y == 868)
//		{
//			try
//			{
//				if (aSymbol.mCharacter != null)
//				{
//					String c = aSymbol.mCharacter.replace("\"", "").replace(":", "").replace("?", "").replace("<", "").replace(">", "").replace("\\", "").replace("/", "").replace("*", "");
//					if (c.charAt(0) >= 'a' && c.charAt(0) <= 'z')
//					{
//						c = "L" + Character.toUpperCase(c.charAt(0));
//					}
//					else if (c.charAt(0) >= 'A' && c.charAt(0) <= 'Z')
//					{
//						c = "U" + c;
//					}
//					else if (c.charAt(0) < 'A' || c.charAt(0) >= 'z')
//					{
//						c = "";
//					}
//					ImageIO.write(tmp2, "png", new File("d:/temp/bitmap_alphabet/" + c + "_" + box.y + "_" + box.x + ".png"));
//				}
//				else
//				{
//					System.out.println(borders);
//
//					BufferedImage tmp3 = mPage.getRegion(box.x + borders.left - 2, box.y + borders.top - 2, box.x + box.width - borders.right + 1 + 4, box.y + box.height - borders.bottom + 1 + 4);
//
//					ImageIO.write(tmp, "png", new File("d:/temp/bitmap_symbols/" + box.y + "_" + box.x + "_a.png"));
//					ImageIO.write(tmp2, "png", new File("d:/temp/bitmap_symbols/" + box.y + "_" + box.x + "_b.png"));
//					ImageIO.write(tmp3, "png", new File("d:/temp/bitmap_symbols/" + box.y + "_" + box.x + "_c.png"));
//				}
//			}
//			catch (Exception e)
//			{
//			}
//		}
	}


	private void extractTemplateDistance(Symbol aSymbol)
	{
		aSymbol.mClosestPixel = new int[MATRIX_SIZE][MATRIX_SIZE];

		for (int y = 0; y < MATRIX_SIZE; y++)
		{
			for (int x = 0; x < MATRIX_SIZE; x++)
			{
				aSymbol.mClosestPixel[y][x] = findClosestPixel(aSymbol, x, y);
			}
		}
	}


	private void extractContour(Symbol aSymbol)
	{
		boolean debug = OCREngine.isDebugEnabled(mPage);

		BufferedImage image = aSymbol.mBitmap;

		double[][] contour = new double[8][MATRIX_SIZE];
		int[][] count = new int[8][MATRIX_SIZE];

		int w = image.getWidth();
		int h = image.getHeight();

		double fx = MATRIX_SIZE / (double)w;
		double fy = MATRIX_SIZE / (double)h;

		for (int ori = 0; ori < 2; ori++)
		{
			for (int y = 0; y < h; y++)
			{
				int x = w * ori / 2;
				for (; x < w; x++)
				{
					if ((image.getRGB(x, y) & 255) < BILEVEL_THRESHOLD)
					{
						break;
					}
				}
				int z = (int)Math.min(MATRIX_SIZE - 1, Math.round(y * fy));

				contour[4 * ori + 0][z] += fx * x;
				count[4 * ori + 0][z] += 1;
			}

			for (int y = 0; y < h; y++)
			{
				int x = w - 1 - w * ori / 2;
				for (; x >= 0; x--)
				{
					if ((image.getRGB(x, y) & 255) < BILEVEL_THRESHOLD)
					{
						break;
					}
				}
				int z = (int)Math.min(MATRIX_SIZE - 1, Math.round(y * fy));
				contour[4 * ori + 1][z] += fx * x;
				count[4 * ori + 1][z] += 1;
			}

			for (int x = 0; x < w; x++)
			{
				int y = h * ori / 2;
				for (; y < h; y++)
				{
					if ((image.getRGB(x, y) & 255) < BILEVEL_THRESHOLD)
					{
						break;
					}
				}
				int z = (int)Math.min(MATRIX_SIZE - 1, Math.round(x * fx));
				contour[4 * ori + 2][z] += fy * y;
				count[4 * ori + 2][z] += 1;
			}

			for (int x = 0; x < w; x++)
			{
				int y = h - 1 - h * ori / 2;
				for (; y >= 0; y--)
				{
					if ((image.getRGB(x, y) & 255) < BILEVEL_THRESHOLD)
					{
						break;
					}
				}

				int z = (int)Math.min(MATRIX_SIZE - 1, Math.round(x * fx));
				contour[4 * ori + 3][z] += fy * y;
				count[4 * ori + 3][z] += 1;
			}
		}

		for (int ori = 0; ori < 8; ori++)
		{
			for (int i = 0; i < MATRIX_SIZE; i++)
			{
				contour[ori][i] /= count[ori][i];
			}
		}

		if (debug)
		{
			for (double[] f : contour)
			{
				for (double d : f)
				{
					System.out.print((int)Math.round(d) + "\t");
				}
				System.out.println();
			}
		}

		aSymbol.mContour = contour;
	}


	private void extractSlopes(Symbol aSymbol)
	{
		boolean debug = OCREngine.isDebugEnabled(mPage);

		int[][] slopes = new int[8][MATRIX_SIZE];
		double[][] contour = aSymbol.mContour;

		for (int orientation = 0; orientation < 8; orientation++)
		{
			boolean hor = (orientation == 2 || orientation == 3 || orientation == 6 || orientation == 7);

			for (int index = 0; index < MATRIX_SIZE; index++)
			{
				double b = (int)(contour[orientation][index]);
				double a = index == 0 ? b : (int)(contour[orientation][index - 1]);
				double c = index == MATRIX_SIZE - 1 ? b : (int)(contour[orientation][index + 1]);

				if (b == -1 || b == MATRIX_SIZE)
				{
					slopes[orientation][index] = -1;
					if (debug)
					{
						System.out.print("#");
					}
				}
				else if (a == b && b == c)
				{
					slopes[orientation][index] = 0;
					if (debug)
					{
						System.out.print(hor ? "-" : "|");
					}
				}
				else if (a == MATRIX_SIZE && b == c)
				{
					slopes[orientation][index] = 0;
					if (debug)
					{
						System.out.print(hor ? "-" : "|");
					}
				}
				else if (a == b && c == MATRIX_SIZE)
				{
					slopes[orientation][index] = 0;
					if (debug)
					{
						System.out.print(hor ? "-" : "|");
					}
				}
				else if (a < b && c < b)
				{
					slopes[orientation][index] = 0;
					if (debug)
					{
						System.out.print(hor ? "-" : "|");
					}
				}
				else if (a > b && c > b)
				{
					slopes[orientation][index] = 0;
					if (debug)
					{
						System.out.print(hor ? "-" : "|");
					}
				}
				else if (a > b && c <= b)
				{
					slopes[orientation][index] = 1;
					if (debug)
					{
						System.out.print("/");
					}
				}
				else if (a >= b && c < b)
				{
					slopes[orientation][index] = 1;
					if (debug)
					{
						System.out.print("/");
					}
				}
				else if (a == MATRIX_SIZE && c < b)
				{
					slopes[orientation][index] = 1;
					if (debug)
					{
						System.out.print("/");
					}
				}
				else if (a > b && c == MATRIX_SIZE)
				{
					slopes[orientation][index] = 1;
					if (debug)
					{
						System.out.print("/");
					}
				}
				else if (a < b && c >= b)
				{
					slopes[orientation][index] = 2;
					if (debug)
					{
						System.out.print("\\");
					}
				}
				else if (a <= b && c > b)
				{
					slopes[orientation][index] = 2;
					if (debug)
					{
						System.out.print("\\");
					}
				}
				else if (a == MATRIX_SIZE && c > b)
				{
					slopes[orientation][index] = 2;
					if (debug)
					{
						System.out.print("\\");
					}
				}
				else if (a < b && c == MATRIX_SIZE)
				{
					slopes[orientation][index] = 3;
					if (debug)
					{
						System.out.print("\\");
					}
				}
				else if (a == MATRIX_SIZE && b == MATRIX_SIZE && c == MATRIX_SIZE)
				{
					slopes[orientation][index] = -1;
					if (debug)
					{
						System.out.print("#");
					}
				}
				else
				{
					slopes[orientation][index] = -1;
					if (debug)
					{
						System.out.print("?");
					}
				}
			}
			if (debug)
			{
				System.out.println();
			}
		}

		aSymbol.mSlopes = slopes;
	}


	private void extractCurvature(Symbol aSymbol)
	{
		boolean debug = OCREngine.isDebugEnabled(mPage);

		int scale = 8;
		int padd = 16;

		BufferedImage output = null;
		Graphics2D g = null;
		if (debug)
		{
			output = new BufferedImage(4 * MATRIX_SIZE * scale + padd + 4 * padd, 2 * MATRIX_SIZE * scale + padd + 2 * padd, BufferedImage.TYPE_INT_RGB);
			g = (Graphics2D)output.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, output.getWidth(), output.getHeight());
			for (int y = 0; y < 2; y++)
			{
				for (int x = 0; x < 4; x++)
				{
					g.setColor(new Color(255, 255, 255, 240));
					g.drawImage(aSymbol.mBitmap, padd + x * padd + x * MATRIX_SIZE * scale, padd + y * padd + y * MATRIX_SIZE * scale, MATRIX_SIZE * scale, MATRIX_SIZE * scale, null);

					if (y == 1)
					{
						if (x == 0)
						{
							g.fillRect(padd + x * padd + x * MATRIX_SIZE * scale, padd + y * padd + y * MATRIX_SIZE * scale, MATRIX_SIZE * scale / 2, MATRIX_SIZE * scale);
						}
						if (x == 1)
						{
							g.fillRect(padd + x * padd + x * MATRIX_SIZE * scale + MATRIX_SIZE * scale / 2, padd + y * padd + y * MATRIX_SIZE * scale, MATRIX_SIZE * scale / 2, MATRIX_SIZE * scale);
						}
						if (x == 2)
						{
							g.fillRect(padd + x * padd + x * MATRIX_SIZE * scale, padd + y * padd + y * MATRIX_SIZE * scale, MATRIX_SIZE * scale, MATRIX_SIZE * scale / 2);
						}
						if (x == 3)
						{
							g.fillRect(padd + x * padd + x * MATRIX_SIZE * scale, padd + y * padd + y * MATRIX_SIZE * scale + MATRIX_SIZE * scale / 2, MATRIX_SIZE * scale, MATRIX_SIZE * scale / 2);
						}
					}

					g.setColor(new Color(255, 255, 255, 64));
					for (int i = 0; i <= MATRIX_SIZE; i++)
					{
						int ix = padd + x * padd + x * MATRIX_SIZE * scale;
						int iy = padd + y * padd + y * MATRIX_SIZE * scale;
						g.drawLine(ix + i * scale, iy, ix + i * scale, iy + MATRIX_SIZE * scale);
						g.drawLine(ix, iy + i * scale, ix + MATRIX_SIZE * scale, iy + i * scale);
					}
				}
			}
			g.setStroke(new BasicStroke(3));
			g.setColor(new Color(0, 0, 0, 16));
			for (int y = 0; y < 2; y++)
			{
				for (int x = 0; x < 4; x++)
				{
					for (int i = 1; i < 3; i++)
					{
						if (x > 1)
						{
							g.drawLine(padd + x * padd + x * MATRIX_SIZE * scale + i * MATRIX_SIZE * scale / 3, padd + y * padd + y * MATRIX_SIZE * scale, padd + x * padd + x * MATRIX_SIZE * scale + i * MATRIX_SIZE * scale / 3, padd + y * padd + (y + 1) * MATRIX_SIZE * scale);
						}
						else
						{
							g.drawLine(padd + x * padd + x * MATRIX_SIZE * scale, padd + y * padd + y * MATRIX_SIZE * scale + i * MATRIX_SIZE * scale / 3, padd + x * padd + (x + 1) * MATRIX_SIZE * scale, padd + y * padd + y * MATRIX_SIZE * scale + i * MATRIX_SIZE * scale / 3);
						}
					}
				}
			}
		}

		double[][] contour = aSymbol.mContour;
		int[][] slopes = aSymbol.mSlopes;

		aSymbol.mCurvature = new Polygon[8][];
		aSymbol.mCurvatureSlopes = new int[8][];

		ArrayList<Polygon> polygons = new ArrayList<>();
		ArrayList<Integer> polygonSlopes = new ArrayList<>();

		for (int orientation = 0; orientation < 8; orientation++)
		{
			boolean hor = (orientation == 2 || orientation == 3 || orientation == 6 || orientation == 7);
			int tx = 0;

			int fromX = 0;
			int fromY = 0;
			boolean first = true;

			for (int i = 0; i < MATRIX_SIZE; i++)
			{
				if (first)
				{
					tx = (int)contour[orientation][i];
				}
				else
				{
					for (int startSlope = slopes[orientation][i]; i < MATRIX_SIZE; i++)
					{
						if (contour[orientation][i] == -1)
						{
							break;
						}
						if (startSlope != slopes[orientation][i] && slopes[orientation][i] != 0)
						{
							break;
						}
						tx = (int)contour[orientation][i];
					}
				}

				if (tx == -1 || tx == MATRIX_SIZE)
				{
					first = true;
					continue;
				}

				int toX, toY;

				if (hor)
				{
					toX = i - (first ? 0 : 1);
					toY = tx;
				}
				else
				{
					toX = tx;
					toY = i - (first ? 0 : 1);
				}

				if (!first && tx > -1 && tx < MATRIX_SIZE && (fromX != toX || Math.abs(fromY - toY) > 0) && (fromY != toY || Math.abs(fromX - toX) > 0))
				{
					int x1 = (int)(padd + scale * fromX) + (orientation % 4) * (padd + scale * MATRIX_SIZE);
					int y1 = (int)(padd + scale * fromY) + (orientation / 4) * (padd + scale * MATRIX_SIZE);
					int x2 = (int)(padd + scale * toX) + (orientation % 4) * (padd + scale * MATRIX_SIZE);
					int y2 = (int)(padd + scale * toY) + (orientation / 4) * (padd + scale * MATRIX_SIZE);

					int slope;

					if (orientation == 0 || orientation == 4)
					{
						if (x2 < x1)
						{
							slope = 1;
						}
						else
						{
							slope = -1;
						}
					}
					else if (orientation == 1 || orientation == 5)
					{
						if (x2 < x1)
						{
							slope = -1;
						}
						else
						{
							slope = 1;
						}
					}
					else if (orientation == 2 || orientation == 6)
					{
						if (y2 < y1)
						{
							slope = -1;
						}
						else
						{
							slope = 1;
						}
					}
					else
					{
						if (y2 < y1)
						{
							slope = 1;
						}
						else
						{
							slope = -1;
						}
					}

					Polygon polyA = new Polygon(new int[]
					{
						fromX, toX, fromX
					}, new int[]
					{
						fromY, toY, toY
					}, 3);
					Polygon polyB = new Polygon(new int[]
					{
						fromX, toX, toX
					}, new int[]
					{
						fromY, toY, fromY
					}, 3);

					polygons.add(slope == 1 ? polyA : polyB);
					polygonSlopes.add(hor ? (slope == 1 ? -1 : 1) : slope);

					if (debug)
					{
						int[][] points = slope == 1 ? new int[][]
						{
							{
								x1, x2, x1
							},
							{
								y1, y2, y2
							}
						} : new int[][]
						{
							{
								x1, x2, x2
							},
							{
								y1, y2, y1
							}
						};

//						System.out.println(fromX+", "+fromY+", "+toX+", "+toY);
						//Color c = Color.getHSBColor(orientation/8f, slope==1?0.5f:1, 1);
						Color c = Color.getHSBColor((hor ? (slope == 1 ? -1 : 1) : slope) == 1 ? 0f : 0.5f, 1, 1);
						g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 128));
						g.fillPolygon(points[0], points[1], 3);
						g.setColor(c);
						g.drawLine(x1, y1, x2, y2);
					}
				}

				fromX = toX;
				fromY = toY;

				first = false;

				if (i < MATRIX_SIZE && contour[orientation][i] == -1)
				{
					first = true;
				}
			}

			aSymbol.mCurvature[orientation] = polygons.toArray(new Polygon[polygons.size()]);

			aSymbol.mCurvatureSlopes[orientation] = new int[polygonSlopes.size()];

			for (int i = 0; i < polygonSlopes.size(); i++)
			{
				aSymbol.mCurvatureSlopes[orientation][i] = polygonSlopes.get(i);
			}

			polygons.clear();
			polygonSlopes.clear();
		}

		if (debug)
		{
			g.dispose();

			if (aSymbol.mCharacter != null)
			{
				String character = aSymbol.mCharacter;
				char c = character.charAt(0);
				if (c == '\"')
				{
					character = "quot";
				}
				if (c == '*')
				{
					character = "star";
				}
				if (c == '/')
				{
					character = "slash";
				}
				if (c == '\\')
				{
					character = "backslash";
				}
				if (c == '.')
				{
					character = "dot";
				}
				if (c == '-')
				{
					character = "dash";
				}
				if (c == ':')
				{
					character = "colon";
				}
				if (c == ',')
				{
					character = "comma";
				}
				if (c == '&')
				{
					character = "amp";
				}
				if (c == '(')
				{
					character = "lparan";
				}
				if (c == ')')
				{
					character = "rparan";
				}
				if (c == '=')
				{
					character = "equal";
				}

				try
				{
					ImageIO.write(output, "png", new File("d:/temp/classifier_alphabet/" + (Character.isLowerCase(c) ? "L" : "") + (Character.isUpperCase(c) ? "U" : "") + character + ".png"));
				}
				catch (Exception e)
				{
					throw new IllegalStateException(e);
				}
			}
			else
			{
				try
				{
					ImageIO.write(output, "png", new File("d:/temp/classifier_symbols/" + aSymbol.mTextBox.y + "_" + aSymbol.mTextBox.x + ".png"));
				}
				catch (IOException e)
				{
					throw new IllegalStateException(e);
				}
			}

			//if("�".equals(aSymbol.mCharacter))System.exit(0);
		}
	}


	private void extractCurvatureVector(Symbol aSymbol)
	{
		boolean debug = OCREngine.isDebugEnabled(mPage);

		Polygon[][] polygons = aSymbol.mCurvature;

		double[][][] fill = aSymbol.mCurvatureVector = new double[8][2][3];

		for (int orientation = 0; orientation < 8; orientation++)
		{
			boolean hor = orientation == 0 || orientation == 1 || orientation == 4 || orientation == 5;

			for (int i = 0; i < polygons[orientation].length; i++)
			{
				Polygon p = polygons[orientation][i];

				int type = aSymbol.mCurvatureSlopes[orientation][i] == -1 ? 0 : 1;

				double x0, y0, x3, y3;

				if (hor)
				{
					x0 = p.ypoints[0];
					y0 = p.xpoints[0];
					x3 = p.ypoints[1];
					y3 = p.xpoints[1];
				}
				else
				{
					x0 = p.xpoints[0];
					y0 = p.ypoints[0];
					x3 = p.xpoints[1];
					y3 = p.ypoints[1];
				}

				double fx = 1;
				double fy = Math.abs((y3 - y0) / (x3 - x0));

				double d1 = 1 * MATRIX_SIZE / 3.0 - x0;
				double d2 = 2 * MATRIX_SIZE / 3.0 - x0;

				double x1 = x0 + fx * d1;
				double y1 = y0 + fy * d1;

				double x2 = x0 + fx * d2;
				double y2 = y0 + fy * d2;

				double area1 = Math.abs((x1 - x0) * (y1 - y0)) / 2.0;
				double area2 = Math.abs((x2 - x0) * (y2 - y0)) / 2.0;
				double area3 = Math.abs((x3 - x0) * (y3 - y0)) / 2.0;

				if (!hor)
				{
					area1 = Math.abs((x1 - x0) * (y3 - y0)) - area1;
					area2 = Math.abs((x2 - x0) * (y3 - y0)) - area2;
					area3 = Math.abs((x3 - x0) * (y3 - y0)) - area3;
				}

				if (x0 <= MATRIX_SIZE / 3.0 && x3 <= MATRIX_SIZE / 3.0)
				{
					fill[orientation][type][0] += area3;
				}
				else if (x0 > MATRIX_SIZE / 3.0 && x3 < 2 * MATRIX_SIZE / 3.0)
				{
					fill[orientation][type][1] += area3;
				}
				else if (x0 >= 2 * MATRIX_SIZE / 3.0 && x3 >= 2 * MATRIX_SIZE / 3.0)
				{
					fill[orientation][type][2] += area3;
				}
				else if (x0 <= MATRIX_SIZE / 3.0 && x3 < 2 * MATRIX_SIZE / 3.0)
				{
					fill[orientation][type][0] += area1;
					fill[orientation][type][1] += area3 - area1;
				}
				else if (x0 <= MATRIX_SIZE / 3.0 && x3 >= 2 * MATRIX_SIZE / 3.0)
				{
					fill[orientation][type][0] += area1;
					fill[orientation][type][1] += area2 - area1;
					fill[orientation][type][2] += area3 - area2;
				}
				else if (x0 < 2 * MATRIX_SIZE / 3.0 && x3 >= 2 * MATRIX_SIZE / 3.0)
				{
					fill[orientation][type][1] += area2;
					fill[orientation][type][2] += area3 - area2;
				}
				else
				{
					throw new RuntimeException();
				}
			}
		}

//		if (debug && "�".equals(aSymbol.mCharacter))
//		{
//			//System.out.print((aSymbol.mCharacter==null?" ":aSymbol.mCharacter)+" ");
//
//String [][][] v = {
//{{" 0"," 6"},{"15"," 0"},{" 0","14"}},
//{{" 2"," 3"},{"20"," 0"},{" 6"," 7"}},
//{{" 6"," 0"},{"25"," 0"},{"23"," 9"}},
//{{" 7"," 0"},{"14"," 0"},{" 0","12"}},
//{{" 0"," 0"},{" 0","10"},{"10"," 0"}},
//{{" 0"," 0"},{" 0","12"},{"12"," 0"}},
//{{" 0","12"},{" 0","12"},{"14"," 0"}},
//{{" 0"," 0"},{" 0"," 5"},{" 4"," 0"}}};
//
//
//			String [] names = {"left out ","right out","up out   ","down out ","left in  ","right in ","up in    ","down in  "};
//
//			for (int orientation = 0; orientation < 8; orientation++)
//			{
//				System.out.println(names[orientation]);
//				for (int zone = 0; zone < 3; zone++)
//				{
//					for (int type = 0; type < 2; type++)
//					{
//						int s = (int)aSymbol.mCurvatureVector[orientation][type==0?1:0][zone];
//						System.out.println((type==0?" 1":"-1")+","+(1+orientation)+","+(1+zone)+"="+(s<10?" ":"")+s+"\t"+v[orientation][zone][type]);
//					}
//				}
//			}
//
//			System.exit(0);
//		}
	}


	public void learnSymbol(String aFontName, TextBox aTextBox)
	{
		boolean debug = OCREngine.isDebugEnabled(mPage);

		Symbol symbol = new Symbol(aTextBox);

		int charIndex = 13 * (aTextBox.y / 69) + (aTextBox.x / 71);

		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@+'/\\\"*.-:,&()=�";
		String character = " ";

		if (charIndex < alphabet.length())
		{
			character = "" + alphabet.charAt(charIndex);
		}

		symbol.mFontName = aFontName;
		symbol.mCharacter = character;

		if (debug)
		{
			System.out.println("");
			System.out.println(character + " - " + aTextBox);
			System.out.println("");
		}

		extractBitmap(symbol);

		if (new Page(symbol.mBitmap).getRectFillFactor(0, 0, MATRIX_SIZE, MATRIX_SIZE) == 0)
		{
			return;
		}

		extractContour(symbol);
		extractSlopes(symbol);
		extractCurvature(symbol);
		extractCurvatureVector(symbol);
		extractTemplateDistance(symbol);

		mSymbols.add(symbol);
	}


	public Result classifySymbol(TextBox aTextBox, Resolver aResolver)
	{
		boolean debug = OCREngine.isDebugEnabled(mPage);

		if (debug)
		{
			mPage.mDebugGraphics.setColor(new Color(255, 255, 0, 128));
			mPage.mDebugGraphics.draw(aTextBox);
		}

		Symbol symbol = new Symbol(aTextBox);
		extractBitmap(symbol);
		extractContour(symbol);
		extractSlopes(symbol);
		extractCurvature(symbol);
		extractCurvatureVector(symbol);
		extractTemplateDistance(symbol);

		ArrayList<Result> results1 = classifySymbolByCurvature(symbol, aTextBox, aResolver);
		ArrayList<Result> results2 = classifySymbolByTemplate(symbol, aTextBox, aResolver);
		ArrayList<Result> results3 = classifySymbolByContour(symbol, aTextBox, aResolver);

		if (results1.isEmpty() || results2.isEmpty() || results3.isEmpty())
		{
			return null;
		}

		Result r1 = results1.get(0);
		Result r2 = results2.get(0);
		Result r3 = results3.get(0);

		for (int i = 1; i < results1.size(); i++)
		{
			if (results1.get(i).mScore > r1.mScore)
			{
				r1 = results1.get(i);
			}
			if (results2.get(i).mScore > r2.mScore)
			{
				r2 = results2.get(i);
			}
			if (results3.get(i).mScore > r3.mScore)
			{
				r3 = results3.get(i);
			}
		}

		Result result;
		boolean guessed = false;

		if (r1.compare(r2) && r1.compare(r3))
		{
			result = new Result((r1.mScore + r2.mScore + r3.mScore) / 3, r1.mSymbol);
		}
		else if (r1.compare(r2))
		{
			result = new Result((r1.mScore + r2.mScore) / 2, r1.mSymbol);
		}
		else if (r1.compare(r3))
		{
			result = new Result((r1.mScore + r3.mScore) / 2, r1.mSymbol);
		}
		else if (r2.compare(r3))
		{
			result = new Result((r2.mScore + r3.mScore) / 2, r2.mSymbol);
		}
		else
		{
			guessed = true;
			result = new Result(0, null);

			for (int i = 0; i < results1.size(); i++)
			{
				double sa = results1.get(i).mScore;
				double sb = results2.get(i).mScore;
				double sc = results3.get(i).mScore;

				double avg = (sa + sb + sc) / 3.0;

				if (!results1.get(i).mSymbol.mCharacter.equals(results2.get(i).mSymbol.mCharacter) || !results1.get(i).mSymbol.mCharacter.equals(results3.get(i).mSymbol.mCharacter))
				{
					throw new RuntimeException();
				}

				if (avg > result.mScore)
				{
					result = new Result(avg, results1.get(i).mSymbol);
				}
			}
		}

		if (debug)
		{
			mPage.mDebugGraphics.setFont(new Font("arial", Font.PLAIN, 10));
			mPage.mDebugGraphics.setColor(guessed && result.mSymbol.equals(r1.mSymbol) ? new Color(0, 255, 0, 128) : result.mSymbol.equals(r1.mSymbol) ? new Color(0, 0, 0) : new Color(255, 0, 0, 128));
			mPage.mDebugGraphics.drawString("" + r1.mSymbol, aTextBox.x, aTextBox.y - 23);
			mPage.mDebugGraphics.setColor(guessed && result.mSymbol.equals(r2.mSymbol) ? new Color(0, 255, 0, 128) : result.mSymbol.equals(r2.mSymbol) ? new Color(0, 0, 0) : new Color(255, 0, 0, 128));
			mPage.mDebugGraphics.drawString("" + r2.mSymbol, aTextBox.x, aTextBox.y - 14);
			mPage.mDebugGraphics.setColor(guessed && result.mSymbol.equals(r3.mSymbol) ? new Color(0, 255, 0, 128) : result.mSymbol.equals(r3.mSymbol) ? new Color(0, 0, 0) : new Color(255, 0, 0, 128));
			mPage.mDebugGraphics.drawString("" + r3.mSymbol, aTextBox.x, aTextBox.y - 5);
		}

		return result;
	}


	private ArrayList<Result> classifySymbolByContour(Symbol aSymbol, TextBox box, Resolver aResolver)
	{
		boolean debug = OCREngine.isDebugEnabled(mPage);

		ArrayList<Result> results = new ArrayList<>();

		for (Symbol cmpSymbol : mSymbols)
		{
			if (!aResolver.acceptSymbol(mPage, box, cmpSymbol))
			{
				continue;
			}

			double cmpDiff = 0;

			for (int orientation = 0; orientation < 8; orientation++)
			{
				double[] symCont = aSymbol.mContour[orientation];
				double[] cmpCont = cmpSymbol.mContour[orientation];
				for (int i = 0; i < MATRIX_SIZE; i++)
				{
					cmpDiff += Math.abs(symCont[i] - cmpCont[i]);
				}
			}

			cmpDiff /= 8 * MATRIX_SIZE * MATRIX_SIZE;
			cmpDiff = 1 - cmpDiff;

			if (debug)
			{
				System.out.println(cmpSymbol.mCharacter + " = " + cmpDiff);
			}

			Result result = new Result(cmpDiff, cmpSymbol);

			results.add(result);
		}

		return results;
	}


	private ArrayList<Result> classifySymbolByTemplate(Symbol aSymbol, TextBox aTextBox, Resolver aResolver)
	{
		boolean debug = OCREngine.isDebugEnabled(mPage);

		ArrayList<Result> results = new ArrayList<>();

		for (Symbol symbol : mSymbols)
		{
			if (!aResolver.acceptSymbol(mPage, aTextBox, symbol))
			{
				continue;
			}

			double score = 0;

			for (int y = 0; y < MATRIX_SIZE; y++)
			{
				for (int x = 0; x < MATRIX_SIZE; x++)
				{
					score += Math.abs(aSymbol.mClosestPixel[y][x] - symbol.mClosestPixel[y][x]);
				}
			}

			score /= MATRIX_SIZE * MATRIX_SIZE * MATRIX_SIZE;
			score = 1 - score;

//			if (aTextBox.x == 1283 && aTextBox.y == 868)
//			{
//				System.out.println(symbol.mCharacter + " " + aTextBox.getIndex() + " " + score);
//
//				if (symbol.mCharacter.equals("0") || symbol.mCharacter.equals("C"))
//				{
//					for (int y = 0; y < MATRIX_SIZE; y++)
//					{
//						for (int x = 0; x < MATRIX_SIZE; x++)
//						{
//							System.out.printf("%3d ", symbol.mClosestPixel[y][x]);
//						}
//						System.out.println();
//					}
//					System.out.println();
//					for (int y = 0; y < MATRIX_SIZE; y++)
//					{
//						for (int x = 0; x < MATRIX_SIZE; x++)
//						{
//							System.out.printf("%3d ", aSymbol.mClosestPixel[y][x]);
//						}
//						System.out.println();
//					}
//					System.out.println();
//				}
//			}

			if (debug)
			{
				System.out.println(symbol.mCharacter + " = " + score);
			}

			results.add(new Result(score, symbol));
		}

		return results;
	}


	private int findClosestPixel(Symbol aSymbol, int x, int y)
	{
		int T = 16;

		for (int s = 0; s < MATRIX_SIZE; s++)
		{
			for (int i = -s; i <= s; i++)
			{
				if (aSymbol.getGray(x + i, y - s) < T)
				{
					return s;
				}
				if (aSymbol.getGray(x + i, y + s) < T)
				{
					return s;
				}
				if (aSymbol.getGray(x - s, y + i) < T)
				{
					return s;
				}
				if (aSymbol.getGray(x + s, y + i) < T)
				{
					return s;
				}
			}
		}
		return MATRIX_SIZE;
	}


	private ArrayList<Result> classifySymbolByCurvature(Symbol symbol, TextBox box, Resolver aResolver)
	{
		boolean debug = OCREngine.isDebugEnabled(mPage);

		ArrayList<Result> results = new ArrayList<>();

		if (debug)
		{
			System.out.println();
			System.out.print("    ");
			for (int orientation = 0; orientation < 8; orientation++)
			{
				System.out.print("[");
				for (int zone = 0; zone < 3; zone++)
				{
					for (int type = 0; type < 2; type++)
					{
						int v = (int)Math.round(symbol.mCurvatureVector[orientation][type][zone]);
						System.out.print((v < 10 ? " " : "") + v + " ");
					}
				}
				System.out.print("]");
			}

			Symbol sym0 = null;
			Symbol sym1 = null;

			for (Symbol cmpSymbol : mSymbols)
			{
				if (cmpSymbol.mCharacter.equals("a"))
				{
					sym0 = cmpSymbol;
				}
				if (cmpSymbol.mCharacter.equals("s"))
				{
					sym1 = cmpSymbol;
				}
			}

			System.out.println();
			for (Symbol cmpSymbol : new Symbol[]
			{
				sym0, sym1
			})
			{
				System.out.println("REAL");
				System.out.print(cmpSymbol.mCharacter + " = ");
				for (int orientation = 0; orientation < 8; orientation++)
				{
					System.out.print("[");
					for (int zone = 0; zone < 3; zone++)
					{
						for (int type = 0; type < 2; type++)
						{
							double cmp = Math.round(cmpSymbol.mCurvatureVector[orientation][type][zone]);
							System.out.print((cmp < 10 ? " " : "") + (int)cmp + " ");
						}
					}
					System.out.print("]");
				}
				System.out.println();
			}

			System.out.println();
		}

		for (Symbol cmpSymbol : mSymbols)
		{
			if (!aResolver.acceptSymbol(mPage, box, cmpSymbol))
			{
				continue;
			}

			double cmpTotal = 0;

			if (debug)
			{
				System.out.print(cmpSymbol.mCharacter + " = ");
			}

			for (int orientation = 0; orientation < 8; orientation++)
			{
				if (debug)
				{
					System.out.print("[");
				}
				for (int zone = 0; zone < 3; zone++)
				{
					for (int type = 0; type < 2; type++)
					{
						if (debug)
						{
							int v = (int)cmpSymbol.mCurvatureVector[orientation][type][zone];
						}

						double s1 = cmpSymbol.mCurvatureVector[orientation][type][zone];
						double s2 = symbol.mCurvatureVector[orientation][type][zone];

						double d = Math.abs(s1 - s2);

						cmpTotal += d;

						if (debug)
						{
							System.out.print((Math.round(d) < 10 ? " " : "") + (int)Math.round(d) + " ");
						}
					}
				}
				if (debug)
				{
					System.out.print("]");
				}
			}

			cmpTotal /= 8 * MATRIX_SIZE * MATRIX_SIZE;
			cmpTotal = 1 - cmpTotal;

			if (debug)
			{
				System.out.println(" = " + cmpTotal);
			}

			Result result = new Result(cmpTotal, cmpSymbol);

			results.add(result);
		}

		return results;
	}
}
