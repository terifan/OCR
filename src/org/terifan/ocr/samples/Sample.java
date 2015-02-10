package org.terifan.ocr.samples;

import org.terifan.ocr.Page;
import org.terifan.ocr.OCREngine;
import org.terifan.ocr.Resolver;
import org.terifan.ocr.Symbol;
import org.terifan.ocr.TextBox;
import org.terifan.io.Streams;
import org.terifan.util.log.Log;


public class Sample
{
	public static void main(String... args)
	{
		try
		{
			Page page = new Page(Streams.fetch("d:/sample.jpg"));

			OCREngine engine = new OCREngine();
			engine.learnAlphabet("courier new", new Page(OCREngine.class.getResourceAsStream("fonts/alphabet_courier_3.png")));
			engine.setMinSymbolWidth(8);
			engine.setMaxSymbolWidth(23);
			engine.setMinSymbolHeight(10);
			engine.setMaxSymbolHeight(25);
			engine.setCharacterSpacingExact(10);

			page.initDebug();

			engine.loadPage(0.4, 0, 1, 0.5, page);

			Log.out.println(engine.scan(0.4, 0, 1, 0.5, new WaybillResolver()));

			page.writeDebug("d:/debug.png");
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static class WaybillResolver implements Resolver
	{
		@Override
		public boolean acceptSymbol(Page aPage, TextBox aTextBox, Symbol aSymbol)
		{
			char c = aSymbol.getCharacter().charAt(0);

			if (aTextBox.getIndex() < 3)
			{
				return Character.isLetter(c);
			}
			else if (aTextBox.getIndex() == 3)
			{
				return Character.isDigit(c) || c == 'S' || c == 'X' || c == 's' || c == 'x';
			}

			return Character.isDigit(c) || c == '/';
		}


		@Override
		public boolean acceptWord(Page aPage, TextBox aTextBox)
		{
			return aTextBox.toString().matches("[A-Za-z]{3,4}[0-9]{5,9}[/]?");
		}
	}
}
