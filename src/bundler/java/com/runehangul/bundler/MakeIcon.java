package com.runehangul.bundler;

import com.runehangul.glyph.GlyphColor;
import com.runehangul.glyph.GlyphRenderer;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Build-time tool: draws the Plugin Hub icon (icon.png, at most 48x72) from the same glyph the
 * side panel button uses, "한" in orange, scaled up with nearest-neighbour so it stays a pixel font.
 */
public final class MakeIcon
{
	private static final int WIDTH = 48;
	private static final int HEIGHT = 72;
	private static final int SCALE = 4;

	private MakeIcon()
	{
	}

	public static void main(String[] args) throws IOException
	{
		BufferedImage glyph = GlyphRenderer.bundled().render('한', GlyphColor.ORANGE);
		BufferedImage icon = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		int w = glyph.getWidth() * SCALE;
		int h = glyph.getHeight() * SCALE;
		g.drawImage(glyph, (WIDTH - w) / 2, (HEIGHT - h) / 2, w, h, null);
		g.dispose();
		File out = new File(args[0]);
		ImageIO.write(icon, "png", out);
		System.out.println("wrote " + out + " (" + WIDTH + "x" + HEIGHT + ")");
	}
}
