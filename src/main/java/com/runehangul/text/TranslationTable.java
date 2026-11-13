package com.runehangul.text;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Immutable lookup for one layer (the bundled data, or all user files together). */
public final class TranslationTable
{
	public static final TranslationTable EMPTY = new TranslationTable(Collections.emptyList());

	private final Map<String, String> byKey = new HashMap<>();

	/** Later entries win, so pass files in name order and lines in file order. */
	public TranslationTable(Iterable<Entry> entries)
	{
		for (Entry e : entries)
		{
			byKey.put(key(e.getEnglish(), e.getScope()), e.getTranslation());
		}
	}

	/** The translation for the most specific matching scope, or null. */
	public String find(String english, Scope scope)
	{
		for (Scope s : scope.widening())
		{
			String translation = byKey.get(key(english, s));
			if (translation != null)
			{
				return translation;
			}
		}
		return null;
	}

	public int size()
	{
		return byKey.size();
	}

	private static String key(String english, Scope scope)
	{
		return scope.getCategory() + '\u0000' + scope.getSubCategory() + '\u0000' + scope.getSource() + '\u0000' + english;
	}
}
