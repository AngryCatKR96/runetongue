package com.runehangul.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * A translation whose English has slots for text that is translated separately or not at all,
 * written {@code <!TYPEn>} in the data as RuneLingual did: {@code Name: <!PLAYER_NAME0>},
 * {@code Monster: <colNum0><!NPC_NAME0></col>}. Matching fills each slot from the shown text, so
 * the rest can be looked up as the template and the slots put back afterwards.
 */
public final class PartialTemplate
{
	private static final Pattern TOKEN = Pattern.compile("<!([A-Z_]+\\d+)>|<Num\\d+>|<colNum\\d+>");

	@Getter
	private final String key;
	private final Pattern pattern;
	private final List<String> slots;

	private PartialTemplate(String key, Pattern pattern, List<String> slots)
	{
		this.key = key;
		this.pattern = pattern;
		this.slots = slots;
	}

	public static boolean isTemplate(String key)
	{
		return key.contains("<!");
	}

	public static PartialTemplate of(String key)
	{
		StringBuilder regex = new StringBuilder("^");
		List<String> slots = new ArrayList<>();
		Matcher m = TOKEN.matcher(key);
		int last = 0;
		while (m.find())
		{
			regex.append(Pattern.quote(key.substring(last, m.start())));
			if (m.group(1) != null)
			{
				slots.add(m.group(1));
				regex.append("(.+?)");
			}
			else if (m.group().startsWith("<Num"))
			{
				regex.append("\\d+(?:[.,]\\d+)*");
			}
			else
			{
				regex.append("<col=[0-9a-fA-F]+>");
			}
			last = m.end();
		}
		regex.append(Pattern.quote(key.substring(last))).append('$');
		return new PartialTemplate(key, Pattern.compile(regex.toString()), Collections.unmodifiableList(slots));
	}

	/** Slot name (e.g. {@code NPC_NAME0}) to its text, in order; null if the text does not fit. */
	public Map<String, String> match(String shown)
	{
		Matcher m = pattern.matcher(shown);
		if (!m.matches())
		{
			return null;
		}
		Map<String, String> filled = new LinkedHashMap<>();
		for (int i = 0; i < slots.size(); i++)
		{
			filled.put(slots.get(i), m.group(i + 1));
		}
		return filled;
	}

	/** {@code shown} with each slot's text replaced by its {@code <!TYPEn>} token again. */
	public String unfill(String shown, Map<String, String> filled)
	{
		Matcher m = pattern.matcher(shown);
		if (!m.matches())
		{
			return shown;
		}
		StringBuilder out = new StringBuilder();
		int last = 0;
		for (int i = 0; i < slots.size(); i++)
		{
			out.append(shown, last, m.start(i + 1)).append("<!").append(slots.get(i)).append('>');
			last = m.end(i + 1);
		}
		return out.append(shown.substring(last)).toString();
	}

	/** The kind of a slot: {@code PLAYER_NAME}, {@code NPC_NAME}, {@code ITEM_NAME}, {@code OBJECT_NAME} or {@code ANY_TRANSLATED}. */
	public static String typeOf(String slot)
	{
		return slot.replaceAll("\\d+$", "");
	}
}
