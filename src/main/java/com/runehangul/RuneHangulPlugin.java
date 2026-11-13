package com.runehangul;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
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
	@Inject
	private Client client;

	@Inject
	private RuneHangulConfig config;

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

	@Provides
	RuneHangulConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneHangulConfig.class);
	}
}
