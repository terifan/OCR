package org.terifan.ocr;


public class Result implements Comparable<Result>
{
	protected Symbol mSymbol;
	protected double mScore;


	public Result(double aScore, Symbol aSymbol)
	{
		mSymbol = aSymbol;
		mScore = aScore;
	}


	@Override
	public String toString()
	{
		return mSymbol.mCharacter+"="+mScore;
	}


	public boolean compare(Result aResult)
	{
		return mSymbol.mCharacter.equalsIgnoreCase(aResult.mSymbol.mCharacter);
	}


	public Symbol getSymbol()
	{
		return mSymbol;
	}


	@Override
	public int compareTo(Result aOther)
	{
		return Double.compare(mScore, aOther.mScore);
	}
}