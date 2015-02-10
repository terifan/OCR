package org.terifan.ocr;


public class SimpleResolver implements Resolver
{
	protected int mMaxErrors;
	protected String [] mWords;


	public SimpleResolver()
	{
	}


	public SimpleResolver(int aMaxErrors, String ... aWords)
	{
		mMaxErrors = aMaxErrors;
		mWords = aWords;
	}


	@Override
	public boolean acceptSymbol(Page aPage, TextBox aTextBox, Symbol aSymbol)
	{
		return true;
	}


	@Override
	public boolean acceptWord(Page aPage, TextBox aTextBox)
	{
		if (mWords == null || mWords.length == 0)
		{
			return true;
		}

		String s = aTextBox.toString();

		for (String w : mWords)
		{
			if (Tools.compareWords(w, s, mMaxErrors, false))
			{
				return true;
			}
		}

		return false;
	}
}