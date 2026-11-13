package com.runehangul.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class TextLookupTest
{
	/** Template lines as they are in data/ko, plus the names they refer to. */
	private static final Translations DATA = new Translations(TranslationTable.EMPTY, new TranslationTable(TsvParser.parse((
		"english\ttranslation\tcategory\tsub_category\tsource\n"
			+ "Name: <!PLAYER_NAME0>\t이름: <!PLAYER_NAME0>\tinterface\tmainTabs\taccountManagementTab\n"
			+ "Monster: <colNum0><!NPC_NAME0></col>\t몬스터: <colNum0><!NPC_NAME0></col>\tinterface\tcombAchvmt\t\n"
			+ "<!ANY_TRANSLATED0> (<Num0>)\t<!ANY_TRANSLATED0> (<Num0>)\tinterface\tcombAchvmt\t\n"
			+ "Giant Mole\tGiant Mole(자이언트 몰)\tname\tnpc\tGiant Mole\n"
			+ "Wintertodt\tWintertodt(윈터토트)\tmanual\tlocation\t\n"
			+ "Filters\t필터\tinterface\tmainTabs\tprayerTab\n").getBytes(StandardCharsets.UTF_8)).getEntries()));

	private static final Scope ACCOUNT = Scope.of("interface", "mainTabs", "accountManagementTab");
	private static final Scope COMBAT_ACHIEVEMENTS = Scope.of("interface", "combAchvmt", "");

	@Test
	public void plainTextNeedsNoTemplate()
	{
		assertEquals("필터", TextLookup.translate(DATA, "Filters", Scope.of("interface", "mainTabs", "prayerTab")));
	}

	@Test
	public void keepsAPlayerNameWithDigits()
	{
		assertEquals("이름: AngryCat9605", TextLookup.translate(DATA, "Name: AngryCat9605", ACCOUNT));
	}

	@Test
	public void translatesNameSlotsAndKeepsColors()
	{
		assertEquals("몬스터: <col=ffffff>Giant Mole(자이언트 몰)</col>",
			TextLookup.translate(DATA, "Monster: <col=ffffff>Giant Mole</col>", COMBAT_ACHIEVEMENTS));
		assertEquals("Wintertodt(윈터토트) (1,234)", TextLookup.translate(DATA, "Wintertodt (1,234)", COMBAT_ACHIEVEMENTS));
	}

	@Test
	public void aSlotWithoutTranslationLeavesTheTextAlone()
	{
		assertNull(TextLookup.translate(DATA, "Monster: <col=ffffff>Nobody</col>", COMBAT_ACHIEVEMENTS));
	}

	@Test
	public void templatesStayInTheirOwnInterface()
	{
		// "<!ANY_TRANSLATED0> (<Num0>)" is for the combat achievements overview only.
		assertNull(TextLookup.translate(DATA, "Wintertodt (5)", Scope.of("interface", "mainTabs", "musicTab")));
	}
}
