package com.runehangul;

import com.google.inject.Provides;
import com.runehangul.glyph.GlyphColor;
import com.runehangul.glyph.GlyphIcons;
import com.runehangul.text.Normalizer;
import com.runehangul.text.ParseResult;
import com.runehangul.text.Scope;
import com.runehangul.text.SkipReason;
import com.runehangul.text.SourceText;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Filepath;
import net.runelite.client.util.LinkBrowser;

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
	private static final int ICON_SIZE = 16;

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

	@Inject
	private DialogueTranslator dialogue;

	@Inject
	private MissingTexts missing;

	@Inject
	private MenuTranslator menu;

	@Inject
	private InterfaceTranslator interfaces;

	@Inject
	private ChatTranslator chatTranslator;

	@Inject
	private WidgetTexts widgetTexts;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ScheduledExecutorService executor;

	private RuneHangulPanel panel;
	private NavigationButton navButton;
	/** A load that finished before login, shown in chat once the player logs in. Client thread only. */
	private LoadReport unannounced;

	@Override
	protected void startUp() throws Exception
	{
		panel = new RuneHangulPanel(() -> openFolder(this::folder), this::addFiles, () -> reload(true),
			() -> openFolder(this::missingFolder));
		navButton = NavigationButton.builder()
			.tooltip("RuneHangul")
			.icon(icon())
			.priority(10)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		log.debug("RuneHangul started");
		missing.start(this::missingFolder);
		reload(false);
	}

	@Override
	protected void shutDown() throws Exception
	{
		store.stop();
		missing.stop();
		dialogue.reset();
		menu.reset();
		widgetTexts.reset();
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
		unannounced = null;
		log.debug("RuneHangul stopped");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (RuneHangulConfig.GROUP.equals(event.getGroup()) && RuneHangulConfig.LANGUAGE.equals(event.getKey()))
		{
			missing.start(this::missingFolder);
			reload(true);
		}
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		dialogue.update();
		interfaces.update();
		menu.update();
	}

	/** Late, so other plugins see the English message first. */
	@Subscribe(priority = -100)
	public void onChatMessage(ChatMessage event)
	{
		if (chatTranslator.translate(event.getMessageNode()))
		{
			client.refreshChat();
		}
	}

	/** Late, so the menu is translated after other plugins have changed it. */
	@Subscribe(priority = -100)
	public void onMenuOpened(MenuOpened event)
	{
		menu.update();
	}

	/** Early, so other plugins see the English option and target. */
	@Subscribe(priority = 100)
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		menu.restore(event.getMenuEntry());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && unannounced != null)
		{
			announce(unannounced);
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

	private Filepath folder() throws IOException
	{
		return getPluginDirectory().join("translations", config.language().getCode());
	}

	private Filepath missingFolder() throws IOException
	{
		return getPluginDirectory().join("missing", config.language().getCode());
	}

	private void reload(boolean announce)
	{
		RuneHangulPanel target = panel;
		if (target == null)
		{
			return;
		}
		store.reload(this::folder, config.language(), report ->
		{
			SwingUtilities.invokeLater(() -> target.show(report));
			clientThread.invoke(() ->
			{
				menu.reset();
				widgetTexts.reset();
				dialogue.reset();
				if (announce || client.getGameState() == GameState.LOGGED_IN)
				{
					announce(report);
				}
				else
				{
					unannounced = report;
				}
			});
		});
	}

	private void openFolder(Callable<Filepath> folder)
	{
		executor.execute(() ->
		{
			try
			{
				Filepath dir = folder.call();
				dir.createDirectories();
				LinkBrowser.open(dir.toString());
			}
			catch (Exception e)
			{
				log.warn("could not open translation folder", e);
				showMessage("폴더를 열지 못했어요: " + e.getMessage());
			}
		});
	}

	private void addFiles(List<Filepath> chosen)
	{
		executor.execute(() ->
		{
			try
			{
				Filepath dir = folder();
				dir.createDirectories();
				for (Filepath file : chosen)
				{
					// A file already in the folder copies onto itself, which Files.copy skips.
					file.copyTo(dir.join(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
				}
				reload(true);
			}
			catch (Exception e)
			{
				log.warn("could not copy translation files", e);
				showMessage("파일을 복사하지 못했어요: " + e.getMessage());
			}
		});
	}

	private void showMessage(String message)
	{
		RuneHangulPanel target = panel;
		if (target != null)
		{
			SwingUtilities.invokeLater(() -> target.showMessage(message));
		}
	}

	private BufferedImage icon()
	{
		BufferedImage glyph = glyphIcons.image('한', GlyphColor.ORANGE);
		BufferedImage icon = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.drawImage(glyph, (ICON_SIZE - glyph.getWidth()) / 2, (ICON_SIZE - glyph.getHeight()) / 2, null);
		g.dispose();
		return icon;
	}

	private void announce(LoadReport report)
	{
		unannounced = null;
		say(report.summary());
		if (report.getError() != null)
		{
			return;
		}
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
