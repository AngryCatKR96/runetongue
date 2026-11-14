package com.runetongue;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(RuneTongueConfig.GROUP)
public interface RuneTongueConfig extends Config
{
	String GROUP = "runetongue";
	String LANGUAGE = "language";
	String ORIGINAL_TOOLTIP = "originalTooltip";

	@ConfigItem(
		keyName = LANGUAGE,
		name = "Language",
		description = "Translation language. Files in plugin-data/runetongue/translations/<code>/ override the bundled translations."
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
