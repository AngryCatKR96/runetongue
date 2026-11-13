package com.runehangul.text;

import java.util.Map;

/**
 * One lookup of game text: the plain key first, then the partial templates. Pure, so the
 * plugin and tests share it.
 */
public final class TextLookup
{
	private TextLookup()
	{
	}

	/** The translation of {@code english} with its numbers and colors put back, or null. */
	public static String translate(Translations t, String english, Scope scope)
	{
		return translate(t, english, scope, true);
	}

	private static String translate(Translations t, String english, Scope scope, boolean withTemplates)
	{
		SourceText source = Normalizer.normalizeSource(english);
		String found = t.find(source.getKey(), scope);
		if (found != null)
		{
			return Normalizer.restore(found, source);
		}
		return withTemplates ? fromTemplate(t, english, scope) : null;
	}

	/**
	 * Text with a part translated separately or kept as is, such as "Name: Zezima" or "Monster:
	 * Giant Mole" (see {@link PartialTemplate}). Unknown text of this kind is never recorded: it
	 * usually holds a player's name.
	 */
	private static String fromTemplate(Translations t, String english, Scope scope)
	{
		for (PartialTemplate template : t.templates())
		{
			Map<String, String> filled = template.match(english);
			if (filled == null)
			{
				continue;
			}
			SourceText source = Normalizer.normalizeSource(template.unfill(english, filled));
			if (!source.getKey().equals(template.getKey()))
			{
				continue;
			}
			String found = t.findTemplate(template.getKey(), scope);
			if (found == null)
			{
				continue;
			}
			String text = Normalizer.restore(found, source);
			for (Map.Entry<String, String> slot : filled.entrySet())
			{
				String part = slotText(t, slot.getKey(), slot.getValue());
				if (part == null)
				{
					return null;
				}
				text = text.replace("<!" + slot.getKey() + ">", part);
			}
			return text;
		}
		return null;
	}

	/** A slot's translation by its kind, as RuneLingual's PartialTranslationManager did; null if it has none. */
	private static String slotText(Translations t, String slot, String content)
	{
		switch (PartialTemplate.typeOf(slot))
		{
			case "PLAYER_NAME":
				return content;
			case "NPC_NAME":
				return translate(t, content, Scope.of("name", "npc", ""), false);
			case "ITEM_NAME":
				return translate(t, content, Scope.of("name", "item", ""), false);
			case "OBJECT_NAME":
				return translate(t, content, Scope.of("name", "object", ""), false);
			default:
				return translate(t, content, Scope.ANY, false);
		}
	}

}
