package org.terifan.ocr.application;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


class ImagePane extends JPanel
{
	private static Cursor OPEN_HAND_CURSOR;
	private static Cursor CLOSED_HAND_CURSOR;

	private BufferedImage mImage;
	private int mStartX, mStartY;
	private int mOffsetX, mOffsetY;
	private int mOldOffsetX, mOldOffsetY;
	private int mImageWidth, mImageHeight;
	private Dimension mPreferredSize;
	private double mScale;
	private boolean mDoScaleToFit;
	private boolean mDoScaleToFillView;
	private boolean mDoCenterOnMouse;
	private boolean mMouseButtonPressed;
	private boolean mScaleChanged;
	private BufferedImage mScaledImage;
	private Rectangle mRectangle;
	private Point mSelectionStartOffset;
	private ImageFilter mImageFilter;
	private InterpreterTool mInterpreterTool;
	private Overlay mViewOverlay;
	private Overlay mImageOverlay;


	static
	{
		if (OPEN_HAND_CURSOR == null)
		{
			OPEN_HAND_CURSOR = Toolkit.getDefaultToolkit().createCustomCursor(new ImageIcon(ImagePane.class.getResource("cursor_open_hand.gif").getPath()).getImage(), new Point(16, 16), "open_hand");
			CLOSED_HAND_CURSOR = Toolkit.getDefaultToolkit().createCustomCursor(new ImageIcon(ImagePane.class.getResource("cursor_closed_hand.gif").getPath()).getImage(), new Point(16, 16), "closed_hand");
		}
	}


	public ImagePane(BufferedImage aImage) throws IOException
	{
		super(new BorderLayout());

		addMouseListener(new MouseListener());
		addMouseMotionListener(new MouseMotionListener());
		addComponentListener(new ComponentListener());
		addMouseWheelListener(new MouseWheelListener());

		setScale(1);
		setImage(aImage);

		setBackground(new Color(240, 240, 240));
		setForeground(Color.BLACK);
		setFocusable(true);
		requestFocusInWindow();
	}


	void setScale(double aScale)
	{
		mScaleChanged = true;
		mScale = Math.max(Math.min(aScale, 5), 0.01);
	}


	void scaleToFit()
	{
		mDoScaleToFit = true;
	}


	void scaleToFillView()
	{
		mDoScaleToFillView = true;
	}


	public void setViewOverlay(Overlay aViewOverlay)
	{
		mViewOverlay = aViewOverlay;
	}


	public void setImageOverlay(Overlay aImageOverlay)
	{
		mImageOverlay = aImageOverlay;
	}



	@Override
	public Dimension getPreferredSize()
	{
		return mPreferredSize;
	}


	public void setImage(BufferedImage aImage)
	{
		setImage(aImage, true);
	}


	public void setImage(BufferedImage aImage, boolean aResetView)
	{
		mImage = aImage;
		mScaledImage = null;
		mScaleChanged = true;

		if (mImage == null)
		{
			mImageWidth = 0;
			mImageHeight = 0;
		}
		else
		{
			mImageWidth = mImage.getWidth();
			mImageHeight = mImage.getHeight();
		}

		if (aResetView)
		{
			mPreferredSize = new Dimension(mImageWidth, mImageHeight);

			mOffsetX = 0;
			mOffsetY = 0;

			scaleToFit();
			updateCursor();
		}
		validateImageOffset();
		repaint();
	}


	public void updateCursor()
	{
		if (mSelectionStartOffset != null && mInterpreterTool != null)
		{
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		}
		else if (mMouseButtonPressed && mImage != null)
		{
			setCursor(mMouseButtonPressed ? CLOSED_HAND_CURSOR : OPEN_HAND_CURSOR);
		}
		else
		{
			setCursor(OPEN_HAND_CURSOR);
		}
	}


	@Override
	public void paintComponent(Graphics aGraphics)
	{
		Graphics2D g = (Graphics2D)aGraphics;

		drawBackground(g);

		int fw = getWidth();
		int fh = getHeight();

		if (mImageFilter != null && mScaleChanged)
		{
			mImageFilter.mAbort = true;
		}

		if (mDoScaleToFit)
		{
			setScale(Math.min(fw / (double) mImageWidth, fh / (double) mImageHeight));
			mDoScaleToFit = false;
			mOffsetX = 0;
			mOffsetY = 0;
		}
		if (mDoScaleToFillView)
		{
			setScale(Math.max(fw / (double) mImageWidth, fh / (double) mImageHeight));
			mDoScaleToFillView = false;

			if (mDoCenterOnMouse)
			{
				mDoCenterOnMouse = false;
			}
			else
			{
				mOffsetX = 0;
				mOffsetY = 0;
			}
		}

		validateImageOffset();

		if (mImage != null)
		{
			int w = (int) (mScale * mImageWidth);
			int h = (int) (mScale * mImageHeight);

			int x = (int) (0.5 * fw - 0.5 * w + mOffsetX);
			int y = (int) (0.5 * fh - 0.5 * h + mOffsetY);

			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

			g.drawImage(mScaledImage == null || mScale > 1 ? mImage : mScaledImage, x, y, w, h, null);

			if (mRectangle != null && mInterpreterTool != null)
			{
				g.setColor(new Color(0, 64, 255, 128));
				g.fillRect(mRectangle.x, mRectangle.y, mRectangle.width, mRectangle.height);
				g.setColor(Color.BLUE);
				g.drawRect(mRectangle.x, mRectangle.y, mRectangle.width, mRectangle.height);
			}

			if (mScale < 1 && mScaleChanged)
			{
				mImageFilter = new ImageFilter(w, h);
				mImageFilter.start();
			}

			if (mImageOverlay != null)
			{
				AffineTransform transform = g.getTransform();
				AffineTransform at = new AffineTransform();
				at.translate(x, y);
				at.scale(mScale, mScale);
				g.setTransform(at);
				mImageOverlay.render(g);
				g.setTransform(transform);
			}

			if (mViewOverlay != null)
			{
				mViewOverlay.render(g);
			}
		}
	}


	private void drawBackground(Graphics aGraphics)
	{
		int w = getWidth();
		int h = getHeight();

		aGraphics.setColor(Color.WHITE);
		aGraphics.fillRect(0, 0, w, h);

		aGraphics.setColor(getBackground());
		for (int x = 0; x < w; x += 16)
		{
			for (int y = ((x>>4)&1)*16; y < h; y += 32)
			{
				aGraphics.fillRect(x, y, 16, 16);
			}
		}
	}


	class ImageFilter extends Thread
	{
		private boolean mAbort;
		private int w, h;


		public ImageFilter(int w, int h)
		{
			this.w = w;
			this.h = h;
		}


		@Override
		public void run()
		{
			mScaleChanged = false;
			BufferedImage tempImage = null;

			if (mScale < 1)
			{
				tempImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
				int[] src = ((DataBufferInt) mImage.getRaster().getDataBuffer()).getData();
				int[] dst = ((DataBufferInt) tempImage.getRaster().getDataBuffer()).getData();
				resizeDown(src, mImage.getWidth(), mImage.getHeight(), dst, w, h, this);
			}
			else
			{
				mScaledImage = mImage;
			}

			if (!mAbort)
			{
				mScaledImage = tempImage;

				repaint();
			}
		}
	}


	private static void resizeDown(int[] aSrcPixels, int aSrcWidth, int aSrcHeight, int[] aDstPixels, int aDstWidth, int aDstHeight, ImageFilter aImageFilter)
	{
		double xScale = aSrcWidth / (double) aDstWidth;
		double yScale = aSrcHeight / (double) aDstHeight;

		for (int dstY = 0, dstOffset = 0; dstY < aDstHeight && !aImageFilter.mAbort; dstY++)
		{
			for (int dstX = 0; dstX < aDstWidth; dstX++, dstOffset++)
			{
				int cnt = 0, r = 0, g = 0, b = 0;

				for (int srcY = (int) (dstY * yScale), srcMaxY = Math.min((int) Math.ceil((dstY + 1) * yScale), aSrcHeight); srcY < srcMaxY; srcY++)
				{
					for (int srcX = (int) (dstX * xScale) + srcY * aSrcWidth, srcMaxX = srcY * aSrcWidth + Math.min((int) Math.ceil((dstX + 1) * xScale), aSrcWidth); srcX < srcMaxX; srcX++)
					{
						int color = aSrcPixels[srcX];

						r += 255 & (color >> 16);
						g += 255 & (color >> 8);
						b += 255 & (color);

						cnt++;
					}
				}

				if (cnt > 0)
				{
					r /= cnt;
					g /= cnt;
					b /= cnt;

					aDstPixels[dstOffset] = (r << 16) + (g << 8) + b;
				}
			}
		}
	}


	private void validateImageOffset()
	{
		/*
		 * if (mImage != null) { updateCursor();
		 *
		 * int canvasWidth = getWidth(); int canvasHeight = getHeight(); int
		 * imageWidth = (int)(mImageWidth * mScale); int imageHeight =
		 * (int)(mImageHeight * mScale);
		 *
		 * if (canvasWidth >= imageWidth) { mOffsetX = 0; } else if (mOffsetX >
		 * (imageWidth-canvasWidth)/2) { mOffsetX = +(imageWidth-canvasWidth)/2;
		 * } else if (mOffsetX < -(imageWidth-canvasWidth)/2) { mOffsetX =
		 * -(imageWidth-canvasWidth)/2; }
		 *
		 * if (canvasHeight >= imageHeight) { mOffsetY = 0; } else if (mOffsetY
		 * > (imageHeight-canvasHeight)/2) { mOffsetY =
		 * +(imageHeight-canvasHeight)/2; } else if (mOffsetY <
		 * -(imageHeight-canvasHeight)/2) { mOffsetY =
		 * -(imageHeight-canvasHeight)/2; }
		}
		 */
	}


	private class ComponentListener extends ComponentAdapter
	{
		@Override
		public void componentResized(ComponentEvent aEvent)
		{
			validateImageOffset();
			updateCursor();
		}
	}


	private class MouseListener extends MouseAdapter
	{
		@Override
		public void mousePressed(MouseEvent aEvent)
		{
			if (SwingUtilities.isLeftMouseButton(aEvent))
			{
				if (mRectangle != null && mInterpreterTool != null)
				{
					mSelectionStartOffset = null;
					mRectangle = null;
					repaint();
				}

				mMouseButtonPressed = true;
				mStartX = mOffsetX;
				mStartY = mOffsetY;
				mOldOffsetX = aEvent.getX();
				mOldOffsetY = aEvent.getY();
				updateCursor();
			}
			else if (SwingUtilities.isMiddleMouseButton(aEvent))
			{
				mSelectionStartOffset = null;
				mRectangle = null;

				if (mOffsetX == 0 && mOffsetY == 0 && mScale == Math.min(getWidth() / (double) mImageWidth, getHeight() / (double) mImageHeight))
				{
					double newScale = Math.max(getWidth() / (double) mImageWidth, getHeight() / (double) mImageHeight);

					double px = getWidth() / 2.0 + mOffsetX - mScale * mImageWidth / 2.0;
					double py = getHeight() / 2.0 + mOffsetY - mScale * mImageHeight / 2.0;
					double pw = mScale * mImageWidth;
					double ph = mScale * mImageHeight;

					double mx = Math.min(Math.max(aEvent.getX(), px), px + pw);
					double my = Math.min(Math.max(aEvent.getY(), py), py + ph);

					int dx = (int) (mx - ImagePane.this.getWidth() / 2.0);
					int dy = (int) (my - ImagePane.this.getHeight() / 2.0);

					mOffsetX -= dx;
					mOffsetY -= dy;

					double d = newScale / mScale;
					mScale *= d;
					mOffsetX *= d;
					mOffsetY *= d;

					mOffsetX += dx;
					mOffsetY += dy;

					if (getWidth() / (double) mImageWidth > getHeight() / (double) mImageHeight)
					{
						mOffsetX = 0;
					}
					else
					{
						mOffsetY = 0;
					}

					mDoCenterOnMouse = true;

					scaleToFillView();
				}
				else
				{
					scaleToFit();
				}

				repaint();
			}
			else if (mInterpreterTool != null)
			{
				int w = (int) (mScale * mImageWidth);
				int h = (int) (mScale * mImageHeight);

				int x = (int) (0.5 * getWidth() - 0.5 * w + mOffsetX);
				int y = (int) (0.5 * getHeight() - 0.5 * h + mOffsetY);

				if (aEvent.getX() >= x && aEvent.getX() < x + w && aEvent.getY() > y && aEvent.getY() < y + h)
				{
					mSelectionStartOffset = aEvent.getPoint();
				}

				mRectangle = null;
				repaint();
				updateCursor();
			}
		}


		@Override
		public void mouseReleased(MouseEvent aEvent)
		{
			if (SwingUtilities.isLeftMouseButton(aEvent))
			{
				mMouseButtonPressed = false;
			}
			else if (mInterpreterTool != null)
			{
				if (mInterpreterTool != null && mRectangle != null)
				{
					mSelectionStartOffset = null;

					double px = getWidth() / 2.0 + mOffsetX - mScale * mImageWidth / 2.0;
					double py = getHeight() / 2.0 + mOffsetY - mScale * mImageHeight / 2.0;
					int x = (int) ((mRectangle.x - px) / mScale);
					int y = (int) ((mRectangle.y - py) / mScale);
					int w = (int) (mRectangle.width / mScale);
					int h = (int) (mRectangle.height / mScale);

					System.out.println("Running interpretation tool on " + new Rectangle(x, y, w, h));

					mInterpreterTool.performAction(new Rectangle(x, y, w, h));

					clearSelection();
					repaint();
				}
			}

			mSelectionStartOffset = null;
			mRectangle = null;

			updateCursor();

			if (mInterpreterTool != null)
			{
				mInterpreterTool.onReleaseFocus();
			}
		}
	}


	private class MouseMotionListener extends MouseMotionAdapter
	{
		@Override
		public void mouseDragged(MouseEvent aEvent)
		{
			if (mImage != null)
			{
				if (SwingUtilities.isLeftMouseButton(aEvent))
				{
					mOffsetX = mStartX + (int) ((aEvent.getX() - mOldOffsetX) * Math.max(mScale, 1));
					mOffsetY = mStartY + (int) ((aEvent.getY() - mOldOffsetY) * Math.max(mScale, 1));

					repaint();
				}
				else if (mSelectionStartOffset != null)
				{
					updateSelectionRectangle(aEvent);
				}
			}
		}
	}


	private void updateSelectionRectangle(MouseEvent aEvent)
	{
		if (mRectangle == null)
		{
			mRectangle = new Rectangle();
		}

		int w2 = (int)(mScale * mImageWidth);
		int h2 = (int)(mScale * mImageHeight);
		int x2 = (getWidth() - w2) / 2 + mOffsetX;
		int y2 = (getHeight() - h2) / 2 + mOffsetY;

		int rx = Math.max(Math.min(mSelectionStartOffset.x, aEvent.getX()), 0);
		int ry = Math.max(Math.min(mSelectionStartOffset.y, aEvent.getY()), 0);

		int w3 = Math.min(rx - x2, 0);
		int h3 = Math.min(ry - y2, 0);

		rx = Math.max(rx, x2);
		ry = Math.max(ry, y2);

		int rw = Math.min(x2 + w2 - rx, Math.abs(Math.max(aEvent.getX(), 0) - mSelectionStartOffset.x) + w3);
		int rh = Math.min(y2 + h2 - ry, Math.abs(Math.max(aEvent.getY(), 0) - mSelectionStartOffset.y) + h3);

		mRectangle.setBounds(rx, ry, rw, rh);
		repaint();
	}


	private class MouseWheelListener implements java.awt.event.MouseWheelListener
	{
		@Override
		public void mouseWheelMoved(MouseWheelEvent aEvent)
		{
			mSelectionStartOffset = null;
			mRectangle = null;
			mScaleChanged = true;

			int scroll = aEvent.getUnitsToScroll() < 0 ? -1 : 1;

			double px = getWidth() / 2.0 + mOffsetX - mScale * mImageWidth / 2.0;
			double py = getHeight() / 2.0 + mOffsetY - mScale * mImageHeight / 2.0;
			double pw = mScale * mImageWidth;
			double ph = mScale * mImageHeight;

			double mx = Math.min(Math.max(aEvent.getX(), px), px + pw);
			double my = Math.min(Math.max(aEvent.getY(), py), py + ph);

			int dx = (int) (mx - ImagePane.this.getWidth() / 2.0);
			int dy = (int) (my - ImagePane.this.getHeight() / 2.0);

			mOffsetX -= dx;
			mOffsetY -= dy;

			if (scroll > 0 && mScale < 5)
			{
				mScale *= 1.2;
				mOffsetX *= 1.2;
				mOffsetY *= 1.2;
			}
			else if (scroll < 0 && mScale > 0.01)
			{
				mScale /= 1.2;
				mOffsetX /= 1.2;
				mOffsetY /= 1.2;
			}

			mOffsetX += dx;
			mOffsetY += dy;

			if (mSelectionStartOffset != null)
			{
				updateSelectionRectangle(aEvent);
			}
			else
			{
				repaint();
			}
		}
	}


	public void setInterpreterTool(InterpreterTool aInterpreterTool)
	{
		mInterpreterTool = aInterpreterTool;
	}


	public void clearSelection()
	{
		mRectangle = null;
		repaint();
	}


	public interface Overlay
	{
		void render(Graphics2D aGraphics);
	}
}