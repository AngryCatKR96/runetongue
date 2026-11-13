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
	}

	Language language;
	String folder;
	List<FileReport> files;
	int bundledEntries;
	String error;

	public int loadedLines()
	{
		return files.stream().mapToInt(f -> (int) f.getResult().getEntries().stream().map(e -> e.getLine()).distinct().count()).sum();
	}

	public long skippedLines()
	{
		return files.stream().flatMap(f -> f.getResult().getSkipped().stream()).filter(s -> s.getReason() != SkipReason.OVERRIDDEN).count();
	}

	public int untranslatedLines()
	{
		return files.stream().mapToInt(f -> f.getResult().getUntranslated()).sum();
	}
}
