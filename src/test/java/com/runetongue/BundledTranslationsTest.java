package com.runetongue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import com.runetongue.text.Entry;
import com.runetongue.text.TsvParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.junit.Test;

/**
 * The bundled translations are committed (the Plugin Hub builds with its own build.gradle, which
 * would skip the bundler), so this fails when data/ko was edited without running
 * {@code ./gradlew bundleTranslations}.
 */
public class BundledTranslationsTest
{
	@Test
	public void bundleMatchesTheDataFolder() throws IOException
	{
		Map<String, String> fromData = new LinkedHashMap<>();
		List<Path> files;
		try (Stream<Path> paths = Files.list(Paths.get("data/ko")))
		{
			files = paths.filter(p -> p.toString().endsWith(".tsv")).sorted().collect(Collectors.toList());
		}
		for (Path file : files)
		{
			addAll(fromData, TsvParser.parse(Files.readAllBytes(file)).getEntries());
		}

		Map<String, String> fromBundle = new LinkedHashMap<>();
		try (InputStream raw = BundledTranslationsTest.class.getResourceAsStream(Language.KOREAN.getBundledResource()))
		{
			assertNotNull("missing " + Language.KOREAN.getBundledResource(), raw);
			try (InputStream in = new GZIPInputStream(raw))
			{
				addAll(fromBundle, TsvParser.parse(in.readAllBytes()).getEntries());
			}
		}

		assertEquals("run ./gradlew bundleTranslations after editing data/ko", "", firstDifference(fromData, fromBundle));
	}

	/** A short description of the first line that differs, so a failure does not print 125,000 lines. */
	private static String firstDifference(Map<String, String> data, Map<String, String> bundle)
	{
		for (Map.Entry<String, String> e : data.entrySet())
		{
			String bundled = bundle.get(e.getKey());
			if (!e.getValue().equals(bundled))
			{
				return e.getKey().replace('\u0000', ' ') + ": data has \"" + e.getValue() + "\", bundle has \"" + bundled + "\"";
			}
		}
		return data.size() == bundle.size() ? "" : "bundle has " + (bundle.size() - data.size()) + " lines that data does not";
	}

	/** Later lines win, as they do when the plugin builds its lookup table. */
	private static void addAll(Map<String, String> into, List<Entry> entries)
	{
		for (Entry e : entries)
		{
			into.put(e.getScope() + "\u0000" + e.getEnglish(), e.getTranslation());
		}
	}
}
