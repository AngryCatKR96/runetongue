package com.runehangul.glyph;

import java.awt.image.BufferedImage;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.ChatIconManager;

/**
 * Turns text into chat markup the game can draw, replacing characters its fonts lack with
 * {@code <img=N>} tags. Glyph images are drawn and registered on first use. Chat icons cannot be
 * unregistered, so this is a singleton that keeps them across plugin restarts.
 * Must be called on the client thread.
 */
@Singleton
public class GlyphIcons
{
	private final ChatIconManager chatIconManager;
	private final GlyphRenderer renderer;
	private final CharsetEncoder gameCharset = Charset.forName("windows-1252").newEncoder();
	private final Map<Long, Integer> iconIds = new HashMap<>();

	@Inject
	GlyphIcons(ChatIconManager chatIconManager)
	{
		this.chatIconManager = chatIconManager;
		this.renderer = GlyphRenderer.bundled();
	}

	public String toChatText(String text, GlyphColor color)
	{
		StringBuilder out = new StringBuilder(text.length() * 8);
		text.codePoints().forEach(cp ->
		{
			if (cp < 0x10000 && gameCharset.canEncode((char) cp))
			{
				out.appendCodePoint(cp);
			}
			else if (renderer.canDisplay(cp))
			{
				out.append("<img=").append(chatIconManager.chatIconIndex(iconId(cp, color))).append('>');
			}
			else
			{
				out.append('?');
			}
		});
		return out.toString();
	}

	/** Draws one glyph without registering it; safe on any thread. */
	public BufferedImage image(int codePoint, GlyphColor color)
	{
		return renderer.render(codePoint, color);
	}

	private int iconId(int codePoint, GlyphColor color)
	{
		long key = ((long) codePoint << 8) | color.ordinal();
		return iconIds.computeIfAbsent(key, k ->
		{
			int id = chatIconManager.reserveChatIcon();
			chatIconManager.updateChatIcon(id, renderer.render(codePoint, color));
			return id;
		});
	}
}
