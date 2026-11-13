package com.runehangul.text;

import java.util.List;
import lombok.Value;

/**
 * English text with numbers and color tags replaced by {@code <NumN>} and {@code <colNumN>}.
 * {@code numbers} and {@code colors} hold what was replaced, indexed by N.
 */
@Value
public class SourceText
{
	String key;
	List<String> numbers;
	List<String> colors;
}
