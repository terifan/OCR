package org.terifan.ocr.application;

import javax.swing.JFrame;
import org.terifan.ocr.Page;
import org.terifan.ocr.OCREngine;
import org.terifan.ocr.Resolver;
import org.terifan.ocr.SimpleResolver;


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

			JFrame frame = new JFrame();
			frame.add(imagePane);
			frame.setSize(1024, 768);
			frame.setLocationRelativeTo(null);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);

			page.writeDebug("d:/debug.png");
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
