/*
 * Copyright (c) 2026, theOranguzang
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.betterdialogue;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class FontRenderer
{
	@Inject
	private BetterDialogueConfig config;

	public Font getFont()
	{
		if (config.fontType() == null)
		{
			return new Font(Font.SANS_SERIF, Font.PLAIN, 14);
		}
		return config.fontType().getFont();
	}

	/**
	 * Option rows are short. Keep the selected family/style, but make them
	 * slightly smaller than dialogue body text.
	 */
	public Font getOptionFont()
	{
		Font base = getFont();
		return base.deriveFont((float) Math.max(10, base.getSize() - 2));
	}

	/**
	 * Continue prompts are also compact in the native UI.
	 */
	public Font getContinueFont()
	{
		Font base = getFont();
		return base.deriveFont((float) Math.max(10, base.getSize() - 2));
	}

	public void applyRenderingHints(Graphics2D g)
	{
		if (config.antiAlias())
		{
			// Grayscale AA is stable when RuneLite's canvas is scaled.
			// Subpixel LCD smoothing can create colored fringes or doubled-looking edges.
			g.setRenderingHint(
				RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON
			);
			g.setRenderingHint(
				RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_OFF
			);
			g.setRenderingHint(
				RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY
			);
		}
		else
		{
			g.setRenderingHint(
				RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
			);
			g.setRenderingHint(
				RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_OFF
			);
		}
	}

	public int drawCenteredString(
		Graphics2D g,
		String text,
		Rectangle bounds,
		int y,
		Color color,
		Font font)
	{
		if (text == null || text.isEmpty())
		{
			return y;
		}

		g.setFont(font);
		g.setColor(color);
		FontMetrics fm = g.getFontMetrics(font);
		int x = bounds.x + (bounds.width - fm.stringWidth(text)) / 2;
		g.drawString(text, x, y + fm.getAscent());
		return y + fm.getHeight();
	}

	public int drawCenteredString(
		Graphics2D g,
		String text,
		Rectangle bounds,
		int y,
		Color color)
	{
		return drawCenteredString(g, text, bounds, y, color, getFont());
	}

	public int drawWrappedText(
		Graphics2D g,
		List<TextSegment> segments,
		Rectangle bounds,
		int startY)
	{
		if (segments == null || segments.isEmpty())
		{
			return startY;
		}

		Font font = getFont();
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics(font);

		int maxWidth = Math.max(1, bounds.width - 8);
		int lineHeight = fm.getHeight();
		int y = startY;

		List<WordToken> tokens = tokenise(segments);
		List<List<WordToken>> lines = layoutLines(tokens, fm, maxWidth);

		for (List<WordToken> line : lines)
		{
			if (y + lineHeight > bounds.y + bounds.height)
			{
				break;
			}

			int lineWidth = measureLine(line, fm);
			int x = bounds.x + (bounds.width - lineWidth) / 2;

			boolean firstToken = true;
			for (WordToken tok : line)
			{
				if (!firstToken)
				{
					g.setColor(tok.color);
					g.drawString(" ", x, y + fm.getAscent());
					x += fm.stringWidth(" ");
				}

				g.setColor(tok.color);
				g.drawString(tok.word, x, y + fm.getAscent());
				x += fm.stringWidth(tok.word);
				firstToken = false;
			}

			y += lineHeight;
		}

		return y;
	}

	private static List<WordToken> tokenise(List<TextSegment> segments)
	{
		List<WordToken> tokens = new ArrayList<>();

		for (TextSegment seg : segments)
		{
			String[] lines = seg.getText().split("\n", -1);
			for (int li = 0; li < lines.length; li++)
			{
				for (String word : lines[li].split(" ", -1))
				{
					if (!word.isEmpty())
					{
						tokens.add(new WordToken(word, seg.getColor(), false));
					}
				}

				if (li < lines.length - 1)
				{
					tokens.add(new WordToken("", seg.getColor(), true));
				}
			}
		}

		return tokens;
	}

	private static List<List<WordToken>> layoutLines(
		List<WordToken> tokens,
		FontMetrics fm,
		int maxWidth)
	{
		List<List<WordToken>> lines = new ArrayList<>();
		List<WordToken> current = new ArrayList<>();
		int currentWidth = 0;
		int spaceWidth = fm.stringWidth(" ");

		for (WordToken tok : tokens)
		{
			if (tok.newline)
			{
				lines.add(new ArrayList<>(current));
				current.clear();
				currentWidth = 0;
				continue;
			}

			int wordWidth = fm.stringWidth(tok.word);
			int widthNeeded = current.isEmpty() ? wordWidth : spaceWidth + wordWidth;

			if (!current.isEmpty() && currentWidth + widthNeeded > maxWidth)
			{
				lines.add(new ArrayList<>(current));
				current.clear();
				currentWidth = 0;
				widthNeeded = wordWidth;
			}

			current.add(tok);
			currentWidth += widthNeeded;
		}

		if (!current.isEmpty())
		{
			lines.add(current);
		}

		return lines;
	}

	private static int measureLine(List<WordToken> line, FontMetrics fm)
	{
		int width = 0;
		int spaceWidth = fm.stringWidth(" ");

		for (int i = 0; i < line.size(); i++)
		{
			if (i > 0)
			{
				width += spaceWidth;
			}
			width += fm.stringWidth(line.get(i).word);
		}

		return width;
	}

	private static final class WordToken
	{
		private final String word;
		private final Color color;
		private final boolean newline;

		private WordToken(String word, Color color, boolean newline)
		{
			this.word = word;
			this.color = color;
			this.newline = newline;
		}
	}
}
