package org.terifan.ocr;


public interface Resolver
{
	public boolean acceptSymbol(Page aPage, TextBox aTextBox, Symbol aSymbol);

	public boolean acceptWord(Page aPage, TextBox aTextBox);
}