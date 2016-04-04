package org.terifan.ocr;

import java.awt.Insets;


class Tools
{
	public static Insets getBorders(Page aPage, int x, int y, int w, int h)
	{
		int x0 = x;
		int y0 = y;
		int x1 = x + w;
		int y1 = y + h;

		Insets borders = new Insets(0, 0, 0, 0);

		outer:
		for (y = y0; y < y1; y++)
		{
			for (x = x0; x < x1; x++)
			{
				if (aPage.isBlack(x,y))
				{
					borders.top = y - y0;
					break outer;
				}
			}
		}

		outer:
		for (y = y1 + 1; --y >= y0;)
		{
			for (x = x0; x < x1; x++)
			{
				if (aPage.isBlack(x,y))
				{
					borders.bottom = y1 - y;
					break outer;
				}
			}
		}

		outer:
		for (x = x0; x < x1; x++)
		{
			for (y = y0; y < y1; y++)
			{
				if (aPage.isBlack(x,y))
				{
					borders.left = x - x0;
					break outer;
				}
			}
		}

		outer:
		for (x = x1 + 1; --x >= x0;)
		{
			for (y = y0; y < y1; y++)
			{
				if (aPage.isBlack(x,y))
				{
					borders.right = x1 - x;
					break outer;
				}
			}
		}

		return borders;
	}


	private static int compareWords(String aTemplate, String aCompare, int aTemplateOffset, int aCompareOffset, int aMaxErrors, int aError, boolean aTrimCompare)
	{
		for (; aTemplateOffset < aTemplate.length() && aCompareOffset < aCompare.length(); aTemplateOffset++, aCompareOffset++)
		{
			char t = aTemplate.charAt(aTemplateOffset);
			if (t != '\r' && t != aCompare.charAt(aCompareOffset))
			{
				aError++;

				if (aError > aMaxErrors)
				{
					return aError;
				}

				int e1 = compareWords(aTemplate, aCompare, aTemplateOffset + 1, aCompareOffset, aMaxErrors, aError, aTrimCompare);
				int e2 = compareWords(aTemplate, aCompare, aTemplateOffset, aCompareOffset + 1, aMaxErrors, aError, aTrimCompare);
				int e3 = compareWords(aTemplate, aCompare, aTemplateOffset + 1, aCompareOffset + 1, aMaxErrors, aError, aTrimCompare);

				return Math.min(e1, Math.min(e2, e3));
			}
		}

		if (aTrimCompare && aTemplateOffset == aTemplate.length())
		{
			return aError;
		}

		return aError + Math.abs((aTemplate.length() - aTemplateOffset) - (aCompare.length() - aCompareOffset));
	}


	public static boolean compareWords(String aTemplate, String aCompare, int aMaxErrors, boolean aCaseSensitive)
	{
		//System.out.println(Tools.compareWords("abcdefghijklmnop", "abcdxfghijklmnop", 10, false));
		//System.out.println(Tools.compareWords("abcdefghijklmnop", "abcdxxfghijklmnop", 10, false));
		//System.out.println(Tools.compareWords("abcdefghijklmnop", "abcdxxxfghijklmnop", 10, false));
		//System.out.println(Tools.compareWords("abcdefghijklmnop", "abcdfghijklmnop", 10, false));
		//System.out.println(Tools.compareWords("abcdefghijklmnop", "abcdghijklmnop", 10, false));
		//System.out.println(Tools.compareWords("abcdefghijklmnop", "abcdhijklmnop", 10, false));
		//System.out.println(Tools.compareWords("abcdefghijklmnop", "acdeghijklmnop", 10, false));
		//System.out.println(Tools.compareWords("abcdefghijklmnop", "axcdeghijklmnop", 10, false));
		//System.out.println(Tools.compareWords("abcdefghijklmnop", "axcdexghijklmnop", 10, false));
		//System.out.println(Tools.compareWords("abcdefghijklmnop", "axcdexghijklmnopxx", 10, false));
		//System.out.println(Tools.compareWords("abcdefghijklmnop", "axcdexghijklmn", 10, false));
		//System.out.println(Tools.compareWords("TOBAPHO-TPAHCIOPTHARHAKJADHARNo^", "TOBAPHO-TPARCIOPTBARHAKJADHARNO", 10, false));
		//System.out.println(Tools.compareWords("TOBAPHO-TPAHCIOPTHARHAKJADHARNo^", "TOBAPHO-TPHCIOP-RHAKXYZJADHARNo121271/o8", 10, false));

		boolean debug = false;

		if (!aCaseSensitive)
		{
			aTemplate = aTemplate.toLowerCase();
			aCompare = aCompare.toLowerCase();
		}

		boolean trimCompare = aTemplate.endsWith("^");
		if (trimCompare)
		{
			aTemplate = aTemplate.substring(0, aTemplate.length() - 1);
		}

		int err = compareWords(aTemplate, aCompare, 0, 0, aMaxErrors, 0, trimCompare);
		if (debug)
		{
			System.out.println("error=" + err);
		}

		return err <= aMaxErrors;
	}
}
