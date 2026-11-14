package com.runetongue.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class TsvParserTest
{
	private static ParseResult parse(String text)
	{
		return TsvParser.parse(text.getBytes(StandardCharsets.UTF_8));
	}

	private static List<String> pairs(ParseResult result)
	{
		return result.getEntries().stream()
			.map(e -> e.getEnglish() + " => " + e.getTranslation())
			.collect(Collectors.toList());
	}

	private static List<String> skips(ParseResult result)
	{
		return result.getSkipped().stream()
			.map(s -> s.getLine() + ":" + s.getReason())
			.collect(Collectors.toList());
	}

	@Test
	public void readsTwoColumnLinesWithoutHeader()
	{
		ParseResult r = parse("Talk-to\t대화하기\nHello there!\t안녕하세요!\n");
		assertEquals(List.of("Talk-to => 대화하기", "Hello there! => 안녕하세요!"), pairs(r));
		assertEquals(Scope.ANY, r.getEntries().get(0).getScope());
		assertEquals(List.of(), skips(r));
	}

	@Test
	public void headerSetsColumnOrderAndOptionalColumnsMayBeMissing()
	{
		ParseResult r = parse("category\ttranslation\tenglish\nactions\t대화하기\tTalk-to\n");
		assertEquals(List.of("Talk-to => 대화하기"), pairs(r));
		assertEquals(Scope.of("actions", "", ""), r.getEntries().get(0).getScope());

		ParseResult partial = parse("Talk-to\t대화하기\tactions\n");
		assertEquals(Scope.of("actions", "", ""), partial.getEntries().get(0).getScope());
	}

	@Test
	public void rawNumbersBecomePlaceholdersMatchedByValue()
	{
		ParseResult r = parse("Wave 5 of 10\t10 중 5번째 웨이브\nYou have 1,234 coins.\t코인 <Num0>개\n");
		assertEquals(List.of(
			"Wave <Num0> of <Num1> => <Num1> 중 <Num0>번째 웨이브",
			"You have <Num0> coins. => 코인 <Num0>개"), pairs(r));
	}

	@Test
	public void skipsPlaceholderMismatchButKeepsOtherLines()
	{
		ParseResult r = parse("You have 5 coins.\t코인이 있어요.\n"
			+ "Hi [player name].\t안녕.\n"
			+ "Hi [player name].\t안녕, [player name].\n");
		assertEquals(List.of("1:PLACEHOLDER_MISMATCH", "2:PLACEHOLDER_MISMATCH"), skips(r));
		assertEquals(List.of("Hi [player name]. => 안녕, [player name]."), pairs(r));
	}

	@Test
	public void emptyTranslationCountsAsUntranslatedNotAnError()
	{
		ParseResult r = parse("english\ttranslation\tcategory\nTalk-to\t\tactions\nAttack\t공격하기\tactions\n");
		assertEquals(1, r.getUntranslated());
		assertEquals(List.of(), skips(r));
		assertEquals(List.of("Attack => 공격하기"), pairs(r));
	}

	@Test
	public void laterDuplicateWinsAndEarlierIsReported()
	{
		ParseResult r = parse("Talk-to\t말 걸기\nTalk-to\t대화하기\nTalk-to\t대화\tactions\n");
		assertEquals(List.of("Talk-to => 대화하기", "Talk-to => 대화"), pairs(r));
		assertEquals(List.of("1:OVERRIDDEN"), skips(r));
	}

	@Test
	public void linesThatNormalizeToTheSameTranslationAreNotReported()
	{
		ParseResult r = parse("Take-1\t1개 가져가기\nTake-5\t5개 가져가기\n");
		assertEquals(List.of("Take-<Num0> => <Num0>개 가져가기"), pairs(r));
		assertEquals(List.of(), skips(r));
	}

	@Test
	public void badUtf8SkipsOnlyThatLine()
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		bytes.writeBytes("Talk-to\t대화하기\n".getBytes(StandardCharsets.UTF_8));
		bytes.writeBytes("Attack\t".getBytes(StandardCharsets.UTF_8));
		bytes.writeBytes(new byte[]{(byte) 0xB0, (byte) 0xF8, (byte) 0xB0, (byte) 0xDD});
		bytes.writeBytes("\nUse\t사용하기\n".getBytes(StandardCharsets.UTF_8));
		ParseResult r = TsvParser.parse(bytes.toByteArray());
		assertEquals(List.of("2:BAD_ENCODING"), skips(r));
		assertEquals(List.of("Talk-to => 대화하기", "Use => 사용하기"), pairs(r));
	}

	@Test
	public void acceptsUtf8BomAndUtf16FromExcel()
	{
		byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
		byte[] body = "english\ttranslation\nTalk-to\t대화하기\n".getBytes(StandardCharsets.UTF_8);
		ByteArrayOutputStream utf8 = new ByteArrayOutputStream();
		utf8.writeBytes(bom);
		utf8.writeBytes(body);
		assertEquals(List.of("Talk-to => 대화하기"), pairs(TsvParser.parse(utf8.toByteArray())));

		ByteArrayOutputStream utf16 = new ByteArrayOutputStream();
		utf16.writeBytes(new byte[]{(byte) 0xFF, (byte) 0xFE});
		utf16.writeBytes("english\ttranslation\r\nTalk-to\t대화하기\r\n".getBytes(StandardCharsets.UTF_16LE));
		assertEquals(List.of("Talk-to => 대화하기"), pairs(TsvParser.parse(utf16.toByteArray())));
	}

	@Test
	public void unquotesExcelEscapedCells()
	{
		ParseResult r = parse("\"\"\"Cabbage!\"\" he says.\"\t\"\"\"양배추!\"\" 그가 말했다.\"\n\"Hi\"\t\"안녕\"\n");
		assertEquals(List.of("\"Cabbage!\" he says. => \"양배추!\" 그가 말했다.", "\"Hi\" => \"안녕\""), pairs(r));
	}

	@Test
	public void expandsChoicesPairwise()
	{
		ParseResult r = parse("Hello, [man/woman]! Thanks, [sir/madam].\t안녕, [젊은이/아가씨]! 고마워요, [선생님/부인].\n"
			+ "Good day, [sir/madam].\t안녕하세요.\n");
		assertEquals(List.of(
			"Hello, man! Thanks, sir. => 안녕, 젊은이! 고마워요, 선생님.",
			"Hello, woman! Thanks, madam. => 안녕, 아가씨! 고마워요, 부인.",
			"Good day, sir. => 안녕하세요.",
			"Good day, madam. => 안녕하세요."), pairs(r));
	}

	@Test
	public void skipsChoiceCountMismatch()
	{
		ParseResult r = parse("Hello, [man/woman].\t안녕, [젊은이/아가씨/어르신].\nHi [a/b] and [c/d/e].\t안녕.\n");
		assertEquals(List.of("1:CHOICE_MISMATCH", "2:CHOICE_MISMATCH"), skips(r));
	}

	@Test
	public void skipsMalformedLines()
	{
		ParseResult r = parse("only one column\n\t번역만 있음\n\n");
		assertEquals(List.of("1:TOO_FEW_COLUMNS", "2:EMPTY_ENGLISH"), skips(r));
		assertTrue(r.getEntries().isEmpty());
	}
}
