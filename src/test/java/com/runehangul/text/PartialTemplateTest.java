package com.runehangul.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Map;
import org.junit.Test;

public class PartialTemplateTest
{
	@Test
	public void fillsSlotsAroundFixedText()
	{
		PartialTemplate t = PartialTemplate.of("Name: <!PLAYER_NAME0>");
		assertEquals(Map.of("PLAYER_NAME0", "Zezima 2"), t.match("Name: Zezima 2"));
		assertNull(t.match("Username: Zezima"));
	}

	@Test
	public void colorTagsAndNumbersMatchAnyValue()
	{
		PartialTemplate monster = PartialTemplate.of("Monster: <colNum0><!NPC_NAME0></col>");
		assertEquals(Map.of("NPC_NAME0", "Giant Mole"), monster.match("Monster: <col=ffffff>Giant Mole</col>"));

		PartialTemplate boss = PartialTemplate.of("<!ANY_TRANSLATED0> (<Num0>)");
		assertEquals(Map.of("ANY_TRANSLATED0", "Wintertodt"), boss.match("Wintertodt (1,234)"));
	}

	@Test
	public void unfillGivesTheTemplateKeyAfterNormalizing()
	{
		// A player name with digits must not turn into a <Num> placeholder of the template.
		PartialTemplate t = PartialTemplate.of("Combat Profile - <!PLAYER_NAME0>");
		String shown = "Combat Profile - AngryCat9605";
		String unfilled = t.unfill(shown, t.match(shown));
		assertEquals("Combat Profile - <!PLAYER_NAME0>", unfilled);
		assertEquals(t.getKey(), Normalizer.normalizeSource(unfilled).getKey());

		PartialTemplate boss = PartialTemplate.of("<!ANY_TRANSLATED0> (<Num0>)");
		String bossShown = "Zulrah (25)";
		assertEquals(boss.getKey(), Normalizer.normalizeSource(boss.unfill(bossShown, boss.match(bossShown))).getKey());
	}

	@Test
	public void slotTypes()
	{
		assertEquals("NPC_NAME", PartialTemplate.typeOf("NPC_NAME0"));
		assertEquals("ANY_TRANSLATED", PartialTemplate.typeOf("ANY_TRANSLATED12"));
	}
}
