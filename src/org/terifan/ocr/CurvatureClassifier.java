package org.terifan.ocr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;


class CurvatureClassifier
{
	private final static int WHITE_THRESHOLD = 160;
	private final static int MATRIX_SIZE = 9;
	private final static double ONE_THIRD_MATRIX = MATRIX_SIZE / 3.0;
	public final static String DEFAULT_ALPHABET =
		  "ABCDEFGHIJKLM" + "NOPQRSTUVWXYZ"
		+ "abcdefghijklm" + "nopqrstuvwxyz"
		+ "0123456789@+'" + "/\\\"*.-:,&()=*";

	private ArrayList<Symbol> mSymbols;
	private Page mPage;


	public CurvatureClassifier()
	{
		reset();
	}


	public ArrayList<Symbol> getSymbols()
	{
		return mSymbols;
	}


	public void learn(String aFontName, Bitmap aBitmap, String aAlphabet)
	{
		if (aAlphabet == null)
		{
			aAlphabet = DEFAULT_ALPHABET;
		}
		if (aAlphabet.length() != DEFAULT_ALPHABET.length())
		{
			throw new IllegalArgumentException("Alphabet must contain " + aAlphabet.length() + " characters");
		}

		int GW = 71;
		int GH = 69;

		ArrayList<TextBox> textBoxes = new ArrayList<>();
		for (int y = 0; y < 6; y++)
		{
			for (int x = 0; x < 13; x++)
			{
				textBoxes.add(new TextBox(new Rectangle(GW * x + 1, GH * y + 1, GW - 2, GH - 2)));
			}
		}

		for (TextBox box : textBoxes)
		{
			Symbol symbol = learnSymbol(aBitmap, aFontName, box, DEFAULT_ALPHABET, aAlphabet);

			if (symbol != null)
			{
				generateCurvatureBitmap(symbol, box);

				symbol.setSymbolBitmap(box.getSymbolBitmap());
			}
		}
	}


	public void reset()
	{
		mSymbols = new ArrayList<>();
	}


	private void extractBitmap(Bitmap aBitmap, Symbol aSymbol)
	{
		TextBox box = aSymbol.mTextBox;

		Insets borders = aBitmap.getBorders(box.x, box.y, box.width, box.height);

		aSymbol.mBorders = borders;

		BufferedImage tmp = aBitmap.getRegion(box.x + borders.left, box.y + borders.top, box.x + box.width - borders.right + 1, box.y + box.height - borders.bottom + 1);

		box.setBitmap(tmp);

		tmp = ImageTools.resize(tmp, MATRIX_SIZE, MATRIX_SIZE, RenderingHints.VALUE_INTERPOLATION_BILINEAR, BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < MATRIX_SIZE; y++)
		{
			for (int x = 0; x < MATRIX_SIZE; x++)
			{
				int c = tmp.getRGB(x, y);
				c = ((255 & (c >> 16)) + (255 & (c >> 8)) + (255 & c)) / 3;
				tmp.setRGB(x, y, c > WHITE_THRESHOLD ? 0xffffff : 0x000000);
			}
		}

		aSymbol.setBitmap(new Bitmap(tmp));
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
		Bitmap image = aSymbol.getBitmap();

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
					if (image.isBlack(x, y))
					{
						break;
					}
				}
				int z = (int)Math.round(y * fy);
				contour[4 * ori + 0][z] += fx * x;
				count[4 * ori + 0][z] += 1;
			}

			for (int y = 0; y < h; y++)
			{
				int x = w - 1 - w * ori / 2;
				for (; x >= 0; x--)
				{
					if (image.isBlack(x, y))
					{
						break;
					}
				}
				int z = (int)Math.round(y * fy);
				contour[4 * ori + 1][z] += fx * x;
				count[4 * ori + 1][z] += 1;
			}

			for (int x = 0; x < w; x++)
			{
				int y = h * ori / 2;
				for (; y < h; y++)
				{
					if (image.isBlack(x, y))
					{
						break;
					}
				}
				int z = (int)Math.round(x * fx);
				contour[4 * ori + 2][z] += fy * y;
				count[4 * ori + 2][z] += 1;
			}

			for (int x = 0; x < w; x++)
			{
				int y = h - 1 - h * ori / 2;
				for (; y >= 0; y--)
				{
					if (image.isBlack(x, y))
					{
						break;
					}
				}
				int z = (int)Math.round(x * fx);
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

		aSymbol.mContour = contour;
	}


	private void extractSlopes(Symbol aSymbol)
	{
		int[][] slopes = new int[8][MATRIX_SIZE];
		int[][] slopes2 = new int[8][MATRIX_SIZE];
		String[][] slopesX = new String[8][MATRIX_SIZE];
		double[][] contour = aSymbol.mContour;

		for (int orientation = 0; orientation < 8; orientation++)
		{
			for (int index = 0; index < MATRIX_SIZE; index++)
			{
				int b =                                (int)(contour[orientation][index    ]);
				int a = index == 0               ? b : (int)(contour[orientation][index - 1]);
				int c = index == MATRIX_SIZE - 1 ? b : (int)(contour[orientation][index + 1]);

				if (a == -1) a = MATRIX_SIZE;
				if (b == -1) b = MATRIX_SIZE;
				if (c == -1) c = MATRIX_SIZE;

				slopesX[orientation][index] = a+":"+b+":"+c;

				int s;
				int t;

				if (b == MATRIX_SIZE)
				{
					s = -1;
					t = 0;
				}
				else if (a == b && b == c)
				{
					s = 0;
					t = 1;
				}
				else if (a == MATRIX_SIZE && b == c)
				{
					s = 0;
					t = 2;
				}
				else if (a == b && c == MATRIX_SIZE)
				{
					s = 0;
					t = 3;
				}
				else if (a < b && c < b)
				{
					s = 2;
					t = 4;
				}
				else if (a > b && c > b)
				{
					s = 0;
					t = 5;
				}
				else if (a > b && c <= b)
				{
					s = 1;
					t = 6;
				}
				else if (a >= b && c < b)
				{
					s = 1;
					t = 7;
				}
				else if (a == MATRIX_SIZE && c < b)
				{
					s = 1;
					t = 8;
				}
				else if (a > b && c == MATRIX_SIZE)
				{
					s = 1;
					t = 9;
				}
				else if (a < b && c >= b)
				{
					s = 2;
					t = 10;
				}
				else if (a <= b && c > b)
				{
					s = 2;
					t = 11;
				}
				else if (a == MATRIX_SIZE && c > b)
				{
					s = 2;
					t = 12;
				}
				else if (a < b && c == MATRIX_SIZE)
				{
					s = 3;
					t = 13;
				}
				else if (a == MATRIX_SIZE && b == MATRIX_SIZE && c == MATRIX_SIZE)
				{
					s = -1;
					t = 14;
				}
				else
				{
					s = -1;
					t = 15;
				}

				slopes[orientation][index] = s;
				slopes2[orientation][index] = t;
			}
		}

		aSymbol.mSlopes = slopes;
		aSymbol.mSlopes2 = slopes2;
		aSymbol.mSlopesX = slopesX;
	}


	private void extractCurvature(Symbol aSymbol)
	{
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

//				if (!first && tx > -1 && tx < MATRIX_SIZE && (fromX != toX || Math.abs(fromY - toY) > 0) && (fromY != toY || Math.abs(fromX - toX) > 0))
				if (!first && tx > -1 && tx < MATRIX_SIZE && (fromX != toX || fromY != toY))
				{
					int x1 = fromX + (orientation % 4) * MATRIX_SIZE;
					int y1 = fromY + (orientation / 4) * MATRIX_SIZE;
					int x2 = toX + (orientation % 4) * MATRIX_SIZE;
					int y2 = toY + (orientation / 4) * MATRIX_SIZE;

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
				}

				if (orientation == 0 && aSymbol.mTextBox.x == 476 && aSymbol.mTextBox.y == 2405)
				{
					System.out.println(fromX+","+fromY+" -> "+toX+","+toY);
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
	}


	private static void generateCurvatureBitmap(Symbol aSymbol, TextBox aTextBox)
	{
		int scale = 8;
		int padLeft = 40;
		int padRight = 10;
		int padBottom = 10;
		int padX = 32;
		int padY = 64;
		int size = MATRIX_SIZE * scale;

		BufferedImage output = new BufferedImage(padLeft + 1 * (size + padX) + padRight, 8 * (size + padY) + padBottom, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = output.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, output.getWidth(), output.getHeight());
		g.setFont(new Font("arial",Font.PLAIN,8));

		for (int orientation = 0; orientation < 8; orientation++)
		{
			int x = orientation / 8;
			int y = orientation % 8;

			int ox = padX + x * (padX + size) + padLeft;
			int oy = padY + y * (padY + size);

			g.drawImage(aSymbol.getBitmap().getImage(), ox, oy, size, size, null);

			g.setColor(new Color(255, 255, 255, 240));

			switch (orientation)
			{
				case 4:
					g.fillRect(ox, oy, size / 2, size);
					break;
				case 5:
					g.fillRect(ox + size / 2, oy, size / 2, size);
					break;
				case 6:
					g.fillRect(ox, oy, size, size / 2);
					break;
				case 7:
					g.fillRect(ox, oy + size / 2, size, size / 2);
					break;
			}

			g.setColor(new Color(255, 255, 255, 64));

			for (int i = 0; i <= MATRIX_SIZE; i++)
			{
				g.drawLine(ox + i * scale, oy, ox + i * scale, oy + size);
				g.drawLine(ox, oy + i * scale, ox + size, oy + i * scale);
			}
		}

		g.setStroke(new BasicStroke(3));
		g.setColor(new Color(0, 0, 0, 16));
		for (int orientation = 0; orientation < 8; orientation++)
		{
			int x = orientation / 8;
			int y = orientation % 8;

			for (int i = 1; i < 3; i++)
			{
				int ox = padX + x * padX + x * size + padLeft;
				int oy = padY + y * padY + y * size;

				if (orientation > 3)
				{
					g.drawLine(ox + i * size / 3, oy, ox + i * size / 3, oy + size);
				}
				else
				{
					g.drawLine(ox, oy + i * size / 3, ox + size, oy + i * size / 3);
				}
			}
		}

		double[][] contour = aSymbol.mContour;
		int[][] slopes = aSymbol.mSlopes;

		for (int orientation = 0; orientation < 8; orientation++)
		{
			int x = orientation / 8;
			int y = orientation % 8;
			boolean hor = (orientation == 2 || orientation == 3 || orientation == 6 || orientation == 7);

			int tmp = 0;
			int fromX = 0;
			int fromY = 0;
			boolean first = true;

			for (int i = 0; i < MATRIX_SIZE; i++)
			{
				if (first)
				{
					tmp = (int)contour[orientation][i];
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
						tmp = (int)contour[orientation][i];
					}
				}

				if (tmp == -1 || tmp == MATRIX_SIZE)
				{
					first = true;
					continue;
				}

				int toX, toY;

				if (hor)
				{
					toX = i - (first ? 0 : 1);
					toY = tmp;
				}
				else
				{
					toX = tmp;
					toY = i - (first ? 0 : 1);
				}

				if (!first && tmp > -1 && tmp < MATRIX_SIZE && (fromX != toX || fromY != toY))
				{
					int x1 = (int)(padX + scale * fromX) + x * (padX + scale * MATRIX_SIZE) + scale / 2 + padLeft;
					int y1 = (int)(padY + scale * fromY) + y * (padY + scale * MATRIX_SIZE) + scale / 2;
					int x2 = (int)(padX + scale * toX) + x * (padX + scale * MATRIX_SIZE) + scale / 2 + padLeft;
					int y2 = (int)(padY + scale * toY) + y * (padY + scale * MATRIX_SIZE) + scale / 2;

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

					int[][] points = slope == 1 ? new int[][]{{x1, x2, x1},{y1, y2, y2}} : new int[][]{{x1, x2, x2},{y1, y2, y1}};

					Color c = Color.getHSBColor((hor ? (slope == 1 ? -1 : 1) : slope) == 1 ? 0f : 0.5f, 1, 1);
					g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 128));
					g.fillPolygon(points[0], points[1], 3);
					g.setColor(c);
					g.drawLine(x1, y1, x2, y2);
				}

				fromX = toX;
				fromY = toY;

				first = false;

				if (i < MATRIX_SIZE && contour[orientation][i] == -1)
				{
					first = true;
				}
			}

			g.setColor(Color.BLACK);
			for (int zone = 0; zone < 3; zone++)
			{
				for (int type = 0; type < 2; type++)
				{
					String text = "" + (int)aSymbol.mCurvatureVector[orientation][type][zone];
					int tx = padX + x * (padX + size) + 25 * type + padLeft;
					int ty = padY + y * (padY + size) + 11 * zone - 40;
					g.drawString(text, tx, ty);
				}
			}
		}

		g.drawString("" + aTextBox.x + ", " + aTextBox.y, 0, 10);
		g.setColor(Color.GREEN);

		for (int orientation = 0; orientation < 8; orientation++)
		{
			int x = orientation / 8;
			int y = orientation % 8;

			for (int i = 0; i < MATRIX_SIZE; i++)
			{
				String text = aSymbol.mSlopesX[orientation][i]+" "+slopes[orientation][i]+" "+aSymbol.mSlopes2[orientation][i]+" "+(int)aSymbol.mContour[orientation][i];
				int tx = padX + x * (padX + size) - 30;
				int ty = padY + y * (padY + size) + i * scale + scale;
				g.drawString(text, tx, ty);
			}
		}

		aTextBox.setSymbolBitmap(output);
	}


	private void extractCurvatureVector(Symbol aSymbol)
	{
		Polygon[][] polygons = aSymbol.mCurvature;

		double[][][] fill = aSymbol.mCurvatureVector = new double[8][2][3];

		for (int orientation = 0; orientation < 8; orientation++)
		{
			boolean hor = orientation == 0 || orientation == 1 || orientation == 4 || orientation == 5;

			for (int i = 0; i < polygons[orientation].length; i++)
			{
				Polygon p = polygons[orientation][i];

				int type = aSymbol.mCurvatureSlopes[orientation][i] == -1 ? 0 : 1;

				double area1, area2, area3;

				int v0 = (int)(0 * ONE_THIRD_MATRIX);
				int v1 = (int)(1 * ONE_THIRD_MATRIX);
				int v2 = (int)(2 * ONE_THIRD_MATRIX);
				int v3 = (int)(3 * ONE_THIRD_MATRIX);

				if (hor)
				{
					area1 = intersect(p, 0, v0, MATRIX_SIZE, v1);
					area2 = intersect(p, 0, v1, MATRIX_SIZE, v2);
					area3 = intersect(p, 0, v2, MATRIX_SIZE, v3);
				}
				else
				{
					area1 = intersect(p, v0, 0, v1, MATRIX_SIZE);
					area2 = intersect(p, v1, 0, v2, MATRIX_SIZE);
					area3 = intersect(p, v2, 0, v3, MATRIX_SIZE);
				}

				fill[orientation][type][0] += area1;
				fill[orientation][type][1] += area2;
				fill[orientation][type][2] += area3;
			}
		}
	}


	private int intersect(Polygon p, int rx0, int ry0, int rx1, int ry1)
	{
		int n = 0;

		if (rx0 > rx1){int t = rx1; rx1 = rx0; rx0 = t;}
		if (ry0 > ry1){int t = ry1; ry1 = ry0; ry0 = t;}

		for (double y = ry0; y < ry1; y++)
		{
			for (double x = rx0; x < rx1; x++)
			{
				if (p.contains(x, y))
				{
					n++;
				}
			}
		}

		return n;
	}


	private Symbol learnSymbol(Bitmap aBitmap, String aFontName, TextBox aTextBox, String aDefaultAlphabet, String aAlphabet)
	{
		Symbol symbol = new Symbol(aTextBox);

		int charIndex = 13 * (aTextBox.y / 69) + (aTextBox.x / 71);

		String character = " ";
		String defCharacter = " ";

		if (charIndex < aAlphabet.length())
		{
			character = "" + aAlphabet.charAt(charIndex);
			defCharacter = "" + aDefaultAlphabet.charAt(charIndex);
		}

		symbol.mFontName = aFontName;
		symbol.mCharacter = character;
		symbol.mDefCharacter = defCharacter;

//		if (debug)
//		{
//			System.out.println("");
//			System.out.println(character + " - " + aTextBox);
//			System.out.println("");
//		}

		extractBitmap(aBitmap, symbol);

		if (symbol.getBitmap().getRectFillFactor(0, 0, MATRIX_SIZE, MATRIX_SIZE) == 0)
		{
			return null;
		}

		extractContour(symbol);
		extractSlopes(symbol);
		extractCurvature(symbol);
		extractCurvatureVector(symbol);
		extractTemplateDistance(symbol);

		mSymbols.add(symbol);

		return symbol;
	}


	public Result classifySymbol(Page aPage, TextBox aTextBox, Resolver aResolver)
	{
		mPage = aPage;

		Symbol symbol = new Symbol(aTextBox);
		extractBitmap(aPage.getBitmap(), symbol);
		extractContour(symbol);
		extractSlopes(symbol);
		extractCurvature(symbol);
		extractCurvatureVector(symbol);
		extractTemplateDistance(symbol);

		generateCurvatureBitmap(symbol, aTextBox);

		ArrayList<Result> results = classifySymbolByCurvature(symbol, aTextBox, aResolver);

		Collections.sort(results);

		return results.get(0);
	}


//	private ArrayList<Result> classifySymbolByContour(Symbol aSymbol, TextBox aTextBox, Resolver aResolver)
//	{
//		ArrayList<Result> results = new ArrayList<>();
//
//		for (Symbol cmpSymbol : mSymbols)
//		{
//			if (!aResolver.acceptSymbol(mPage, aTextBox, cmpSymbol))
//			{
//				continue;
//			}
//
//			double cmpDiff = 0;
//
//			for (int orientation = 0; orientation < 8; orientation++)
//			{
//				double[] symCont = aSymbol.mContour[orientation];
//				double[] cmpCont = cmpSymbol.mContour[orientation];
//				for (int i = 0; i < MATRIX_SIZE; i++)
//				{
//					cmpDiff += Math.abs(symCont[i] - cmpCont[i]);
//				}
//			}
//
//			cmpDiff /= 8 * MATRIX_SIZE * MATRIX_SIZE;
//			cmpDiff = 1 - cmpDiff;
//
////			if (debug)
////			{
////				System.out.println(cmpSymbol.mCharacter + " = " + cmpDiff);
////			}
//
//			Result result = new Result(cmpDiff, cmpSymbol);
//
//			results.add(result);
//		}
//
//		return results;
//	}
//
//
//	private ArrayList<Result> classifySymbolByTemplate(Symbol aSymbol, TextBox aTextBox, Resolver aResolver)
//	{
//		ArrayList<Result> results = new ArrayList<>();
//
//		for (Symbol symbol : mSymbols)
//		{
//			if (!aResolver.acceptSymbol(mPage, aTextBox, symbol))
//			{
//				continue;
//			}
//
//			double score = 0;
//
//			for (int y = 0; y < MATRIX_SIZE; y++)
//			{
//				for (int x = 0; x < MATRIX_SIZE; x++)
//				{
//					score += Math.abs(aSymbol.mClosestPixel[y][x] - symbol.mClosestPixel[y][x]);
//				}
//			}
//
//			score /= MATRIX_SIZE * MATRIX_SIZE * MATRIX_SIZE;
//			score = 1 - score;
//
////			if (debug)
////			{
////				System.out.println(symbol.mCharacter + " = " + score);
////			}
//
//			results.add(new Result(score, symbol));
//		}
//
//		return results;
//	}


	private int findClosestPixel(Symbol aSymbol, int x, int y)
	{
		Bitmap bitmap = aSymbol.getBitmap();

		for (int s = 0; s < MATRIX_SIZE; s++)
		{
			for (int i = -s; i <= s; i++)
			{
				if (bitmap.isBlack(x + i, y - s, false))
				{
					return s;
				}
				if (bitmap.isBlack(x + i, y + s, false))
				{
					return s;
				}
				if (bitmap.isBlack(x - s, y + i, false))
				{
					return s;
				}
				if (bitmap.isBlack(x + s, y + i, false))
				{
					return s;
				}
			}
		}
		return MATRIX_SIZE;
	}


	private ArrayList<Result> classifySymbolByCurvature(Symbol aSymbol, TextBox aTextBox, Resolver aResolver)
	{
		ArrayList<Result> results = new ArrayList<>();

		int[] points = new int[mSymbols.size()];

		for (int orientation = 0; orientation < 8; orientation++)
		{
			for (int zone = 0; zone < 3; zone++)
			{
				for (int type = 0; type < 2; type++)
				{
					for (int sym = 0; sym < mSymbols.size(); sym++)
					{
						Symbol cmpSymbol = mSymbols.get(sym);

						if (!aResolver.acceptSymbol(mPage, aTextBox, cmpSymbol))
						{
							continue;
						}

						double s1 = cmpSymbol.mCurvatureVector[orientation][type][zone];
						double s2 = aSymbol.mCurvatureVector[orientation][type][zone];

						points[sym] += Math.abs(s1 - s2);
					}
				}
			}
		}

		Symbol cmpSymbol = null;
		int smallest = Integer.MAX_VALUE;

		for (int sym = 0; sym < mSymbols.size(); sym++)
		{
			if (points[sym] < smallest)
			{
				cmpSymbol = mSymbols.get(sym);
				smallest = points[sym];
			}
		}

		Result result = new Result(0, cmpSymbol);

		results.add(result);

//		for (Symbol cmpSymbol : mSymbols)
//		{
//			if (!aResolver.acceptSymbol(mPage, aTextBox, cmpSymbol))
//			{
//				continue;
//			}
//
//			double cmpTotal = 0;
//
//			for (int orientation = 0; orientation < 8; orientation++)
//			{
//				for (int zone = 0; zone < 3; zone++)
//				{
//					for (int type = 0; type < 2; type++)
//					{
//						double s1 = cmpSymbol.mCurvatureVector[orientation][type][zone];
//						double s2 = aSymbol.mCurvatureVector[orientation][type][zone];
//
//						double d = Math.pow(Math.abs(s1 - s2), 2);
//
//						cmpTotal += d;
//					}
//				}
//			}
//
//			Result result = new Result(cmpTotal, cmpSymbol);
//
//			results.add(result);
//		}

		return results;
	}
}
