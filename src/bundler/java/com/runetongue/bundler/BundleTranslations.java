package com.runetongue.bundler;

import com.runetongue.text.Entry;
import com.runetongue.text.ParseResult;
import com.runetongue.text.SkipReason;
import com.runetongue.text.TsvParser;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

/**
 * Build-time tool: turns {@code data/<lang>/*.tsv} into {@code com/runetongue/translations_<lang>.tsv.gz}.
 * Each file goes through the plugin's own parser; lines it would skip at runtime are left out of
 * the bundle and listed in {@code <reports>/<lang>-skipped.tsv}. Files are concatenated in name
 * order, so a later file still wins over an earlier one, as it does in the user folder.
 */
public final class BundleTranslations
{
	private BundleTranslations()
	{
	}

	public static void main(String[] args) throws IOException
	{
		Path dataRoot = Paths.get(args[0]);
		Path resourcesRoot = Paths.get(args[1]);
		Path reportRoot = Paths.get(args[2]);
		Files.createDirectories(resourcesRoot.resolve("com/runetongue"));
		Files.createDirectories(reportRoot);
		for (Path languageDir : list(dataRoot, Files::isDirectory))
		{
			String language = languageDir.getFileName().toString();
			bundle(language, list(languageDir, p -> p.toString().endsWith(".tsv")),
				resourcesRoot.resolve("com/runetongue/translations_" + language + ".tsv.gz"),
				reportRoot.resolve(language + "-skipped.tsv"));
		}
	}

	private static void bundle(String language, List<Path> files, Path out, Path report) throws IOException
	{
		String header = null;
		int kept = 0;
		int untranslated = 0;
		Map<SkipReason, Integer> skipCounts = new EnumMap<>(SkipReason.class);
		try (Writer bundle = gzipWriter(out);
			BufferedWriter skips = Files.newBufferedWriter(report, StandardCharsets.UTF_8))
		{
			skips.write("file\tline\treason\tdetail\tlineText\n");
			for (Path file : files)
			{
				byte[] bytes = Files.readAllBytes(file);
				String[] lines = new String(bytes, StandardCharsets.UTF_8).split("\r?\n", -1);
				if (header == null)
				{
					header = lines[0];
					bundle.write(header);
					bundle.write('\n');
				}
				else if (!header.equals(lines[0]))
				{
					throw new IllegalStateException(file + ": header differs from the first file's: " + lines[0]);
				}

				ParseResult result = TsvParser.parse(bytes);
				Set<Integer> keep = result.getEntries().stream().map(Entry::getLine).collect(Collectors.toSet());
				for (int i = 1; i < lines.length; i++)
				{
					if (keep.contains(i + 1))
					{
						bundle.write(lines[i]);
						bundle.write('\n');
						kept++;
					}
				}
				for (ParseResult.Skipped s : result.getSkipped())
				{
					skipCounts.merge(s.getReason(), 1, Integer::sum);
					skips.write(String.join("\t", file.getFileName().toString(), String.valueOf(s.getLine()),
						s.getReason().name(), oneLine(s.getDetail()), oneLine(lines[s.getLine() - 1])));
					skips.write('\n');
				}
				untranslated += result.getUntranslated();
			}
		}
		System.out.printf("%s: %d files, %d lines bundled, %d untranslated, skipped %s -> %s (%d KB)%n",
			language, files.size(), kept, untranslated, skipCounts, out.getFileName(), Files.size(out) / 1024);
		System.out.printf("%s: skipped lines listed in %s%n", language, report);
	}

	private static Writer gzipWriter(Path out) throws IOException
	{
		OutputStream gzip = new GZIPOutputStream(Files.newOutputStream(out))
		{
			{
				def.setLevel(Deflater.BEST_COMPRESSION);
			}
		};
		return new BufferedWriter(new OutputStreamWriter(gzip, StandardCharsets.UTF_8));
	}

	private static String oneLine(String text)
	{
		return text.replace('\t', ' ').replace('\n', ' ');
	}

	private static List<Path> list(Path dir, java.util.function.Predicate<Path> filter) throws IOException
	{
		try (Stream<Path> paths = Files.list(dir))
		{
			return paths.filter(filter).sorted().collect(Collectors.toList());
		}
	}
}
