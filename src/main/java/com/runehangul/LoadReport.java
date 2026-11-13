package com.runehangul;

import com.runehangul.text.ParseResult;
import com.runehangul.text.SkipReason;
import java.util.List;
import lombok.Value;

@Value
public class LoadReport
{
	@Value
	public static class FileReport
	{
		String fileName;
		ParseResult result;

		public int loadedLines()
		{
			return (int) result.getEntries().stream().map(e -> e.getLine()).distinct().count();
		}

		public long skippedLines()
		{
			return result.getSkipped().stream().filter(s -> s.getReason() != SkipReason.OVERRIDDEN).count();
		}
	}

	Language language;
	String folder;
	List<FileReport> files;
	int bundledEntries;
	String error;

	public int loadedLines()
	{
		return files.stream().mapToInt(FileReport::loadedLines).sum();
	}

	public long skippedLines()
	{
		return files.stream().mapToLong(FileReport::skippedLines).sum();
	}

	public int untranslatedLines()
	{
		return files.stream().mapToInt(f -> f.getResult().getUntranslated()).sum();
	}

	/** One line shared by the chat message and the side panel. */
	public String summary()
	{
		if (error != null)
		{
			return "번역 폴더를 읽지 못했어요: " + error;
		}
		return String.format("%s 파일 %d개, %d줄 불러옴, %d줄 건너뜀, 미번역 %d줄 (내장 번역 %d개)",
			language.getCode(), files.size(), loadedLines(), skippedLines(), untranslatedLines(), bundledEntries);
	}
}
