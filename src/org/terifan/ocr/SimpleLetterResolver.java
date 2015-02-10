package org.terifan.ocr;


public class SimpleLetterResolver extends SimpleResolver
{
	private boolean mLetters;
	private boolean mDigits;


	public SimpleLetterResolver()
	{
		mLetters = true;
		mDigits = true;
	}


	public SimpleLetterResolver(int aMaxErrors, String ... aWords)
	{
		super(aMaxErrors, aWords);

		if (aWords.length == 0)
		{
			mLetters = true;
			mDigits = true;
		}

		for (String s : aWords)
		{
			for (char c : s.toCharArray())
			{
				mLetters |= Character.isLetter(c);
				mDigits |= Character.isDigit(c);
			}
		}
	}


	@Override
	public boolean acceptSymbol(Page aPage, TextBox aTextBox, Symbol aSymbol)
	{
		char c = aSymbol.getCharacter().charAt(0);

		return mLetters && Character.isLetter(c) || mDigits && Character.isDigit(c);
	}
}
