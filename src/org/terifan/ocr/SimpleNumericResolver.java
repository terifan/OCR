package org.terifan.ocr;


public class SimpleNumericResolver extends SimpleResolver
{
	public SimpleNumericResolver()
	{
		super(0);
	}


	@Override
	public boolean acceptSymbol(Page aPage, TextBox aTextBox, Symbol aSymbol)
	{
		return Character.isDigit(aSymbol.getCharacter().charAt(0));
	}
}
