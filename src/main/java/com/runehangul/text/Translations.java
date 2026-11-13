package com.runehangul.text;

/** User files over the bundled data: any user match beats any bundled match. */
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
		String translation = user.find(english, scope);
		return translation != null ? translation : bundled.find(english, scope);
	}
}
