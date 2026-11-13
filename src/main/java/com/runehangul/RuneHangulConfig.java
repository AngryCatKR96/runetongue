package com.runehangul;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(RuneHangulConfig.GROUP)
public interface RuneHangulConfig extends Config
{
	String GROUP = "runehangul";
	String LANGUAGE = "language";
	String ORIGINAL_TOOLTIP = "originalTooltip";

	@ConfigItem(
		keyName = LANGUAGE,
		name = "Language",
		description = "Translation language. Files in plugin-data/runehangul/translations/<code>/ override the bundled translations."
	)
	default Language language()
	{
		return Language.KOREAN;
	}

	@ConfigItem(
		keyName = ORIGINAL_TOOLTIP,
		name = "Show English on hover",
		description = "Show the English of a translated menu option in a tooltip at the mouse."
	)
	default boolean originalTooltip()
	{
		return true;
	}
}
