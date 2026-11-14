package com.runetongue.glyph;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public final class GlyphRenderer
{
	private static final String BUNDLED_FONT = "/com/runetongue/Galmuri11-subset.ttf";
	private static final float FONT_SIZE = 12f;
	private static final int MIN_HEIGHT = 12;

	private final Font font;
	private final FontRenderContext frc = new FontRenderContext(null, false, false);
	private final int baseline;

	public GlyphRenderer(Font font)
	{
		this.font = font.deriveFont(FONT_SIZE);
		LineMetrics metrics = this.font.getLineMetrics("한", frc);
		this.baseline = Math.round(metrics.getAscent());
	}

	public static GlyphRenderer bundled()
	{
		try (InputStream in = GlyphRenderer.class.getResourceAsStream(BUNDLED_FONT))
		{
			if (in == null)
			{
				throw new IllegalStateException("missing " + BUNDLED_FONT);
			}
			return new GlyphRenderer(Font.createFont(Font.TRUETYPE_FONT, in));
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		catch (FontFormatException e)
		{
			throw new IllegalStateException(e);
		}
	}

	public boolean canDisplay(int codePoint)
	{
		return font.canDisplay(codePoint);
	}

	public BufferedImage render(int codePoint, GlyphColor color)
	{
		return render(codePoint, color.getText(), color.getShadow());
	}

	/** Width in pixels of the image {@link #render} makes for this code point. */
	public int width(int codePoint)
	{
		return width(font.createGlyphVector(frc, new String(Character.toChars(codePoint))));
	}

	/** Draws one glyph; {@code shadow} may be null. */
	public BufferedImage render(int codePoint, Color textColor, Color shadow)
	{
		String text = new String(Character.toChars(codePoint));
		GlyphVector glyph = font.createGlyphVector(frc, text);
		int width = width(glyph);
		int bottom = (int) Math.ceil(baseline + glyph.getVisualBounds().getMaxY());
		int height = Math.max(MIN_HEIGHT, bottom);

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
			g.setFont(font);
			if (shadow != null)
			{
				g.setColor(shadow);
				g.drawString(text, 1, baseline + 1);
			}
			g.setColor(textColor);
			g.drawString(text, 0, baseline);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	private static int width(GlyphVector glyph)
	{
		return Math.max(1, (int) Math.ceil(glyph.getGlyphMetrics(0).getAdvanceX()));
	}
}
