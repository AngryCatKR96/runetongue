package com.runehangul.text;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reads translation tsv files: the bundled data, hand-made files and filled-in exports alike.
 * Bad lines are skipped and reported; the rest of the file still loads.
 */
public final class TsvParser
{
	private static final List<String> DEFAULT_COLUMNS = Arrays.asList("english", "translation", "category", "sub_category", "source");
	private static final char REPLACEMENT = '�';

	private TsvParser()
	{
	}

	public static ParseResult parse(byte[] bytes)
	{
		String[] lines = decode(bytes).split("\r?\n", -1);
		List<String> columns = DEFAULT_COLUMNS;
		int first = 0;
		List<String> header = new ArrayList<>();
		for (String name : lines[0].split("\t", -1))
		{
			header.add(name.trim().toLowerCase(Locale.ROOT));
		}
		if (header.contains("english") && header.contains("translation"))
		{
			columns = header;
			first = 1;
		}
		Columns col = new Columns(columns);

		Map<String, Entry> entries = new LinkedHashMap<>();
		List<ParseResult.Skipped> skipped = new ArrayList<>();
		Set<Integer> overriddenLines = new HashSet<>();
		int untranslated = 0;
		for (int i = first; i < lines.length; i++)
		{
			int lineNo = i + 1;
			String line = lines[i];
			if (line.trim().isEmpty())
			{
				continue;
			}
			if (line.indexOf(REPLACEMENT) >= 0)
			{
				skipped.add(new ParseResult.Skipped(lineNo, SkipReason.BAD_ENCODING, ""));
				continue;
			}
			String[] cells = line.split("\t", -1);
			if (cells.length < 2)
			{
				skipped.add(new ParseResult.Skipped(lineNo, SkipReason.TOO_FEW_COLUMNS, line));
				continue;
			}
			String english = col.get(cells, "english");
			String translation = col.get(cells, "translation");
			if (english.isEmpty())
			{
				skipped.add(new ParseResult.Skipped(lineNo, SkipReason.EMPTY_ENGLISH, line));
				continue;
			}
			if (translation.isEmpty())
			{
				untranslated++;
				continue;
			}
			Scope scope = Scope.of(col.get(cells, "category"), col.get(cells, "sub_category"), col.get(cells, "source"));

			List<Entry> lineEntries = new ArrayList<>();
			ParseResult.Skipped problem = expand(english, translation, scope, lineNo, lineEntries);
			if (problem != null)
			{
				skipped.add(problem);
				continue;
			}
			for (Entry e : lineEntries)
			{
				Entry previous = entries.put(e.getEnglish() + '\t' + scope, e);
				if (previous != null && previous.getLine() != lineNo
					&& !previous.getTranslation().equals(e.getTranslation())
					&& overriddenLines.add(previous.getLine()))
				{
					skipped.add(new ParseResult.Skipped(previous.getLine(), SkipReason.OVERRIDDEN, lineNo + "번째 줄"));
				}
			}
		}
		skipped.sort((a, b) -> Integer.compare(a.getLine(), b.getLine()));
		return new ParseResult(new ArrayList<>(entries.values()), skipped, untranslated);
	}

	private static ParseResult.Skipped expand(String english, String translation, Scope scope, int lineNo, List<Entry> out)
	{
		int count = Choices.count(english);
		int translationCount = Choices.count(translation);
		if (count < 0 || translationCount < 0 || (translationCount != 1 && translationCount != count))
		{
			return new ParseResult.Skipped(lineNo, SkipReason.CHOICE_MISMATCH, english + " / " + translation);
		}
		List<String> englishVariants = Choices.expand(english, count);
		List<String> translationVariants = Choices.expand(translation, count);
		for (int k = 0; k < count; k++)
		{
			SourceText source = Normalizer.normalizeSource(englishVariants.get(k));
			String normalized = Normalizer.normalizeTranslation(translationVariants.get(k), source);
			Set<String> expected = Normalizer.placeholders(source.getKey());
			Set<String> actual = Normalizer.placeholders(normalized);
			if (!expected.equals(actual))
			{
				return new ParseResult.Skipped(lineNo, SkipReason.PLACEHOLDER_MISMATCH, "원문 " + expected + ", 번역 " + actual);
			}
			out.add(new Entry(source.getKey(), normalized, scope, lineNo));
		}
		return null;
	}

	private static String decode(byte[] bytes)
	{
		Charset charset = StandardCharsets.UTF_8;
		int offset = 0;
		if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF)
		{
			offset = 3;
		}
		else if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE)
		{
			charset = StandardCharsets.UTF_16LE;
			offset = 2;
		}
		else if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF)
		{
			charset = StandardCharsets.UTF_16BE;
			offset = 2;
		}
		CharsetDecoder decoder = charset.newDecoder()
			.onMalformedInput(CodingErrorAction.REPLACE)
			.onUnmappableCharacter(CodingErrorAction.REPLACE)
			.replaceWith(String.valueOf(REPLACEMENT));
		try
		{
			return decoder.decode(ByteBuffer.wrap(bytes, offset, bytes.length - offset)).toString();
		}
		catch (CharacterCodingException e)
		{
			throw new IllegalStateException("decoder set to REPLACE cannot fail", e);
		}
	}

	private static String cell(String[] cells, int index)
	{
		if (index < 0 || index >= cells.length)
		{
			return "";
		}
		String value = cells[index].trim();
		if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"") && value.contains("\"\""))
		{
			value = value.substring(1, value.length() - 1).replace("\"\"", "\"");
		}
		return value;
	}

	private static final class Columns
	{
		private final List<String> names;

		Columns(List<String> names)
		{
			this.names = names;
		}

		String get(String[] cells, String name)
		{
			return cell(cells, names.indexOf(name));
		}
	}
}
