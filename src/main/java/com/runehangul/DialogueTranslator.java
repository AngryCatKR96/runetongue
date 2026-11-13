package com.runehangul;

import com.runehangul.glyph.GlyphIcons;
import com.runehangul.text.LineBreaker;
import com.runehangul.text.Normalizer;
import com.runehangul.text.Scope;
import com.runehangul.text.SourceText;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.FontTypeFace;
import net.runelite.api.Player;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

/**
 * Translates the chatbox dialogue: NPC lines, player lines, options, the NPC name and the
 * "Click here to continue" prompt. Runs every frame on the client thread but only looks at
 * those few widgets, and does a lookup only when a widget's text changes. A widget with no
 * translation is left exactly as the game drew it.
 */
@Slf4j
@Singleton
class DialogueTranslator
{
	private static final Pattern LEADING_COLOR = Pattern.compile("^<col=([0-9a-fA-F]{6})>");
	private static final String DIALOGUE = "dialogue";
	private static final Scope FIXED = Scope.of(DIALOGUE, "manual", "");
	private static final String PLAYER = "Player";
	private static final char NBSP = ' ';

	/**
	 * What a widget showed before we touched it and what we set it to. {@code lines} is null when
	 * there was no translation; then {@code output} is the original text.
	 */
	private static final class Shown
	{
		final String original;
		final List<String> lines;
		final int color;
		final String output;

		Shown(String original, List<String> lines, int color, String output)
		{
			this.original = original;
			this.lines = lines;
			this.color = color;
			this.output = output;
		}
	}

	private final Client client;
	private final GlyphIcons glyphs;
	private final TranslationStore store;
	private final Map<Long, Shown> shown = new HashMap<>();
	/** The NPC in the current conversation, in English; player lines and options are looked up under it. */
	private String npc = "";

	@Inject
	DialogueTranslator(Client client, GlyphIcons glyphs, TranslationStore store)
	{
		this.client = client;
		this.glyphs = glyphs;
		this.store = store;
	}

	void update()
	{
		String name = handle(InterfaceID.ChatLeft.NAME, 0, Scope.of("name", "npc", ""), false);
		if (name != null)
		{
			npc = Text.removeTags(name).replace(NBSP, ' ');
		}
		handle(InterfaceID.ChatLeft.TEXT, 0, Scope.of(DIALOGUE, npc, npc), true);
		handle(InterfaceID.ChatLeft.CONTINUE, 0, FIXED, false);
		handle(InterfaceID.ChatRight.TEXT, 0, Scope.of(DIALOGUE, npc, PLAYER), true);
		handle(InterfaceID.ChatRight.CONTINUE, 0, FIXED, false);

		Widget options = visible(client.getWidget(InterfaceID.Chatmenu.OPTIONS));
		if (options != null && options.getDynamicChildren() != null)
		{
			Widget[] children = options.getDynamicChildren();
			for (int i = 0; i < children.length; i++)
			{
				handle(children[i], key(InterfaceID.Chatmenu.OPTIONS, i), i == 0 ? FIXED : Scope.of(DIALOGUE, npc, PLAYER), false);
			}
		}
	}

	void reset()
	{
		shown.clear();
		npc = "";
	}

	private String handle(int widgetId, int index, Scope scope, boolean wrap)
	{
		return handle(visible(client.getWidget(widgetId)), key(widgetId, index), scope, wrap);
	}

	/** Translates the widget if its text changed; returns its original text, or null if it is not shown. */
	private String handle(Widget widget, long key, Scope scope, boolean wrap)
	{
		if (widget == null || widget.isHidden())
		{
			return null;
		}
		String text = widget.getText();
		if (text == null || text.isEmpty())
		{
			return null;
		}
		Shown last = shown.get(key);
		if (last != null && (text.equals(last.output) || text.equals(last.original)))
		{
			if (last.lines != null)
			{
				// The game recolors some text, e.g. "Click here to continue" turns white under the mouse;
				// glyph images carry their color, so redraw them in the widget's current color.
				int color = colorOf(widget, last.original);
				if (color != last.color || !text.equals(last.output))
				{
					last = show(widget, last.original, last.lines, color);
					shown.put(key, last);
				}
			}
			return last.original;
		}
		List<String> lines = translate(widget, text, scope, wrap);
		shown.put(key, lines == null ? new Shown(text, null, 0, text) : show(widget, text, lines, colorOf(widget, text)));
		return text;
	}

	private Shown show(Widget widget, String original, List<String> lines, int color)
	{
		String output = lines.stream()
			.map(line -> glyphs.toChatText(line, color, widget.getTextShadowed()))
			.collect(Collectors.joining("<br>"));
		widget.setText(output);
		return new Shown(original, lines, color, output);
	}

	/** A color tag at the very start of the original wins over the widget's color, as it does in the game. */
	private static int colorOf(Widget widget, String original)
	{
		Matcher m = LEADING_COLOR.matcher(original);
		return m.find() ? Integer.parseInt(m.group(1), 16) : widget.getTextColor();
	}

	private List<String> translate(Widget widget, String original, Scope scope, boolean wrap)
	{
		if (original.contains("<img="))
		{
			return null;
		}
		// The game breaks dialogue lines by replacing a space with <br>.
		String english = Text.removeTags(original.replaceAll("(?<=\\S)<br>(?=\\S)", " ")).replace(NBSP, ' ').trim();
		String player = playerName();
		if (player != null)
		{
			english = english.replace(player, Normalizer.PLAYER_NAME);
		}
		SourceText source = Normalizer.normalizeSource(english);
		String found = store.get().find(source.getKey(), scope);
		log.debug("dialogue {} color={} scope={} text={} -> {}", widget.getId(), Integer.toHexString(widget.getTextColor()), scope, original, found);
		if (found == null)
		{
			return null;
		}
		String text = Normalizer.restore(found, source);
		if (player != null)
		{
			text = text.replace(Normalizer.PLAYER_NAME, player);
		}
		FontTypeFace font = widget.getFont();
		return wrap && font != null
			? LineBreaker.wrap(text, widget.getWidth(), cp -> glyphs.width(cp, font))
			: Collections.singletonList(text);
	}

	private String playerName()
	{
		Player local = client.getLocalPlayer();
		String name = local == null ? null : local.getName();
		return name == null || name.isEmpty() ? null : name.replace(NBSP, ' ');
	}

	private static Widget visible(Widget widget)
	{
		return widget == null || widget.isHidden() ? null : widget;
	}

	private static long key(int widgetId, int index)
	{
		return ((long) widgetId << 8) | index;
	}
}
