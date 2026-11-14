package com.runetongue.text;

import java.util.ArrayList;
import java.util.List;
import lombok.Value;

/**
 * Where a translation applies. Fields narrow in order (category, then sub_category, then source);
 * a field only counts when the ones before it are set, so a scope is one of four levels.
 */
@Value
public class Scope
{
	public static final Scope ANY = new Scope("", "", "");

	String category;
	String subCategory;
	String source;

	public static Scope of(String category, String subCategory, String source)
	{
		String c = category.trim();
		String s = c.isEmpty() ? "" : subCategory.trim();
		String o = s.isEmpty() ? "" : source.trim();
		return c.isEmpty() ? ANY : new Scope(c, s, o);
	}

	/** True when every field set in {@code level} matches this scope's; unset fields match anything. */
	public boolean isWithin(Scope level)
	{
		return (level.category.isEmpty() || level.category.equals(category))
			&& (level.subCategory.isEmpty() || level.subCategory.equals(subCategory))
			&& (level.source.isEmpty() || level.source.equals(source));
	}

	/** This scope and each broader one, most specific first, ending with {@link #ANY}. */
	public List<Scope> widening()
	{
		List<Scope> scopes = new ArrayList<>(4);
		if (!source.isEmpty())
		{
			scopes.add(this);
		}
		if (!subCategory.isEmpty())
		{
			scopes.add(new Scope(category, subCategory, ""));
		}
		if (!category.isEmpty())
		{
			scopes.add(new Scope(category, "", ""));
		}
		scopes.add(ANY);
		return scopes;
	}
}
