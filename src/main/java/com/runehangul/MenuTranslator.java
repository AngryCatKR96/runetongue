package com.runehangul;

import com.runehangul.glyph.GlyphIcons;
import com.runehangul.text.Scope;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.client.util.Text;

/**
 * Translates right-click menu options and the names in their targets. Menu text is replaced just
 * before each frame is drawn, after every plugin has added and sorted entries by their English
 * text, and put back when an entry is clicked so other plugins still see English. Results are
 * cached, so a frame costs a few map lookups. Client thread only.
 */
@Slf4j
@Singleton
class MenuTranslator
{
	private static final int CACHE_SIZE = 4096;
	private static final int WHITE = 0xFFFFFF;
	/** A target is a run of {@code <col=rrggbb>text} pieces, e.g. a name followed by its level. */
	private static final Pattern PIECE = Pattern.compile("(<col=([0-9a-fA-F]{1,6})>)?([^<]+)|(<[^>]*>)");
	private static final Pattern LEVEL = Pattern.compile("\\s*\\(level-\\d+\\)\\s*");

	private final Client client;
	private final GlyphIcons glyphs;
	private final GameTextLookup lookup;
	/** Translated text by original, for options and targets alike; also tells our own output apart. */
	private final Map<String, String> translated = lru();
	private final Map<String, String> originals = lru();

	@Inject
	MenuTranslator(Client client, GlyphIcons glyphs, GameTextLookup lookup)
	{
		this.client = client;
		this.glyphs = glyphs;
		this.lookup = lookup;
	}

	void update()
	{
		if (!lookup.isReady())
		{
			return;
		}
		for (MenuEntry entry : client.getMenu().getMenuEntries())
		{
			if (entry.getType().name().startsWith("RUNELITE"))
			{
				// Added by a RuneLite plugin, not game text.
				continue;
			}
			String option = entry.getOption();
			String target = entry.getTarget();
			if (originals.containsKey(option) || originals.containsKey(target))
			{
				continue;
			}
			Kind kind = Kind.of(entry);
			String name = Text.removeTags(LEVEL.matcher(target).replaceAll("")).trim();
			String newOption = cached(kind.ordinal() + "|" + name + "|" + option, option, () -> option(option, kind, name));
			String newTarget = kind == Kind.PLAYER ? target : cached(kind.ordinal() + "|" + target, target, () -> target(target, kind));
			if (!newOption.equals(option))
			{
				entry.setOption(newOption);
			}
			if (!newTarget.equals(target))
			{
				entry.setTarget(newTarget);
			}
		}
	}

	/** Puts the English back on a clicked entry before other plugins read it. */
	void restore(MenuEntry entry)
	{
		String option = originals.get(entry.getOption());
		if (option != null)
		{
			entry.setOption(option);
		}
		String target = originals.get(entry.getTarget());
		if (target != null)
		{
			entry.setTarget(target);
		}
	}

	void reset()
	{
		translated.clear();
		originals.clear();
	}

	private String cached(String key, String original, java.util.function.Supplier<String> translate)
	{
		String result = translated.get(key);
		if (result == null)
		{
			result = translate.get();
			log.debug("menu {} -> {}", key, result);
			translated.put(key, result);
			if (!result.equals(original))
			{
				originals.put(result, original);
			}
		}
		return result;
	}

	private String option(String option, Kind kind, String targetName)
	{
		String found = lookup.find(option, Scope.of(kind.category, kind.subCategory, targetName), true);
		return found == null ? option : glyphs.toChatText(found, WHITE, false);
	}

	/** Translates each name piece of the target; colors, levels and " -> " stay as they are. */
	private String target(String target, Kind kind)
	{
		StringBuilder out = new StringBuilder();
		boolean changed = false;
		Matcher m = PIECE.matcher(target);
		while (m.find())
		{
			if (m.group(4) != null)
			{
				out.append(m.group(4));
				continue;
			}
			String tag = m.group(1) == null ? "" : m.group(1);
			String text = m.group(3);
			int color = m.group(2) == null ? WHITE : Integer.parseInt(m.group(2), 16);
			String name = text.trim();
			String found = name.isEmpty() || LEVEL.matcher(text).matches() || name.equals("->")
				? null : lookup.find(name, Scope.of("name", kind.nameSubCategory, ""), true);
			if (found == null)
			{
				out.append(tag).append(text);
				continue;
			}
			changed = true;
			String lead = text.substring(0, text.indexOf(name));
			String trail = text.substring(text.indexOf(name) + name.length());
			out.append(tag).append(lead).append(glyphs.toChatText(found, color, false)).append(trail);
		}
		return changed ? out.toString() : target;
	}

	private static <V> Map<String, V> lru()
	{
		return new LinkedHashMap<String, V>(256, 0.75f, true)
		{
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, V> eldest)
			{
				return size() > CACHE_SIZE;
			}
		};
	}

	/** What a menu entry acts on, and where its option and target are found in the data. */
	private enum Kind
	{
		NPC("actions", "npc", "npc"),
		OBJECT("actions", "object", "object"),
		GROUND_ITEM("actions", "item", "item"),
		INVENTORY_ITEM("inventoryActions", "item", "item"),
		PLAYER("actions", "player", "player"),
		FIXED("actions", "manual", "menu"),
		OTHER("actions", "menu", "menu");

		final String category;
		final String subCategory;
		final String nameSubCategory;

		Kind(String category, String subCategory, String nameSubCategory)
		{
			this.category = category;
			this.subCategory = subCategory;
			this.nameSubCategory = nameSubCategory;
		}

		static Kind of(MenuEntry entry)
		{
			MenuAction type = entry.getType();
			if (entry.getNpc() != null)
			{
				return NPC;
			}
			if (entry.getPlayer() != null)
			{
				return PLAYER;
			}
			switch (type)
			{
				case GAME_OBJECT_FIRST_OPTION:
				case GAME_OBJECT_SECOND_OPTION:
				case GAME_OBJECT_THIRD_OPTION:
				case GAME_OBJECT_FOURTH_OPTION:
				case GAME_OBJECT_FIFTH_OPTION:
				case EXAMINE_OBJECT:
				case WIDGET_TARGET_ON_GAME_OBJECT:
					return OBJECT;
				case GROUND_ITEM_FIRST_OPTION:
				case GROUND_ITEM_SECOND_OPTION:
				case GROUND_ITEM_THIRD_OPTION:
				case GROUND_ITEM_FOURTH_OPTION:
				case GROUND_ITEM_FIFTH_OPTION:
				case EXAMINE_ITEM_GROUND:
				case WIDGET_TARGET_ON_GROUND_ITEM:
					return GROUND_ITEM;
				case WALK:
				case CANCEL:
					return FIXED;
				default:
					return entry.getItemId() > 0 ? INVENTORY_ITEM : OTHER;
			}
		}
	}
}
