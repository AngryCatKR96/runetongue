package com.runehangul.text;

import static org.junit.Assert.assertEquals;
import java.util.List;
import java.util.function.IntUnaryOperator;
import org.junit.Test;

public class LineBreakerTest
{
	/** Hangul 12px, everything else 6px, like a game font next to the glyph images. */
	private static final IntUnaryOperator WIDTH = cp -> cp >= 0xAC00 && cp <= 0xD7A3 ? 12 : 6;

	@Test
	public void breaksAtSpacesUsingRealWidths()
	{
		// "가나다" = 36, " " = 6, "ab" = 12: 36 + 6 + 12 = 54 fits in 54, the next word does not.
		assertEquals(List.of("가나다 ab", "라마"), LineBreaker.wrap("가나다 ab 라마", 54, WIDTH));
	}

	@Test
	public void mixedTextIsNotBrokenEarly()
	{
		// 12px per character would count 228px; the line is really 126 + 6 + 24 = 156px.
		assertEquals(List.of("Lumbridge(럼브리지)에 가요"), LineBreaker.wrap("Lumbridge(럼브리지)에 가요", 160, WIDTH));
	}

	@Test
	public void keepsForcedBreaks()
	{
		assertEquals(List.of("가나", "다라"), LineBreaker.wrap("가나<br>다라", 500, WIDTH));
	}

	@Test
	public void breaksAWordWiderThanTheLine()
	{
		assertEquals(List.of("가나다", "라마"), LineBreaker.wrap("가나다라마", 36, WIDTH));
		assertEquals(List.of("ab", "가나다", "라마"), LineBreaker.wrap("ab 가나다라마", 36, WIDTH));
	}

	@Test
	public void tagsTakeNoWidthAndStayWhole()
	{
		assertEquals(24, LineBreaker.width("<col=ff0000>가나</col>", WIDTH));
		assertEquals(List.of("<col=ff0000>가나", "다</col>"), LineBreaker.wrap("<col=ff0000>가나다</col>", 24, WIDTH));
	}
}
