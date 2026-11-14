package com.runetongue.glyph;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.FontTypeFace;
import net.runelite.client.game.ChatIconManager;

/**
 * Turns text into markup the game can draw, replacing characters its fonts lack with
 * {@code <img=N>} tags. Glyph images are drawn and registered on first use. Chat icons cannot be
 * unregistered, so this is a singleton that keeps them across plugin restarts.
 * Must be called on the client thread.
 */
@Singleton
public class GlyphIcons
{
	/** ImageUtil.getImageIndexedSprite turns opaque 0x000000 into the transparent palette slot. */
	private static final int NEAR_BLACK = 0x010101;
	private static final Color SHADOW = new Color(NEAR_BLACK);

	private final ChatIconManager chatIconManager;
	private final GlyphRenderer renderer;
	private final CharsetEncoder gameCharset = Charset.forName("windows-1252").newEncoder();
	private final Map<Long, Integer> iconIds = new HashMap<>();
	private final Map<Integer, Integer> widths = new HashMap<>();

	@Inject
	GlyphIcons(ChatIconManager chatIconManager)
	{
		this.chatIconManager = chatIconManager;
		this.renderer = GlyphRenderer.bundled();
	}

	public String toChatText(String text, GlyphColor color)
	{
		return toMarkup(text, color.getText(), color.getShadow());
	}

	/** Markup for a widget whose text is drawn in {@code rgb}, with the game's drop shadow if {@code shadowed}. */
	public String toChatText(String text, int rgb, boolean shadowed)
	{
		int visible = (rgb & 0xFFFFFF) == 0 ? NEAR_BLACK : rgb & 0xFFFFFF;
		return toMarkup(text, new Color(visible), shadowed ? SHADOW : null);
	}

	/** Width in pixels of one character as {@link #toChatText} would draw it in {@code font}. */
	public int width(int codePoint, FontTypeFace font)
	{
		if (isGameChar(codePoint))
		{
			return font.getTextWidth(new String(Character.toChars(codePoint)));
		}
		if (renderer.canDisplay(codePoint))
		{
			return widths.computeIfAbsent(codePoint, renderer::width);
		}
		return font.getTextWidth("?");
	}

	/** Draws one glyph without registering it; safe on any thread. */
	public BufferedImage image(int codePoint, GlyphColor color)
	{
		return renderer.render(codePoint, color);
	}

	/**
	 * Tags pass through unchanged; a {@code <col=rrggbb>} tag switches the color glyphs are drawn
	 * in until its {@code </col>}, as the game does for its own font.
	 */
	private String toMarkup(String text, Color textColor, Color shadow)
	{
		StringBuilder out = new StringBuilder(text.length() * 8);
		Color color = textColor;
		for (int i = 0; i < text.length(); )
		{
			int cp = text.codePointAt(i);
			int end = cp == '<' ? text.indexOf('>', i) : -1;
			if (end > 0)
			{
				String tag = text.substring(i, end + 1);
				out.append(tag);
				if (tag.startsWith("<col=") && tag.length() > 6)
				{
					color = colorOf(tag.substring(5, tag.length() - 1), color);
				}
				else if (tag.equals("</col>"))
				{
					color = textColor;
				}
				i = end + 1;
				continue;
			}
			if (isGameChar(cp))
			{
				out.appendCodePoint(cp);
			}
			else if (renderer.canDisplay(cp))
			{
				out.append("<img=").append(chatIconManager.chatIconIndex(iconId(cp, color, shadow))).append('>');
			}
			else
			{
				out.append('?');
			}
			i += Character.charCount(cp);
		}
		return out.toString();
	}

	private static Color colorOf(String hex, Color fallback)
	{
		try
		{
			int rgb = Integer.parseInt(hex, 16) & 0xFFFFFF;
			return new Color(rgb == 0 ? NEAR_BLACK : rgb);
		}
		catch (NumberFormatException e)
		{
			return fallback;
		}
	}

	private boolean isGameChar(int codePoint)
	{
		return codePoint < 0x10000 && gameCharset.canEncode((char) codePoint);
	}

	private int iconId(int codePoint, Color textColor, Color shadow)
	{
		long key = ((long) codePoint << 25) | ((long) (textColor.getRGB() & 0xFFFFFF) << 1) | (shadow != null ? 1 : 0);
		return iconIds.computeIfAbsent(key, k ->
		{
			int id = chatIconManager.reserveChatIcon();
			chatIconManager.updateChatIcon(id, renderer.render(codePoint, textColor, shadow));
			return id;
		});
	}
}
