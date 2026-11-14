package com.runetongue;

import com.runetongue.text.Entry;
import com.runetongue.text.ParseResult;
import com.runetongue.text.TranslationTable;
import com.runetongue.text.Translations;
import com.runetongue.text.TsvParser;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;

/**
 * Holds the active translations. Loading runs on the executor and swaps in a finished,
 * immutable {@link Translations}, so the client thread only ever reads.
 */
@Slf4j
@Singleton
public class TranslationStore
{
	private final ScheduledExecutorService executor;
	private volatile Translations translations = Translations.EMPTY;
	private final Object bundledLock = new Object();
	private Future<?> pending;
	private int generation;
	private Language bundledLanguage;
	private TranslationTable bundled = TranslationTable.EMPTY;

	@Inject
	TranslationStore(ScheduledExecutorService executor)
	{
		this.executor = executor;
	}

	public Translations get()
	{
		return translations;
	}

	/** False until the first load finishes (and after stop), when every lookup misses. */
	public boolean isLoaded()
	{
		return translations != Translations.EMPTY;
	}

	public synchronized void reload(Callable<Filepath> folder, Language language, Consumer<LoadReport> onDone)
	{
		cancelPending();
		int current = ++generation;
		pending = executor.submit(() ->
		{
			LoadReport report = load(folder, language, current);
			if (report != null)
			{
				onDone.accept(report);
			}
		});
	}

	public synchronized void stop()
	{
		cancelPending();
		generation++;
		translations = Translations.EMPTY;
	}

	private void cancelPending()
	{
		if (pending != null)
		{
			pending.cancel(true);
			pending = null;
		}
	}

	private LoadReport load(Callable<Filepath> folder, Language language, int current)
	{
		long start = System.nanoTime();
		TranslationTable bundledTable = bundledFor(language);
		List<LoadReport.FileReport> files = new ArrayList<>();
		String folderName = "";
		String error = null;
		try
		{
			Filepath dir = folder.call();
			dir.createDirectories();
			folderName = dir.toString();
			for (Filepath file : tsvFiles(dir))
			{
				if (Thread.currentThread().isInterrupted())
				{
					return null;
				}
				try (InputStream in = file.openInputStream())
				{
					files.add(new LoadReport.FileReport(file.getFileName(), TsvParser.parse(in.readAllBytes())));
				}
			}
		}
		catch (Exception e)
		{
			log.warn("could not read translation folder", e);
			error = String.valueOf(e.getMessage());
		}

		List<Entry> entries = new ArrayList<>();
		for (LoadReport.FileReport f : files)
		{
			entries.addAll(f.getResult().getEntries());
		}
		Translations loaded = new Translations(new TranslationTable(entries), bundledTable);
		synchronized (this)
		{
			if (current != generation)
			{
				return null;
			}
			translations = loaded;
		}
		log.debug("loaded {} user files and {} bundled entries for {} in {} ms",
			files.size(), bundledTable.size(), language, (System.nanoTime() - start) / 1_000_000);
		return new LoadReport(language, folderName, files, bundledTable.size(), error);
	}

	private static List<Filepath> tsvFiles(Filepath dir) throws IOException
	{
		try (Stream<Filepath> walk = dir.walk(1))
		{
			return walk
				.filter(Filepath::isFile)
				.filter(f -> f.getFileName().toLowerCase(Locale.ROOT).endsWith(".tsv"))
				.sorted(Comparator.comparing(Filepath::getFileName))
				.collect(Collectors.toList());
		}
	}

	private TranslationTable bundledFor(Language language)
	{
		synchronized (bundledLock)
		{
			if (language != bundledLanguage)
			{
				bundled = readBundled(language);
				bundledLanguage = language;
			}
			return bundled;
		}
	}

	private static TranslationTable readBundled(Language language)
	{
		String resource = language.getBundledResource();
		if (resource == null)
		{
			return TranslationTable.EMPTY;
		}
		try (InputStream raw = TranslationStore.class.getResourceAsStream(resource))
		{
			if (raw == null)
			{
				log.debug("no bundled translations at {}", resource);
				return TranslationTable.EMPTY;
			}
			try (InputStream in = new GZIPInputStream(raw))
			{
				ParseResult result = TsvParser.parse(in.readAllBytes());
				return new TranslationTable(result.getEntries());
			}
		}
		catch (IOException e)
		{
			log.warn("could not read bundled translations", e);
			return TranslationTable.EMPTY;
		}
	}
}
