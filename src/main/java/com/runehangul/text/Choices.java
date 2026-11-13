package com.runehangul.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands {@code [man/woman]} style choices. The game picks one option per line, so option k of
 * every bracket goes together; the k-th English variant pairs with the k-th translation variant.
 */
final class Choices
{
	private static final Pattern GROUP = Pattern.compile("\\[([^\\[\\]/]+(?:/[^\\[\\]/]+)+)]");

	private Choices()
	{
	}

	/** Returns the number of options, 1 when there are no brackets, or -1 when brackets disagree. */
	static int count(String text)
	{
		int count = 1;
		Matcher m = GROUP.matcher(text);
		while (m.find())
		{
			int n = m.group(1).split("/", -1).length;
			if (count != 1 && count != n)
			{
				return -1;
			}
			count = n;
		}
		return count;
	}

	/** Returns {@code count(text)} variants; text without brackets is repeated. */
	static List<String> expand(String text, int count)
	{
		if (count(text) == 1)
		{
			return Collections.nCopies(count, text);
		}
		List<String> variants = new ArrayList<>(count);
		for (int k = 0; k < count; k++)
		{
			StringBuilder out = new StringBuilder(text.length());
			Matcher m = GROUP.matcher(text);
			while (m.find())
			{
				m.appendReplacement(out, Matcher.quoteReplacement(m.group(1).split("/", -1)[k]));
			}
			m.appendTail(out);
			variants.add(out.toString());
		}
		return variants;
	}
}
