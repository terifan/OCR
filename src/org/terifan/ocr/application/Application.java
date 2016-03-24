package org.terifan.ocr.application;

import java.awt.BorderLayout;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
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
			Page page = new Page(Application.class.getResourceAsStream("samples/sample_01.png"));

			page.eraseLines(0.5, 0);
			page.updateImage();

			OCREngine engine = new OCREngine();
			engine.learnAlphabet("courier new", new Page(OCREngine.class.getResourceAsStream("fonts/alphabet_arial_ru_bold.png")));
			engine.setMinSymbolWidth(8);
			engine.setMaxSymbolWidth(23);
			engine.setMinSymbolHeight(10);
			engine.setMaxSymbolHeight(25);
			engine.setCharacterSpacingExact(10);

			page.initDebug();

			engine.loadPage(0.0, 0.0, 1.0, 1.0, page);

			Resolver resolver = new SimpleResolver();
//			Resolver resolver = new WaybillResolver();

			System.out.println(engine.scan(0.0, 0.0, 1.0, 1.0, resolver));

			ImagePane imagePane = new ImagePane(page.getDebugImage());
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

			DefaultMutableTreeNode root = new DefaultMutableTreeNode();
			buildTree(root, engine.getTextBoxes());
			JTree tree = new JTree(root);

			JPanel controlPane = new JPanel(new BorderLayout());
			controlPane.add(new JScrollPane(tree), BorderLayout.CENTER);

			JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlPane, imagePane);

			JFrame frame = new JFrame();
			frame.add(splitPane);
			frame.setSize(1400, 768);
			frame.setLocationRelativeTo(null);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);

			splitPane.setDividerLocation(0.25);

			page.writeDebug("d:/debug.png");
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
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(textBox.toString());
			aNode.add(node);

			buildTree(aNode, textBox.getChildren());
		}
	}
}
