package org.terifan.ocr.application;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import org.terifan.ocr.Bitmap;
import org.terifan.ocr.Page;
import org.terifan.ocr.OCREngine;
import org.terifan.ocr.Resolver;
import org.terifan.ocr.SimpleResolver;
import org.terifan.ocr.TextBox;


public class Application
{
	public static void main(String... args)
	{
		try
		{
			Bitmap bitmap = new Bitmap(ImageIO.read(Application.class.getResource("samples/sample_02.png")));
//			Bitmap bitmap = new Bitmap(ImageIO.read(Application.class.getResource("samples/sample_01.png")));
			bitmap.eraseLines(0.5, 0);

			Page page = new Page(bitmap);

			OCREngine engine = new OCREngine();
//			engine.learnAlphabet("courier new", new Page(new Bitmap(ImageIO.read(OCREngine.class.getResource("fonts/alphabet_arial_ru_bold.png")))));
			engine.learnAlphabet("courier new", new Bitmap(ImageIO.read(OCREngine.class.getResource("fonts/alphabet_arial.png"))));
			engine.setMinSymbolWidth(2);
			engine.setMaxSymbolWidth(30);
			engine.setMinSymbolHeight(10);
			engine.setMaxSymbolHeight(35);
			engine.setCharacterSpacingFraction(0.25);

			engine.loadPage(0.0, 0.0, 1.0, 1.0, page);

			Resolver resolver = new SimpleResolver();
//			Resolver resolver = new WaybillResolver();

			System.out.println(engine.scan(0.0, 0.0, 1.0, 1.0, resolver));

			ImagePane imagePane = new ImagePane(bitmap.getImage());
//			imagePane.setInterpreterTool(new InterpreterTool()
//			{
//				@Override
//				public void performAction(Rectangle aRectangle)
//				{
//				}
//
//				@Override
//				public void onReleaseFocus()
//				{
//				}
//			});

			ImagePane imagePane2 = new ImagePane(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));

			DefaultMutableTreeNode root = new DefaultMutableTreeNode();
			buildTree(root, engine.getTextBoxes());
			JTree tree = new JTree(root);
			tree.addTreeSelectionListener(e ->
			{
				TextBox tb = (TextBox)((DefaultMutableTreeNode)e.getPath().getLastPathComponent()).getUserObject();
				if (tb != null)
				{
					imagePane.setImageOverlay(g->{
						g.setColor(new Color(255,255,0,128));
						if (tb.getChildren().isEmpty())
						{
							g.fillRect(tb.x, tb.y, tb.width+1, tb.height+1);

							imagePane2.setImage(bitmap.getRegion(tb.x, tb.y, tb.x+tb.width, tb.y+tb.height));
						}
						for (TextBox tb1 : tb.getChildren())
						{
							g.fillRect(tb1.x, tb1.y, tb1.width+1, tb1.height+1);
						}
					});
					imagePane.repaint();
				}
			});

			JPanel controlPane = new JPanel(new BorderLayout());
			controlPane.add(new JScrollPane(tree), BorderLayout.CENTER);

			JSplitPane splitPaneH = new JSplitPane(JSplitPane.VERTICAL_SPLIT, imagePane, imagePane2);
			JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlPane, splitPaneH);

			JFrame frame = new JFrame();
			frame.add(splitPane);
			frame.setSize(1400, 768);
			frame.setLocationRelativeTo(null);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);

			splitPane.setDividerLocation(0.25);
			splitPaneH.setDividerLocation(0.85);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static void buildTree(DefaultMutableTreeNode aNode, ArrayList<TextBox> aTextBoxes)
	{
		for (TextBox textBox : aTextBoxes)
		{
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(textBox);
			aNode.add(node);

			buildTree(node, textBox.getChildren());
		}
	}
}
