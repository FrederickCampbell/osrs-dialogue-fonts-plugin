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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

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
	 * Modern Chat does not force a custom anti-aliasing mode in its core text
	 * renderer. SYSTEM_DEFAULT deliberately leaves the Graphics2D hints alone.
	 */
	public void applyRenderingMode(Graphics2D g)
	{
		switch (config.renderingMode())
		{
			case GRAYSCALE:
				g.setRenderingHint(
					RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON
				);
				g.setRenderingHint(
					RenderingHints.KEY_FRACTIONALMETRICS,
					RenderingHints.VALUE_FRACTIONALMETRICS_OFF
				);
				break;

			case CRISP:
				g.setRenderingHint(
					RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
				);
				g.setRenderingHint(
					RenderingHints.KEY_FRACTIONALMETRICS,
					RenderingHints.VALUE_FRACTIONALMETRICS_OFF
				);
				break;

			case SYSTEM_DEFAULT:
			default:
				// Intentionally untouched.
				break;
		}
	}

	public void drawCenteredString(
		Graphics2D g,
		String text,
		Rectangle bounds,
		Color color)
	{
		if (text == null || text.isEmpty() || bounds == null)
		{
			return;
		}

		Font font = getFont();
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics(font);

		int x = bounds.x + (bounds.width - fm.stringWidth(text)) / 2;
		int baseline =
			bounds.y +
			(bounds.height - (fm.getAscent() + fm.getDescent())) / 2 +
			fm.getAscent();

		drawText(g, text, x, baseline, color);
	}

	/**
	 * Wraps with the selected TrueType font, measures the entire line block, then
	 * vertically centers that block inside the native OSRS body widget.
	 *
	 * The game's body widgets are centered vertically; the old fork incorrectly
	 * started replacement text at the top edge.
	 */
	public void drawWrappedCentered(
		Graphics2D g,
		List<TextSegment> segments,
		Rectangle bounds)
	{
		if (segments == null || segments.isEmpty() || bounds == null ||
			bounds.width <= 0 || bounds.height <= 0)
		{
			return;
		}

		Font font = getFont();
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics(font);

		int maxWidth = Math.max(1, bounds.width - 8);
		List<List<WordToken>> lines =
			layoutLines(tokenise(segments), fm, maxWidth);

		if (lines.isEmpty())
		{
			return;
		}

		int glyphHeight = fm.getAscent() + fm.getDescent();
		int lineHeight = Math.max(1, glyphHeight + config.lineSpacing());
		int totalHeight =
			glyphHeight + Math.max(0, lines.size() - 1) * lineHeight;

		int firstBaseline =
			bounds.y +
			(bounds.height - totalHeight) / 2 +
			fm.getAscent();

		Shape oldClip = g.getClip();
		g.clip(bounds);
		try
		{
			int baseline = firstBaseline;

			for (List<WordToken> line : lines)
			{
				int lineWidth = measureLine(line, fm);
				int x = bounds.x + (bounds.width - lineWidth) / 2;

				for (int i = 0; i < line.size(); i++)
				{
					WordToken token = line.get(i);

					if (i > 0)
					{
						x += fm.stringWidth(" ");
					}

					drawText(
						g,
						token.word,
						x,
						baseline,
						token.color
					);

					x += fm.stringWidth(token.word);
				}

				baseline += lineHeight;
			}
		}
		finally
		{
			g.setClip(oldClip);
		}
	}

	private void drawText(
		Graphics2D g,
		String text,
		int x,
		int baseline,
		Color color)
	{
		TextDrawUtil.drawText(
			g,
			text,
			x,
			baseline,
			color,
			config.shadowColor(),
			config.textShadow(),
			config.textOutline()
		);
	}

	private static List<WordToken> tokenise(List<TextSegment> segments)
	{
		List<WordToken> tokens = new ArrayList<>();

		for (TextSegment segment : segments)
		{
			String[] words = segment.getText().trim().split("\\s+");
			for (String word : words)
			{
				if (!word.isEmpty())
				{
					tokens.add(new WordToken(word, segment.getColor()));
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

		for (WordToken token : tokens)
		{
			int wordWidth = fm.stringWidth(token.word);
			int needed = current.isEmpty() ? wordWidth : spaceWidth + wordWidth;

			if (!current.isEmpty() && currentWidth + needed > maxWidth)
			{
				lines.add(new ArrayList<>(current));
				current.clear();
				currentWidth = 0;
				needed = wordWidth;
			}

			current.add(token);
			currentWidth += needed;
		}

		if (!current.isEmpty())
		{
			lines.add(current);
		}

		return lines;
	}

	private static int measureLine(
		List<WordToken> line,
		FontMetrics fm)
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

		private WordToken(String word, Color color)
		{
			this.word = word;
			this.color = color;
		}
	}
}
