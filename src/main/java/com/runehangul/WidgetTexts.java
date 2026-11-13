package com.runehangul;

import com.runehangul.glyph.GlyphIcons;
import com.runehangul.text.LineBreaker;
import com.runehangul.text.Scope;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AllArgsConstructor;
import net.runelite.api.FontTypeFace;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetSizeMode;

/**
 * Replaces a widget's text with its translation, drawn with glyph images. Remembers what each
 * widget showed, so a lookup happens only when the game changes the text, and a widget the game
 * resets to English gets the translation back without a lookup. A widget with no translation is
 * left exactly as the game drew it. Client thread only.
 */
@Singleton
class WidgetTexts
{
	/** Glyph line height; RuneLingual used the same for Korean. */
	private static final int GLYPH_LINE_HEIGHT = 12;
	private static final Pattern LEADING_COLOR = Pattern.compile("^<col=([0-9a-fA-F]{6})>");

	/** How a widget's lines are laid out (RuneLingual's NiceBr, KeepBr, SplitTextAtBr and BrAsIs). */
	enum Layout
	{
		/** Look up without the game's {@code <br>}; break the translation to the widget's width. */
		WRAP,
		/** Look up with {@code <br>} kept; keep the translation's breaks and break lines that overflow. */
		KEEP_BR,
		/** Look up each {@code <br>} line on its own and keep the lines as they are. */
		SPLIT_AT_BR,
		/** Look up without {@code <br>}; show the translation on the lines it has. */
		AS_IS
	}

	@AllArgsConstructor
	static final class Request
	{
		final Scope scope;
		final Layout layout;
		/** Interface data keeps color tags in its keys; dialogue and names do not. */
		final boolean keepTags;
		final boolean record;
		/** Set the glyph line height and resize hover boxes after translating (interfaces). */
		final boolean adjust;
		/** A width to give the widget, or 0 to leave it. */
		final int fixedWidth;

		static Request dialogue(Scope scope, boolean wrap)
		{
			return new Request(scope, wrap ? Layout.WRAP : Layout.AS_IS, false, true, false, 0);
		}
	}

	/**
	 * What a widget showed before we touched it and what we set it to. {@code lines} is null when
	 * there was no translation; then {@code output} is the original text.
	 */
	@AllArgsConstructor
	private static final class Shown
	{
		final String original;
		final List<String> lines;
		final int color;
		final String output;
	}

	private final GlyphIcons glyphs;
	private final GameTextLookup lookup;
	private final Widget2ModDict boxes;
	private final Map<Long, Shown> shown = new HashMap<>();

	@Inject
	WidgetTexts(GlyphIcons glyphs, GameTextLookup lookup, Widget2ModDict boxes)
	{
		this.glyphs = glyphs;
		this.lookup = lookup;
		this.boxes = boxes;
	}

	/**
	 * Translates the widget if its text changed. {@code key} identifies the widget (dynamic
	 * children share their parent's id). Returns the widget's original text, or null if it shows
	 * nothing.
	 */
	String handle(Widget widget, long key, Request request)
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
					last = show(widget, last.original, last.lines, color, request);
					shown.put(key, last);
				}
			}
			return last.original;
		}
		List<String> lines = text.contains("<img=") ? null : translate(widget, text, request);
		shown.put(key, lines == null ? new Shown(text, null, 0, text) : show(widget, text, lines, colorOf(widget, text), request));
		return text;
	}

	void reset()
	{
		shown.clear();
	}

	private List<String> translate(Widget widget, String original, Request request)
	{
		String text;
		if (request.layout == Layout.SPLIT_AT_BR)
		{
			List<String> lines = new ArrayList<>();
			for (String line : original.split("<br>", -1))
			{
				String found = line.trim().isEmpty() ? line : find(line, request, false);
				if (found == null)
				{
					return null;
				}
				lines.add(found);
			}
			return lines;
		}
		text = find(original, request, request.layout == Layout.KEEP_BR);
		if (text == null)
		{
			return null;
		}
		FontTypeFace font = widget.getFont();
		boolean wrap = request.layout == Layout.WRAP || request.layout == Layout.KEEP_BR;
		if (!wrap || font == null)
		{
			return Collections.singletonList(text);
		}
		return LineBreaker.wrap(text, widget.getWidth(), cp -> glyphs.width(cp, font));
	}

	private String find(String text, Request request, boolean keepBr)
	{
		return request.keepTags
			? lookup.findKeepingTags(text, request.scope, keepBr, request.record)
			: lookup.find(text, request.scope, request.record);
	}

	private Shown show(Widget widget, String original, List<String> lines, int color, Request request)
	{
		// One markup pass over all lines, so a color tag carries over a line break.
		String output = glyphs.toChatText(String.join("<br>", lines), color, widget.getTextShadowed());
		widget.setText(output);
		if (request.adjust)
		{
			if (output.contains("<img="))
			{
				widget.setLineHeight(GLYPH_LINE_HEIGHT);
			}
			if (request.fixedWidth > 0)
			{
				widget.setWidthMode(WidgetSizeMode.ABSOLUTE).setOriginalWidth(request.fixedWidth).revalidate();
			}
			boxes.resizeWidgetIfNeeded(widget, output);
		}
		return new Shown(original, lines, color, output);
	}

	/** A color tag at the very start of the original wins over the widget's color, as it does in the game. */
	private static int colorOf(Widget widget, String original)
	{
		Matcher m = LEADING_COLOR.matcher(original);
		return m.find() ? Integer.parseInt(m.group(1), 16) : widget.getTextColor();
	}
}
