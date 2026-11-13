package com.runehangul.text;

import lombok.Value;

/** One translation after normalization and choice expansion. */
@Value
public class Entry
{
	String english;
	String translation;
	Scope scope;
	int line;
}
