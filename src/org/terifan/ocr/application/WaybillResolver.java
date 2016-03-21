package org.terifan.ocr.application;

import org.terifan.ocr.Page;
import org.terifan.ocr.Resolver;
import org.terifan.ocr.Symbol;
import org.terifan.ocr.TextBox;


class WaybillResolver implements Resolver
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
