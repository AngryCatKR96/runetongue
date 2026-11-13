package com.runehangul;

import com.google.inject.Provides;
import com.runehangul.glyph.GlyphColor;
import com.runehangul.glyph.GlyphIcons;
import com.runehangul.text.Normalizer;
import com.runehangul.text.ParseResult;
import com.runehangul.text.Scope;
import com.runehangul.text.SkipReason;
import com.runehangul.text.SourceText;
import java.util.Arrays;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "RuneHangul",
	description = "Translates Old School RuneScape into Korean",
	tags = {"korean", "translation", "language", "한국어"},
	internalName = "runehangul"
)
public class RuneHangulPlugin extends Plugin
{
	private static final String GLYPH_SAMPLE = "다람쥐 헌 쳇바퀴에 타고파, Lumbridge(럼브리지) 1,234gp 똠";
	private static final int SKIPPED_LINES_IN_CHAT = 5;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private RuneHangulConfig config;

	@Inject
	private GlyphIcons glyphIcons;

	@Inject
	private TranslationStore store;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("RuneHangul started");
		reload(false);
	}

	@Override
	protected void shutDown() throws Exception
	{
		store.stop();
		log.debug("RuneHangul stopped");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (RuneHangulConfig.GROUP.equals(event.getGroup()) && RuneHangulConfig.LANGUAGE.equals(event.getKey()))
		{
			reload(true);
		}
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		String[] args = event.getArguments();
		if ("hangul".equals(event.getCommand()))
		{
			for (GlyphColor color : GlyphColor.values())
			{
				chat(color.name().toLowerCase() + ": " + glyphIcons.toChatText(GLYPH_SAMPLE, color));
			}
		}
		else if ("rh".equals(event.getCommand()) && args.length >= 1 && "reload".equals(args[0]))
		{
			reload(true);
		}
		else if ("rh".equals(event.getCommand()) && args.length >= 2 && "find".equals(args[0]))
		{
			String english = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
			SourceText source = Normalizer.normalizeSource(english);
			String found = store.get().find(source.getKey(), Scope.ANY);
			say(found == null ? english + " -> 번역 없음" : english + " -> " + Normalizer.restore(found, source));
		}
	}

	private void reload(boolean announce)
	{
		store.reload(this::getPluginDirectory, config.language(), report -> clientThread.invoke(() ->
		{
			if (announce || client.getGameState() == GameState.LOGGED_IN)
			{
				announce(report);
			}
		}));
	}

	private void announce(LoadReport report)
	{
		if (report.getError() != null)
		{
			say("번역 폴더를 읽지 못했어요: " + report.getError());
			return;
		}
		say(String.format("%s 파일 %d개, %d줄 불러옴, %d줄 건너뜀, 미번역 %d줄 (내장 번역 %d개)",
			report.getLanguage().getCode(), report.getFiles().size(), report.loadedLines(),
			report.skippedLines(), report.untranslatedLines(), report.getBundledEntries()));
		int shown = 0;
		for (LoadReport.FileReport file : report.getFiles())
		{
			for (ParseResult.Skipped s : file.getResult().getSkipped())
			{
				if (s.getReason() == SkipReason.OVERRIDDEN)
				{
					continue;
				}
				if (shown++ == SKIPPED_LINES_IN_CHAT)
				{
					say("…");
					return;
				}
				say(file.getFileName() + " " + s.getLine() + "번째 줄: " + s.getReason().getMessage());
			}
		}
	}

	private void say(String text)
	{
		chat("RuneHangul: " + glyphIcons.toChatText(text, GlyphColor.BLACK));
	}

	private void chat(String markup)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", markup, null);
	}

	@Provides
	RuneHangulConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneHangulConfig.class);
	}
}
