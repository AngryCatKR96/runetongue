package com.runehangul.text;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable lookup for one layer (the bundled data, or all user files together). Every line for
 * one English key is kept with its scope, so a lookup can fall back from an exact scope match to
 * a looser one. {@link Translations} decides the order across layers.
 */
public final class TranslationTable
{
	public static final TranslationTable EMPTY = new TranslationTable(Collections.emptyList());

	private static final class Candidate
	{
		final Scope scope;
		final String translation;

		Candidate(Scope scope, String translation)
		{
			this.scope = scope;
			this.translation = translation;
		}
	}

	private final Map<String, Candidate[]> byEnglish = new HashMap<>();
	private final int size;

	/** Later entries win, so pass files in name order and lines in file order. */
	public TranslationTable(Iterable<Entry> entries)
	{
		Map<Scope, Scope> scopes = new HashMap<>();
		int count = 0;
		for (Entry e : entries)
		{
			Candidate c = new Candidate(scopes.computeIfAbsent(e.getScope(), s -> s), e.getTranslation());
			Candidate[] old = byEnglish.get(e.getEnglish());
			if (old == null)
			{
				byEnglish.put(e.getEnglish(), new Candidate[]{c});
				count++;
				continue;
			}
			int same = indexOf(old, c.scope);
			if (same >= 0)
			{
				old[same] = c;
				continue;
			}
			Candidate[] grown = Arrays.copyOf(old, old.length + 1);
			grown[old.length] = c;
			byEnglish.put(e.getEnglish(), grown);
			count++;
		}
		size = count;
	}

	/**
	 * A line whose scope is the asked scope or a broader one (a line with no category applies
	 * everywhere), most specific first.
	 */
	public String exact(String english, Scope scope)
	{
		Candidate[] candidates = byEnglish.get(english);
		if (candidates == null)
		{
			return null;
		}
		for (Scope level : scope.widening())
		{
			for (int i = candidates.length - 1; i >= 0; i--)
			{
				if (candidates[i].scope.equals(level))
				{
					return candidates[i].translation;
				}
			}
		}
		return null;
	}

	/**
	 * A line that agrees with the asked scope on as many leading fields as possible, ignoring the
	 * rest: same category and sub_category but another source, then same category, then anything.
	 */
	public String relaxed(String english, Scope scope)
	{
		Candidate[] candidates = byEnglish.get(english);
		if (candidates == null)
		{
			return null;
		}
		for (Scope level : scope.widening())
		{
			for (int i = candidates.length - 1; i >= 0; i--)
			{
				if (candidates[i].scope.isWithin(level))
				{
					return candidates[i].translation;
				}
			}
		}
		return null;
	}

	/** Distinct (English, scope) pairs. */
	public int size()
	{
		return size;
	}

	private static int indexOf(Candidate[] candidates, Scope scope)
	{
		for (int i = 0; i < candidates.length; i++)
		{
			if (candidates[i].scope.equals(scope))
			{
				return i;
			}
		}
		return -1;
	}
}
