package com.runetongue.text;

import static org.junit.Assert.assertEquals;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class NormalizerTest
{
	@Test
	public void replacesNumbersAndColorsButLeavesOtherTags()
	{
		SourceText s = Normalizer.normalizeSource(" <col=ff0000>Nex</col> (level-1,001)<br><img=12> ");
		assertEquals("<colNum0>Nex</col> (level-<Num0>)<br><img=12>", s.getKey());
		assertEquals(List.of("1,001"), s.getNumbers());
		assertEquals(List.of("<col=ff0000>"), s.getColors());
	}

	@Test
	public void keepsExistingPlaceholdersAndNumbersAfterThem()
	{
		SourceText s = Normalizer.normalizeSource("<Num0> of 10");
		assertEquals("<Num0> of <Num1>", s.getKey());
	}

	@Test
	public void restoresLiveValuesIntoTranslation()
	{
		SourceText live = Normalizer.normalizeSource("Wave <col=ffff00>3</col> of 7");
		assertEquals("Wave <colNum0><Num0></col> of <Num1>", live.getKey());
		assertEquals("7 중 <col=ffff00>3</col>번째", Normalizer.restore("<Num1> 중 <colNum0><Num0></col>번째", live));
	}

	@Test
	public void numbersNotInSourceStayLiteral()
	{
		SourceText s = Normalizer.normalizeSource("A dozen eggs");
		assertEquals("달걀 12개", Normalizer.normalizeTranslation("달걀 12개", s));
	}

	@Test
	public void findsPlaceholders()
	{
		assertEquals(Set.of("<Num0>", "<colNum0>", "[player name]"),
			Normalizer.placeholders("[player name] has <colNum0><Num0></col> and <Num0>"));
	}
}
