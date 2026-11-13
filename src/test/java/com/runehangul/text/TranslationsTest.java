package com.runehangul.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class TranslationsTest
{
	private static List<Entry> file(String text)
	{
		return TsvParser.parse(text.getBytes(StandardCharsets.UTF_8)).getEntries();
	}

	private static final Scope DIALOGUE = Scope.of("dialogue", "Hans", "Hans");

	@Test
	public void mostSpecificScopeWinsWithinALayer()
	{
		TranslationTable t = new TranslationTable(file("Hello.\t안녕.\nHello.\t안녕하시오.\tdialogue\tHans\nHello.\t안녕하세요.\tactions\n"));
		assertEquals("안녕하시오.", t.find("Hello.", DIALOGUE));
		assertEquals("안녕.", t.find("Hello.", Scope.of("dialogue", "Bob", "Bob")));
		assertEquals("안녕하세요.", t.find("Hello.", Scope.of("actions", "", "")));
		assertNull(t.find("Bye.", DIALOGUE));
	}

	@Test
	public void anyUserLineBeatsTheBundledData()
	{
		TranslationTable bundled = new TranslationTable(file("Talk-to\t말 걸기\tactions\tnpc\tMan\n"));
		TranslationTable user = new TranslationTable(file("Talk-to\t대화하기\n"));
		Translations translations = new Translations(user, bundled);
		assertEquals("대화하기", translations.find("Talk-to", Scope.of("actions", "npc", "Man")));
		assertEquals("말 걸기", new Translations(TranslationTable.EMPTY, bundled).find("Talk-to", Scope.of("actions", "npc", "Man")));
	}

	@Test
	public void laterFileWins()
	{
		List<Entry> both = new ArrayList<>(file("Talk-to\t말 걸기\n"));
		both.addAll(file("Talk-to\t대화하기\n"));
		assertEquals("대화하기", new TranslationTable(both).find("Talk-to", Scope.ANY));
	}
}
