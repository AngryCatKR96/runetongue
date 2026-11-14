package com.runetongue.text;

import java.util.ArrayList;
import java.util.List;

/**
 * User files over the bundled data. An exact scope match in either layer beats any looser match;
 * within each kind, the user layer is asked first. So a user line with no category overrides the
 * bundled data everywhere, but a user line for one category does not override another category's
 * exact bundled line.
 */
public final class Translations
{
	public static final Translations EMPTY = new Translations(TranslationTable.EMPTY, TranslationTable.EMPTY);

	private final TranslationTable user;
	private final TranslationTable bundled;

	public Translations(TranslationTable user, TranslationTable bundled)
	{
		this.user = user;
		this.bundled = bundled;
	}

	/** Templates from both layers; a template is found with {@link #findTemplate}. */
	public List<PartialTemplate> templates()
	{
		List<PartialTemplate> all = new ArrayList<>(user.templates());
		all.addAll(bundled.templates());
		return all;
	}

	/**
	 * A template's translation, only from lines of the asked category and sub_category: a template
	 * such as {@code <!ANY_TRANSLATED0> (<Num0>)} would otherwise fit text anywhere.
	 */
	public String findTemplate(String english, Scope scope)
	{
		String t = user.exact(english, scope);
		if (t == null)
		{
			t = bundled.exact(english, scope);
		}
		if (t == null)
		{
			t = user.relaxed(english, scope, true);
		}
		if (t == null)
		{
			t = bundled.relaxed(english, scope, true);
		}
		return t;
	}

	public String find(String english, Scope scope)
	{
		String t = user.exact(english, scope);
		if (t == null)
		{
			t = bundled.exact(english, scope);
		}
		if (t == null)
		{
			t = user.relaxed(english, scope);
		}
		if (t == null)
		{
			t = bundled.relaxed(english, scope);
		}
		return t;
	}
}
