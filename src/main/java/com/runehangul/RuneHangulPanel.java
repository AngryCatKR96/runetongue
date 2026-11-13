package com.runehangul;

import com.runehangul.text.ParseResult;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.Filepath;

/** Side panel for the user translation folder. All methods run on the Swing EDT. */
class RuneHangulPanel extends PluginPanel
{
	static final int SKIPPED_LINES_PER_FILE = 50;
	private static final int DETAIL_LENGTH = 80;
	/** The RuneScape font RuneLite uses by default has no Hangul; this one falls back to OS fonts. */
	private static final Font FONT = FontManager.getDefaultFont().deriveFont(12f);

	private final JTextArea report = new JTextArea("불러오는 중…");

	RuneHangulPanel(Runnable openFolder, Consumer<List<Filepath>> addFiles, Runnable reload, Runnable openMissingFolder)
	{
		setLayout(new BorderLayout(0, 8));

		JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 4));
		buttons.add(button("폴더 열기", openFolder));
		buttons.add(button("파일 추가", () -> chooseFiles(addFiles)));
		buttons.add(button("다시 불러오기", reload));
		buttons.add(button("빠진 문장 폴더 열기", openMissingFolder));
		add(buttons, BorderLayout.NORTH);

		report.setFont(FONT);
		report.setEditable(false);
		report.setLineWrap(true);
		report.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		report.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		add(report, BorderLayout.CENTER);
	}

	void show(LoadReport loaded)
	{
		report.setText(describe(loaded));
		report.setCaretPosition(0);
	}

	void showMessage(String message)
	{
		report.setText(message);
	}

	private void chooseFiles(Consumer<List<Filepath>> addFiles)
	{
		List<Filepath> chosen = new Filepath.Chooser()
			.setIsOpen()
			.setAcceptsFiles()
			.setMultiSelectionEnabled(true)
			.addExtensionFilter("TSV (*.tsv)", "tsv")
			.setDialogTitle("번역 폴더에 넣을 tsv 파일")
			.showDialog(this);
		if (chosen != null && !chosen.isEmpty())
		{
			addFiles.accept(chosen);
		}
	}

	private static JButton button(String label, Runnable action)
	{
		JButton button = new JButton(label);
		button.setFont(FONT);
		button.addActionListener(e -> action.run());
		return button;
	}

	static String describe(LoadReport loaded)
	{
		StringBuilder out = new StringBuilder();
		out.append("언어: ").append(loaded.getLanguage()).append('\n');
		out.append("폴더: ").append(loaded.getFolder()).append("\n\n");
		out.append(loaded.summary()).append('\n');
		if (loaded.getError() == null && loaded.getFiles().isEmpty())
		{
			out.append("\n폴더에 tsv 파일이 없어요. '파일 추가'로 고르거나 '폴더 열기'로 직접 넣은 뒤 '다시 불러오기'를 누르세요.\n");
		}
		for (LoadReport.FileReport file : loaded.getFiles())
		{
			ParseResult result = file.getResult();
			out.append('\n').append(file.getFileName()).append(": ")
				.append(file.loadedLines()).append("줄 불러옴, ")
				.append(file.skippedLines()).append("줄 건너뜀, 미번역 ")
				.append(result.getUntranslated()).append("줄\n");
			List<ParseResult.Skipped> skipped = result.getSkipped();
			int shown = Math.min(skipped.size(), SKIPPED_LINES_PER_FILE);
			for (ParseResult.Skipped s : skipped.subList(0, shown))
			{
				out.append("  ").append(s.getLine()).append("번째 줄: ").append(s.getReason().getMessage()).append('\n');
				if (!s.getDetail().isEmpty())
				{
					out.append("    ").append(shorten(s.getDetail())).append('\n');
				}
			}
			if (skipped.size() > shown)
			{
				out.append("  … 외 ").append(skipped.size() - shown).append("줄\n");
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
