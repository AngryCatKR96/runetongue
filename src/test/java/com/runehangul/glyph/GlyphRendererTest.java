package com.runehangul.glyph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.Test;

/**
 * The reference PNGs come from RuneLingual's update_char_images.py, which the Korean glyphs
 * were tuned against in game. Its shadows are pure black, which never showed in game, so the
 * comparison maps them to the shadow color we draw.
 */
public class GlyphRendererTest
{
	private static final String CHARS = "한뷁g,yW";

	private final GlyphRenderer renderer = GlyphRenderer.bundled();

	@Test
	public void matchesRuneLingualImages() throws IOException
	{
		for (GlyphColor color : GlyphColor.values())
		{
			for (int cp : CHARS.codePoints().toArray())
			{
				String name = color.name().toLowerCase() + "--" + cp + ".png";
				assertSamePixels(name, reference(name), renderer.render(cp, color));
			}
		}
	}

	@Test
	public void bundledFontCoversOnlyTheSubset()
	{
		assertTrue(renderer.canDisplay('한'));
		assertFalse(renderer.canDisplay('똠'));
	}

	private static BufferedImage reference(String name) throws IOException
	{
		try (InputStream in = GlyphRendererTest.class.getResourceAsStream(name))
		{
			assertNotNull(name, in);
			return ImageIO.read(in);
		}
	}

	private static void assertSamePixels(String name, BufferedImage expected, BufferedImage actual)
	{
		assertEquals(name + " width", expected.getWidth(), actual.getWidth());
		assertEquals(name + " height", expected.getHeight(), actual.getHeight());
		for (int y = 0; y < expected.getHeight(); y++)
		{
			for (int x = 0; x < expected.getWidth(); x++)
			{
				assertEquals(name + " at " + x + "," + y, visible(expected.getRGB(x, y)), visible(actual.getRGB(x, y)));
			}
		}
	}

	private static int visible(int argb)
	{
		if ((argb >>> 24) == 0)
		{
			return 0;
		}
		return argb == 0xFF000000 ? GlyphColor.YELLOW.getShadow().getRGB() : argb;
	}

	@Test
	public void noOpaquePureBlackPixels()
	{
		for (GlyphColor color : GlyphColor.values())
		{
			BufferedImage image = renderer.render('한', color);
			for (int y = 0; y < image.getHeight(); y++)
			{
				for (int x = 0; x < image.getWidth(); x++)
				{
					assertTrue(color + " at " + x + "," + y + " would turn transparent in game", image.getRGB(x, y) != 0xFF000000);
				}
			}
		}
	}
}
