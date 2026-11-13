package com.runehangul;

import static org.junit.Assert.assertEquals;
import com.runehangul.text.Scope;
import com.runehangul.text.TranslationTable;
import com.runehangul.text.Translations;
import com.runehangul.text.TsvParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.client.util.Filepath;
import org.junit.Test;

public class MissingTextsTest
{
	private static final Scope HANS = Scope.of("dialogue", "Hans", "Hans");

	/** Runs one session on its own executor and waits for its writes. */
	private static void session(Path dir, String... englishLines) throws Exception
	{
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		MissingTexts missing = new MissingTexts(executor);
		missing.start(() -> Filepath.Unchecked.getRooted(dir));
		executor.submit(() -> null).get();
		for (String english : englishLines)
		{
			missing.record(english, HANS);
		}
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);
	}

	@Test
	public void writesEachTextOnceAcrossSessions() throws Exception
	{
		Path dir = Files.createTempDirectory("missing");
		session(dir, "Hello there.", "You have <Num0> coins.", "Hello there.");
		session(dir, "Hello there.", "Goodbye.");

		List<String> lines = Files.readAllLines(dir.resolve(MissingTexts.FILE_NAME), StandardCharsets.UTF_8);
		assertEquals(List.of(
			"english\ttranslation\tcategory\tsub_category\tsource",
			"Hello there.\t\tdialogue\tHans\tHans",
			"You have <Num0> coins.\t\tdialogue\tHans\tHans",
			"Goodbye.\t\tdialogue\tHans\tHans"), lines);
	}

	@Test
	public void theFileLoadsAsAUserTranslationFile() throws Exception
	{
		Path dir = Files.createTempDirectory("missing");
		session(dir, "Hello there.", "You have <Num0> coins.");

		byte[] bytes = Files.readAllBytes(dir.resolve(MissingTexts.FILE_NAME));
		assertEquals(2, TsvParser.parse(bytes).getUntranslated());
		assertEquals(List.of(), TsvParser.parse(bytes).getSkipped());
	}

	@Test
	public void dropsLinesThatAreNowTranslated() throws Exception
	{
		Path dir = Files.createTempDirectory("missing");
		session(dir, "Hello there.", "You have <Num0> coins.");
		Translations translated = new Translations(new TranslationTable(TsvParser.parse(
			"You have 5 coins.\t코인이 5개 있어요.\tdialogue\n".getBytes(StandardCharsets.UTF_8)).getEntries()), TranslationTable.EMPTY);

		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		MissingTexts missing = new MissingTexts(executor);
		missing.start(() -> Filepath.Unchecked.getRooted(dir));
		AtomicInteger left = new AtomicInteger(-1);
		missing.prune(translated, left::set);
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		assertEquals(1, left.get());
		assertEquals(List.of("english\ttranslation\tcategory\tsub_category\tsource", "Hello there.\t\tdialogue\tHans\tHans"),
			Files.readAllLines(dir.resolve(MissingTexts.FILE_NAME), StandardCharsets.UTF_8));
	}
}
