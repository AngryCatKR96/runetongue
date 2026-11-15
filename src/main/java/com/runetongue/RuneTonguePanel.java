package com.runetongue;

import com.runetongue.text.ParseResult;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.Filepath;

/** Side panel for the user translation folder. All methods run on the Swing EDT. */
class RuneTonguePanel extends PluginPanel
{
	static final int SKIPPED_LINES_PER_FILE = 50;
	private static final int DETAIL_LENGTH = 80;
	/** The RuneScape font RuneLite uses by default has no Hangul; this one falls back to OS fonts. */
	private static final Font FONT = FontManager.getDefaultFont().deriveFont(12f);

	private final JTextArea report = new JTextArea();
	private final JLabel missing = new JLabel(" ");
	private final JButton copyPathButton;
	private final JButton addFilesButton;
	private final JButton reloadButton;
	private final JButton saveMissingButton;
	private Language language;

	/**
	 * Plugin Hub plugins may not start other programs, so the panel cannot open the folder in the
	 * file manager: it copies the folder's path instead, adds files through a file chooser, and
	 * saves the untranslated-text file wherever the player picks.
	 */
	RuneTonguePanel(Language language, Runnable copyFolderPath, Consumer<List<Filepath>> addFiles, Runnable reload,
		Consumer<Filepath> saveMissing)
	{
		setLayout(new BorderLayout(0, 8));

		JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 4));
		copyPathButton = button(copyFolderPath);
		addFilesButton = button(() -> chooseFiles(addFiles));
		reloadButton = button(reload);
		saveMissingButton = button(() -> chooseMissingTarget(saveMissing));
		buttons.add(copyPathButton);
		buttons.add(addFilesButton);
		buttons.add(reloadButton);
		buttons.add(saveMissingButton);
		missing.setFont(FONT);
		buttons.add(missing);
		add(buttons, BorderLayout.NORTH);

		report.setFont(FONT);
		report.setEditable(false);
		report.setLineWrap(true);
		report.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		report.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		add(report, BorderLayout.CENTER);
		setLanguage(language);
		report.setText(language.ui("불러오는 중…", "Loading…"));
	}

	/** The panel speaks Korean for Korean and English for the other languages. */
	private void setLanguage(Language language)
	{
		this.language = language;
		copyPathButton.setText(language.ui("폴더 경로 복사", "Copy folder path"));
		addFilesButton.setText(language.ui("파일 추가", "Add files"));
		reloadButton.setText(language.ui("다시 불러오기", "Reload"));
		saveMissingButton.setText(language.ui("빠진 문장 저장…", "Save untranslated text…"));
	}

	void show(LoadReport loaded)
	{
		setLanguage(loaded.getLanguage());
		report.setText(describe(loaded));
		report.setCaretPosition(0);
	}

	/** How many lines are waiting in missing/<lang>/missing.tsv. */
	void showMissing(int lines)
	{
		missing.setText(language.ui("빠진 문장 " + lines + "줄 모임 (번역하면 자동으로 지워짐)",
			lines + " untranslated lines collected (removed once translated)"));
	}

	void showMessage(String message)
	{
		report.setText(message);
	}

	private void chooseMissingTarget(Consumer<Filepath> saveMissing)
	{
		List<Filepath> chosen = new Filepath.Chooser()
			.setIsSave()
			.setAcceptsFiles()
			.addExtensionFilter("TSV (*.tsv)", "tsv")
			.setDefaultExtension("tsv")
			.setFileName("missing_" + language.getCode() + ".tsv")
			.setDialogTitle(language.ui("빠진 문장 저장", "Save untranslated text"))
			.showDialog(this);
		if (chosen != null && !chosen.isEmpty())
		{
			saveMissing.accept(chosen.get(0));
		}
	}

	private void chooseFiles(Consumer<List<Filepath>> addFiles)
	{
		List<Filepath> chosen = new Filepath.Chooser()
			.setIsOpen()
			.setAcceptsFiles()
			.setMultiSelectionEnabled(true)
			.addExtensionFilter("TSV (*.tsv)", "tsv")
			.setDialogTitle(language.ui("번역 폴더에 넣을 tsv 파일", "tsv files to add to the translation folder"))
			.showDialog(this);
		if (chosen != null && !chosen.isEmpty())
		{
			addFiles.accept(chosen);
		}
	}

	private static JButton button(Runnable action)
	{
		JButton button = new JButton();
		button.setFont(FONT);
		button.addActionListener(e -> action.run());
		return button;
	}

	static String describe(LoadReport loaded)
	{
		Language lang = loaded.getLanguage();
		StringBuilder out = new StringBuilder();
		out.append(lang.ui("언어: ", "Language: ")).append(lang).append('\n');
		out.append(lang.ui("폴더: ", "Folder: ")).append(loaded.getFolder()).append("\n\n");
		out.append(loaded.summary()).append('\n');
		if (loaded.getError() == null && loaded.getFiles().isEmpty())
		{
			out.append('\n').append(lang.ui(
				"폴더에 tsv 파일이 없어요. '파일 추가'로 고르거나 '폴더 열기'로 직접 넣은 뒤 '다시 불러오기'를 누르세요.",
				"No tsv files in the folder yet. Pick some with 'Add files', or put them in with 'Open folder' and press 'Reload'."))
				.append('\n');
		}
		for (LoadReport.FileReport file : loaded.getFiles())
		{
			ParseResult result = file.getResult();
			out.append('\n').append(file.getFileName()).append(": ").append(String.format(
				lang.ui("%d줄 불러옴, %d줄 건너뜀, 미번역 %d줄", "%d loaded, %d skipped, %d untranslated"),
				file.loadedLines(), file.skippedLines(), result.getUntranslated())).append('\n');
			List<ParseResult.Skipped> skipped = result.getSkipped();
			int shown = Math.min(skipped.size(), SKIPPED_LINES_PER_FILE);
			for (ParseResult.Skipped s : skipped.subList(0, shown))
			{
				out.append("  ").append(loaded.skippedLine(s)).append('\n');
				if (!s.getDetail().isEmpty())
				{
					out.append("    ").append(shorten(s.getDetail())).append('\n');
				}
			}
			if (skipped.size() > shown)
			{
				out.append("  ").append(lang.ui("… 외 " + (skipped.size() - shown) + "줄", "… and " + (skipped.size() - shown) + " more")).append('\n');
			}
		}
		return out.toString();
	}

	private static String shorten(String detail)
	{
		String oneLine = detail.replace('\t', ' ');
		return oneLine.length() <= DETAIL_LENGTH ? oneLine : oneLine.substring(0, DETAIL_LENGTH) + "…";
	}
}
