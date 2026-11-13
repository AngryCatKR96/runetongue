package com.runehangul.text;

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
