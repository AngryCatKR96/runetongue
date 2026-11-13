package com.runehangul;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(RuneHangulConfig.GROUP)
public interface RuneHangulConfig extends Config
{
	String GROUP = "runehangul";
	String LANGUAGE = "language";

	@ConfigItem(
		keyName = LANGUAGE,
		name = "Language",
		description = "Translation language. Files in plugin-data/runehangul/translations/<code>/ override the bundled translations."
	)
	default Language language()
	{
		return Language.KOREAN;
	}
}
