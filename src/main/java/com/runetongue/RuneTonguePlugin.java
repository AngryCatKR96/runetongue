package com.runetongue;

import com.google.inject.Provides;
import com.runetongue.glyph.GlyphColor;
import com.runetongue.glyph.GlyphIcons;
import com.runetongue.text.Normalizer;
import com.runetongue.text.ParseResult;
import com.runetongue.text.Scope;
import com.runetongue.text.SkipReason;
import com.runetongue.text.SourceText;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
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
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Filepath;

@Slf4j
@PluginDescriptor(
	name = "RuneTongue",
	description = "Play in Korean, or in your own language by dropping translation tsv files into a folder",
	tags = {"korean", "translation", "translate", "language", "spanish", "portuguese", "french", "german", "한국어"},
	internalName = "runetongue"
)
public class RuneTonguePlugin extends Plugin
{
	private static final String GLYPH_SAMPLE = "다람쥐 헌 쳇바퀴에 타고파, Lumbridge(럼브리지) 1,234gp 똠";
	private static final int SKIPPED_LINES_IN_CHAT = 5;
	private static final int ICON_SIZE = 16;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private RuneTongueConfig config;

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
	private OverlayManager overlayManager;

	@Inject
	private OriginalTextTooltip originalTooltip;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ScheduledExecutorService executor;

	private RuneTonguePanel panel;
	private NavigationButton navButton;
	/** A load that finished before login, shown in chat once the player logs in. Client thread only. */
	private LoadReport unannounced;

	@Override
	protected void startUp() throws Exception
	{
		panel = new RuneTonguePanel(config.language(), this::copyFolderPath, this::addFiles, () -> reload(true),
			this::saveMissing);
		navButton = NavigationButton.builder()
			.tooltip("RuneTongue")
			.icon(icon())
			.priority(10)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(originalTooltip);
		log.debug("RuneTongue started");
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
		overlayManager.remove(originalTooltip);
		navButton = null;
		panel = null;
		unannounced = null;
		log.debug("RuneTongue stopped");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (RuneTongueConfig.GROUP.equals(event.getGroup()) && RuneTongueConfig.LANGUAGE.equals(event.getKey()))
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

	@Subscribe(priority = -100)
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		chatTranslator.translateOverhead(event.getActor(), event.getOverheadText());
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
			say(found == null ? english + config.language().ui(" -> 번역 없음", " -> no translation") : english + " -> " + Normalizer.restore(found, source));
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
		RuneTonguePanel target = panel;
		if (target == null)
		{
			return;
		}
		store.reload(this::folder, config.language(), report ->
		{
			SwingUtilities.invokeLater(() -> target.show(report));
			missing.prune(store.get(), lines -> SwingUtilities.invokeLater(() -> target.showMissing(lines)));
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

	/** Puts the translation folder's path on the clipboard, to paste into the file manager. */
	private void copyFolderPath()
	{
		executor.execute(() ->
		{
			try
			{
				Filepath dir = folder();
				dir.createDirectories();
				String path = dir.toString();
				SwingUtilities.invokeLater(() ->
				{
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(path), null);
					showMessage(config.language().ui("폴더 경로를 복사했어요. 파일 탐색기 주소창에 붙여 넣으세요:\n",
						"Copied the folder path. Paste it into your file manager's address bar:\n") + path);
				});
			}
			catch (Exception e)
			{
				log.warn("could not find the translation folder", e);
				showMessage(config.language().ui("폴더를 찾지 못했어요: ", "Could not find the folder: ") + e.getMessage());
			}
		});
	}

	/** Copies missing/<lang>/missing.tsv to where the player chose to save it. */
	private void saveMissing(Filepath target)
	{
		executor.execute(() ->
		{
			try
			{
				Filepath source = missingFolder().join(MissingTexts.FILE_NAME);
				if (!source.exists())
				{
					showMessage(config.language().ui("아직 모인 빠진 문장이 없어요.", "No untranslated text has been collected yet."));
					return;
				}
				source.copyTo(target, StandardCopyOption.REPLACE_EXISTING);
				showMessage(config.language().ui("빠진 문장을 저장했어요: ", "Saved the untranslated text to: ") + target);
			}
			catch (Exception e)
			{
				log.warn("could not save the missing text file", e);
				showMessage(config.language().ui("파일을 저장하지 못했어요: ", "Could not save the file: ") + e.getMessage());
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
				showMessage(config.language().ui("파일을 복사하지 못했어요: ", "Could not copy the files: ") + e.getMessage());
			}
		});
	}

	private void showMessage(String message)
	{
		RuneTonguePanel target = panel;
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
				say(file.getFileName() + " " + report.skippedLine(s));
			}
		}
	}

	private void say(String text)
	{
		chat("RuneTongue: " + glyphIcons.toChatText(text, GlyphColor.BLACK));
	}

	private void chat(String markup)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", markup, null);
	}

	@Provides
	RuneTongueConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneTongueConfig.class);
	}
}
