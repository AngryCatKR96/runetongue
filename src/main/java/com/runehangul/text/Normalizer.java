package com.runehangul.text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The lookup-key rules shared by the bundled data, user translation files and live game text.
 */
public final class Normalizer
{
	public static final String PLAYER_NAME = "[player name]";

	private static final Pattern TOKEN = Pattern.compile(
		"(?<num><Num(?<numIdx>\\d+)>)"
			+ "|(?<colnum><colNum(?<colIdx>\\d+)>)"
			+ "|(?<col><col[=A-Za-z0-9]*>)"
			+ "|(?<tag><![A-Z_]+\\d+>|<[^<>\\s]{1,15}>)"
			+ "|(?<digits>\\d+(?:[.,]\\d+)*)");
	private static final Pattern PLACEHOLDER = Pattern.compile("<Num\\d+>|<colNum\\d+>|" + Pattern.quote(PLAYER_NAME));

	private Normalizer()
	{
	}

	public static SourceText normalizeSource(String english)
	{
		String text = english.trim();
		int nextNum = maxIndex(text, "numIdx") + 1;
		int nextCol = maxIndex(text, "colIdx") + 1;
		List<String> numbers = new ArrayList<>();
		List<String> colors = new ArrayList<>();
		for (int i = 0; i < nextNum; i++)
		{
			numbers.add(null);
		}
		for (int i = 0; i < nextCol; i++)
		{
			colors.add(null);
		}

		StringBuilder out = new StringBuilder(text.length());
		Matcher m = TOKEN.matcher(text);
		while (m.find())
		{
			if (m.group("col") != null)
			{
				m.appendReplacement(out, "<colNum" + colors.size() + ">");
				colors.add(m.group("col"));
			}
			else if (m.group("digits") != null)
			{
				m.appendReplacement(out, "<Num" + numbers.size() + ">");
				numbers.add(m.group("digits"));
			}
			else
			{
				m.appendReplacement(out, Matcher.quoteReplacement(m.group()));
			}
		}
		m.appendTail(out);
		return new SourceText(out.toString(), numbers, colors);
	}

	/**
	 * Replaces raw numbers and color tags in a translation with the source placeholder holding the
	 * same value, so "5 of 10" translated as "10 중 5" keeps each number in place. Values the
	 * source does not have stay as written.
	 */
	public static String normalizeTranslation(String translation, SourceText source)
	{
		String text = translation.trim();
		boolean[] usedNum = new boolean[source.getNumbers().size()];
		boolean[] usedCol = new boolean[source.getColors().size()];
		StringBuilder out = new StringBuilder(text.length());
		Matcher m = TOKEN.matcher(text);
		while (m.find())
		{
			String replacement = m.group();
			if (m.group("col") != null)
			{
				int i = claim(source.getColors(), usedCol, m.group("col"));
				if (i >= 0)
				{
					replacement = "<colNum" + i + ">";
				}
			}
			else if (m.group("digits") != null)
			{
				int i = claim(source.getNumbers(), usedNum, m.group("digits"));
				if (i >= 0)
				{
					replacement = "<Num" + i + ">";
				}
			}
			m.appendReplacement(out, Matcher.quoteReplacement(replacement));
		}
		m.appendTail(out);
		return out.toString();
	}

	public static String restore(String translation, SourceText source)
	{
		StringBuilder out = new StringBuilder(translation.length());
		Matcher m = TOKEN.matcher(translation);
		while (m.find())
		{
			String replacement = m.group();
			if (m.group("num") != null)
			{
				replacement = valueAt(source.getNumbers(), m.group("numIdx"), replacement);
			}
			else if (m.group("colnum") != null)
			{
				replacement = valueAt(source.getColors(), m.group("colIdx"), replacement);
			}
			m.appendReplacement(out, Matcher.quoteReplacement(replacement));
		}
		m.appendTail(out);
		return out.toString();
	}

	public static Set<String> placeholders(String normalized)
	{
		Set<String> found = new HashSet<>();
		Matcher m = PLACEHOLDER.matcher(normalized);
		while (m.find())
		{
			found.add(m.group());
		}
		return found;
	}

	private static int maxIndex(String text, String group)
	{
		int max = -1;
		Matcher m = TOKEN.matcher(text);
		while (m.find())
		{
			if (m.group(group) != null)
			{
				max = Math.max(max, Integer.parseInt(m.group(group)));
			}
		}
		return max;
	}

	private static int claim(List<String> values, boolean[] used, String value)
	{
		for (int i = 0; i < values.size(); i++)
		{
			if (!used[i] && value.equals(values.get(i)))
			{
				used[i] = true;
				return i;
			}
		}
		return -1;
	}

	private static String valueAt(List<String> values, String index, String fallback)
	{
		int i = Integer.parseInt(index);
		return i < values.size() && values.get(i) != null ? values.get(i) : fallback;
	}
}
