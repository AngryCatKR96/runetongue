package com.runehangul.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class TranslationsTest
{
	private static TranslationTable table(String text)
	{
		return new TranslationTable(TsvParser.parse(text.getBytes(StandardCharsets.UTF_8)).getEntries());
	}

	private static Translations bundledOnly(String text)
	{
		return new Translations(TranslationTable.EMPTY, table(text));
	}

	private static final Scope HANS = Scope.of("dialogue", "Hans", "Hans");

	@Test
	public void mostSpecificScopeWinsWithinALayer()
	{
		Translations t = bundledOnly("Hello.\t안녕.\nHello.\t안녕하시오.\tdialogue\tHans\nHello.\t안녕하세요.\tactions\n");
		assertEquals("안녕하시오.", t.find("Hello.", HANS));
		assertEquals("안녕.", t.find("Hello.", Scope.of("dialogue", "Bob", "Bob")));
		assertEquals("안녕하세요.", t.find("Hello.", Scope.of("actions", "", "")));
		assertNull(t.find("Bye.", HANS));
	}

	@Test
	public void aLookupWithoutScopeFindsCategorizedLines()
	{
		Translations t = bundledOnly("Study\t연구하기\tactions\titem\tFire\n");
		assertEquals("연구하기", t.find("Study", Scope.ANY));
		assertEquals("연구하기", t.find("Study", Scope.of("actions", "item", "Bonfire")));
	}

	@Test
	public void looserMatchesStayInTheSameCategoryFirst()
	{
		Translations t = bundledOnly(
			"Attack\t공격!\tdialogue\tGuard\tGuard\n"
				+ "Attack\t공격하기\tactions\tnpc\tMan\n"
				+ "Hello.\t안녕하시오.\tdialogue\tHans\tHans\n");
		assertEquals("공격하기", t.find("Attack", Scope.of("actions", "npc", "Goblin")));
		assertEquals("공격!", t.find("Attack", Scope.of("dialogue", "Bob", "Bob")));
		assertEquals("안녕하시오.", t.find("Hello.", Scope.of("dialogue", "Hans", "Player")));
	}

	@Test
	public void aUserLineWithoutCategoryBeatsTheBundledData()
	{
		TranslationTable bundled = table("Talk-to\t대화하기\tactions\tnpc\tMan\n");
		Translations t = new Translations(table("Talk-to\t말 걸기\n"), bundled);
		assertEquals("말 걸기", t.find("Talk-to", Scope.of("actions", "npc", "Man")));
		assertEquals("말 걸기", t.find("Talk-to", Scope.ANY));
	}

	@Test
	public void aUserLineForOneCategoryDoesNotOverrideAnother()
	{
		TranslationTable bundled = table("Attack\t공격하기\tactions\tnpc\tMan\n");
		Translations t = new Translations(table("Attack\t공격!\tdialogue\n"), bundled);
		assertEquals("공격하기", t.find("Attack", Scope.of("actions", "npc", "Man")));
		assertEquals("공격!", t.find("Attack", Scope.of("dialogue", "Bob", "Bob")));
	}

	@Test
	public void laterFileWins()
	{
		List<Entry> both = new ArrayList<>(TsvParser.parse("Talk-to\t말 걸기\n".getBytes(StandardCharsets.UTF_8)).getEntries());
		both.addAll(TsvParser.parse("Talk-to\t대화하기\n".getBytes(StandardCharsets.UTF_8)).getEntries());
		assertEquals("대화하기", new Translations(new TranslationTable(both), TranslationTable.EMPTY).find("Talk-to", Scope.ANY));
	}
}
