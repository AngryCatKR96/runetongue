package com.runehangul;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Languages a user translation folder can hold. Only Korean ships bundled translations. */
@Getter
@AllArgsConstructor
public enum Language
{
	KOREAN("ko", "한국어 (Korean)", "/com/runehangul/translations_ko.tsv.gz"),
	SPANISH("es", "Español (Spanish)", null),
	PORTUGUESE("pt", "Português (Portuguese)", null),
	FRENCH("fr", "Français (French)", null),
	GERMAN("de", "Deutsch (German)", null);

	private final String code;
	private final String displayName;
	private final String bundledResource;

	@Override
	public String toString()
	{
		return displayName;
	}
}
