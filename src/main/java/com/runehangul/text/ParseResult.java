package com.runehangul.text;

import java.util.List;
import lombok.Value;

@Value
public class ParseResult
{
	@Value
	public static class Skipped
	{
		int line;
		SkipReason reason;
		String detail;
	}

	List<Entry> entries;
	List<Skipped> skipped;
	int untranslated;
}
