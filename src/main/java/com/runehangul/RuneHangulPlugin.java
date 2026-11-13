package com.runehangul;

import com.google.inject.Provides;
import com.runehangul.glyph.GlyphColor;
import com.runehangul.glyph.GlyphIcons;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "RuneHangul",
	description = "Translates Old School RuneScape into Korean",
	tags = {"korean", "translation", "language", "한국어"}
)
public class RuneHangulPlugin extends Plugin
{
	private static final String GLYPH_SAMPLE = "다람쥐 헌 쳇바퀴에 타고파, Lumbridge(럼브리지) 1,234gp 똠";

	@Inject
	private Client client;

	@Inject
	private RuneHangulConfig config;

	@Inject
	private GlyphIcons glyphIcons;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("RuneHangul started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("RuneHangul stopped");
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		if (!"hangul".equals(event.getCommand()))
		{
			return;
		}
		for (GlyphColor color : GlyphColor.values())
		{
			String line = color.name().toLowerCase() + ": " + glyphIcons.toChatText(GLYPH_SAMPLE, color);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", line, null);
		}
	}

	@Provides
	RuneHangulConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneHangulConfig.class);
	}
}
