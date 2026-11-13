package com.runehangul;

import com.runehangul.text.Scope;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;

/**
 * Collects text the plugin could not translate into {@code missing/<lang>/missing.tsv}, in the
 * user translation file format with the translation column empty, so a filled-in copy can go
 * straight into the translations folder (CONTEXT.md 3.5). Each English text and scope is written
 * once, across sessions. Only game text is recorded, never chat, and the player's name is already
 * replaced with {@code [player name]} by the caller. Disk access happens on the executor.
 */
@Slf4j
@Singleton
class MissingTexts
{
	static final String FILE_NAME = "missing.tsv";
	private static final String HEADER = "english\ttranslation\tcategory\tsub_category\tsource\n";

	/** One language's file; replaced as a whole when the language changes. */
	private static final class Session
	{
		final Set<String> seen = ConcurrentHashMap.newKeySet();
		volatile Filepath file;
	}

	private final ScheduledExecutorService executor;
	private volatile Session session;

	@Inject
	MissingTexts(ScheduledExecutorService executor)
	{
		this.executor = executor;
	}

	void start(Callable<Filepath> folder)
	{
		Session s = new Session();
		session = s;
		executor.execute(() ->
		{
			try
			{
				Filepath dir = folder.call();
				dir.createDirectories();
				Filepath file = dir.join(FILE_NAME);
				if (file.exists())
				{
					try (BufferedReader in = file.openBufferedReader())
					{
						in.lines().skip(1).map(MissingTexts::keyOfLine).forEach(s.seen::add);
					}
				}
				s.file = file;
				log.debug("collecting missing text in {} ({} already listed)", file, s.seen.size());
			}
			catch (Exception e)
			{
				log.warn("could not open the missing text file", e);
			}
		});
	}

	void stop()
	{
		session = null;
	}

	/** Client thread. {@code english} is the normalized lookup key. */
	void record(String english, Scope scope)
	{
		Session s = session;
		if (s == null || s.file == null || english.isEmpty())
		{
			return;
		}
		String line = String.join("\t", english, "", scope.getCategory(), scope.getSubCategory(), scope.getSource());
		if (!s.seen.add(keyOfLine(line)))
		{
			return;
		}
		executor.execute(() -> append(s, line));
	}

	private static void append(Session s, String line)
	{
		try
		{
			boolean fresh = !s.file.exists() || s.file.size() == 0;
			try (OutputStream out = s.file.openOutputStream(StandardOpenOption.CREATE, StandardOpenOption.APPEND))
			{
				out.write(((fresh ? HEADER : "") + line + "\n").getBytes(StandardCharsets.UTF_8));
			}
		}
		catch (IOException e)
		{
			log.warn("could not write missing text", e);
		}
	}

	/** english, category, sub_category and source; the translation column is ignored. */
	private static String keyOfLine(String line)
	{
		String[] c = line.split("\t", -1);
		StringBuilder key = new StringBuilder();
		for (int i : new int[]{0, 2, 3, 4})
		{
			key.append(i < c.length ? c[i] : "").append('\u0000');
		}
		return key.toString();
	}
}
