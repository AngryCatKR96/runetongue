package com.runehangul;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.runehangul.text.TsvParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Test;

public class RuneHangulPanelTest
{
	private static LoadReport report(String fileName, byte[] tsv)
	{
		return new LoadReport(Language.KOREAN, "/folder/ko",
			List.of(new LoadReport.FileReport(fileName, TsvParser.parse(tsv))), 0, null);
	}

	@Test
	public void describesTheExampleFile() throws IOException
	{
		byte[] example = Files.readAllBytes(Paths.get("docs/examples/example_ko.tsv"));
		String text = RuneHangulPanel.describe(report("example_ko.tsv", example));

		assertTrue(text, text.contains("폴더: /folder/ko"));
		assertTrue(text, text.contains("ko 파일 1개, 5줄 불러옴, 1줄 건너뜀, 미번역 1줄 (내장 번역 0개)"));
		assertTrue(text, text.contains("example_ko.tsv: 5줄 불러옴, 1줄 건너뜀, 미번역 1줄"));
		assertTrue(text, text.contains("  6번째 줄: 치환 기호가 원문과 다름"));
		assertTrue(text, text.contains("  8번째 줄: 같은 파일의 뒤 줄이 덮어씀"));
		assertTrue(text, text.contains("    -> 9"));
	}

	@Test
	public void capsSkippedLinesPerFile()
	{
		StringBuilder tsv = new StringBuilder();
		for (int i = 0; i < RuneHangulPanel.SKIPPED_LINES_PER_FILE + 7; i++)
		{
			tsv.append("only one column\n");
		}
		String text = RuneHangulPanel.describe(report("bad.tsv", tsv.toString().getBytes(StandardCharsets.UTF_8)));

		assertEquals(RuneHangulPanel.SKIPPED_LINES_PER_FILE, text.split("번째 줄:", -1).length - 1);
		assertTrue(text, text.contains("… 외 7줄"));
	}

	@Test
	public void tellsWhatToDoWhenTheFolderIsEmpty()
	{
		LoadReport empty = new LoadReport(Language.SPANISH, "/folder/es", List.of(), 0, null);
		String text = RuneHangulPanel.describe(empty);

		// Every language but Korean gets the plugin's messages in English.
		assertTrue(text, text.contains("es: 0 files, 0 lines loaded"));
		assertTrue(text, text.contains("No tsv files in the folder yet"));
	}
}
