package com.runehangul;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Languages a user translation folder can hold. Only Korean ships bundled translations. */
@Getter
@AllArgsConstructor
public enum Language
{
	// Shown in RuneLite's config dropdown, which draws with the RuneScape font: no Hangul there.
	KOREAN("ko", "Korean", "/com/runehangul/translations_ko.tsv.gz"),
	SPANISH("es", "Español (Spanish)", null),
	PORTUGUESE("pt", "Português (Portuguese)", null),
	FRENCH("fr", "Français (French)", null),
	GERMAN("de", "Deutsch (German)", null);

	private final String code;
	private final String displayName;
	private final String bundledResource;

	/** The plugin's own messages are Korean for Korean and English for every other language. */
	public String ui(String korean, String english)
	{
		return this == KOREAN ? korean : english;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
