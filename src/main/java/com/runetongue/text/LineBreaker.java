package com.runetongue.text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Breaks text into lines that fit a pixel width, measuring each character with the given width
 * function. Breaks at spaces; a word wider than a line is broken between characters. Tags such as
 * {@code <col=ff0000>} take no width and are never split; {@code <br>} forces a break.
 */
public final class LineBreaker
{
	private LineBreaker()
	{
	}

	public static List<String> wrap(String text, int maxWidth, IntUnaryOperator charWidth)
	{
		List<String> lines = new ArrayList<>();
		int space = charWidth.applyAsInt(' ');
		for (String paragraph : text.split("<br>", -1))
		{
			StringBuilder line = new StringBuilder();
			int lineWidth = 0;
			for (String word : paragraph.split(" ", -1))
			{
				if (word.isEmpty())
				{
					continue;
				}
				int wordWidth = width(word, charWidth);
				if (line.length() > 0 && lineWidth + space + wordWidth <= maxWidth)
				{
					line.append(' ').append(word);
					lineWidth += space + wordWidth;
					continue;
				}
				if (line.length() > 0)
				{
					lines.add(line.toString());
					line.setLength(0);
					lineWidth = 0;
				}
				if (wordWidth <= maxWidth)
				{
					line.append(word);
					lineWidth = wordWidth;
					continue;
				}
				for (String piece : splitWord(word, maxWidth, charWidth))
				{
					if (line.length() > 0)
					{
						lines.add(line.toString());
					}
					line.setLength(0);
					line.append(piece);
					lineWidth = width(piece, charWidth);
				}
			}
			lines.add(line.toString());
		}
		return lines;
	}

	/** Pixel width of text, not counting tags. */
	public static int width(String text, IntUnaryOperator charWidth)
	{
		int total = 0;
		boolean inTag = false;
		for (int i = 0; i < text.length(); )
		{
			int cp = text.codePointAt(i);
			i += Character.charCount(cp);
			if (cp == '<')
			{
				inTag = true;
			}
			else if (cp == '>' && inTag)
			{
				inTag = false;
			}
			else if (!inTag)
			{
				total += charWidth.applyAsInt(cp);
			}
		}
		return total;
	}

	private static List<String> splitWord(String word, int maxWidth, IntUnaryOperator charWidth)
	{
		List<String> pieces = new ArrayList<>();
		StringBuilder piece = new StringBuilder();
		int pieceWidth = 0;
		for (int i = 0; i < word.length(); )
		{
			int end = word.charAt(i) == '<' && word.indexOf('>', i) > 0 ? word.indexOf('>', i) + 1 : i + Character.charCount(word.codePointAt(i));
			String unit = word.substring(i, end);
			int unitWidth = width(unit, charWidth);
			if (pieceWidth + unitWidth > maxWidth && pieceWidth > 0)
			{
				pieces.add(piece.toString());
				piece.setLength(0);
				pieceWidth = 0;
			}
			piece.append(unit);
			pieceWidth += unitWidth;
			i = end;
		}
		pieces.add(piece.toString());
		return pieces;
	}
}
