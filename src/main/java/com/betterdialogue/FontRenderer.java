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
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FontRenderer
{
	@Inject
	private BetterDialogueConfig config;

	public Font getBaseFont()
	{
		if (config.fontType() == null)
		{
			return new Font(Font.SANS_SERIF, Font.PLAIN, 14);
		}
		return config.fontType().getFont();
	}

	public Font getFont(ElementFontStyle style)
	{
		Font base = getBaseFont();
		return style == null
			? base
			: style.apply(base);
	}

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
				// Match the practical Modern Chat path: keep the Graphics2D defaults.
				break;
		}
	}

	public Font fitSingleLineFont(
		Graphics2D g,
		String text,
		Rectangle bounds,
		ElementFontStyle style)
	{
		Font selected = getFont(style);

		if (text == null || text.isEmpty() || bounds == null)
		{
			return selected;
		}

		float size = selected.getSize2D();
		Font font = selected;

		while (size > 6f)
		{
			g.setFont(font);
			FontMetrics fm = g.getFontMetrics(font);

			int glyphHeight = fm.getAscent() + fm.getDescent();
			int width = fm.stringWidth(text);

			if (glyphHeight <= Math.max(1, bounds.height - 1) &&
				width <= Math.max(1, bounds.width - 4))
			{
				break;
			}

			size -= 1f;
			font = selected.deriveFont(size);
		}

		return font;
	}

	public void drawSingleLine(
		Graphics2D g,
		String text,
		Rectangle bounds,
		Color color,
		ElementFontStyle style)
	{
		if (text == null || text.isEmpty() || bounds == null)
		{
			return;
		}

		Font font =
			fitSingleLineFont(g, text, bounds, style);

		g.setFont(font);
		FontMetrics fm = g.getFontMetrics(font);

		int x =
			bounds.x +
			(bounds.width - fm.stringWidth(text)) / 2;

		int baseline =
			bounds.y +
			(bounds.height - (fm.getAscent() + fm.getDescent())) / 2 +
			fm.getAscent();

		drawText(g, text, x, baseline, color);
	}

	public WrappedLayout layout(
		Graphics2D g,
		List<TextSegment> segments,
		int maxWidth,
		ElementFontStyle style)
	{
		if (segments == null || segments.isEmpty())
		{
			return WrappedLayout.empty();
		}

		Font font = getFont(style);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics(font);

		int width = Math.max(1, maxWidth);
		int spaceWidth = fm.stringWidth(" ");
		List<WordToken> tokens = tokeniseAndSplit(
			segments,
			fm,
			width
		);

		boolean preserveSourceBreaks =
			sourceBreaksStillFit(
				tokens,
				fm,
				width
			);

		List<VisualLine> lines = new ArrayList<>();
		List<WordToken> current = new ArrayList<>();
		int currentWidth = 0;

		for (WordToken token : tokens)
		{
			if (token.hardBreak)
			{
				if (!preserveSourceBreaks)
				{
					// The custom font already forced one original Jagex line to
					// wrap, so its old <br> positions are now stale. Reflow the
					// paragraph naturally instead of creating tiny orphan lines.
					continue;
				}

				lines.add(
					new VisualLine(
						new ArrayList<>(current),
						currentWidth
					)
				);
				current.clear();
				currentWidth = 0;
				continue;
			}

			int tokenWidth = fm.stringWidth(token.text);
			int needed =
				current.isEmpty()
					? tokenWidth
					: spaceWidth + tokenWidth;

			if (!current.isEmpty() &&
				currentWidth + needed > width)
			{
				lines.add(
					new VisualLine(
						new ArrayList<>(current),
						currentWidth
					)
				);

				current.clear();
				currentWidth = 0;
				needed = tokenWidth;
			}

			current.add(token);
			currentWidth += needed;
		}

		if (!current.isEmpty())
		{
			lines.add(
				new VisualLine(
					new ArrayList<>(current),
					currentWidth
				)
			);
		}

		int glyphHeight =
			fm.getAscent() + fm.getDescent();

		int lineHeight =
			Math.max(
				1,
				glyphHeight + config.lineSpacing()
			);

		int contentHeight =
			lines.isEmpty()
				? 0
				: glyphHeight +
					Math.max(0, lines.size() - 1) * lineHeight;

		return new WrappedLayout(
			font,
			lines,
			lineHeight,
			glyphHeight,
			contentHeight
		);
	}

	public void drawWrapped(
		Graphics2D g,
		WrappedLayout layout,
		Rectangle viewport,
		int scrollOffset,
		boolean verticallyCenter)
	{
		if (layout == null ||
			layout.lines.isEmpty() ||
			viewport == null)
		{
			return;
		}

		g.setFont(layout.font);
		FontMetrics fm =
			g.getFontMetrics(layout.font);

		int top;

		if (verticallyCenter)
		{
			top =
				viewport.y +
				(viewport.height - layout.contentHeight) / 2;
		}
		else
		{
			top = viewport.y + 2 - scrollOffset;
		}

		int baseline = top + fm.getAscent();

		Shape oldClip = g.getClip();
		g.clip(viewport);

		try
		{
			for (VisualLine line : layout.lines)
			{
				if (baseline + fm.getDescent() >= viewport.y &&
					baseline - fm.getAscent() <=
						viewport.y + viewport.height)
				{
					int x =
						viewport.x +
						(viewport.width - line.width) / 2;

					for (int i = 0; i < line.tokens.size(); i++)
					{
						WordToken token = line.tokens.get(i);

						if (i > 0)
						{
							x += fm.stringWidth(" ");
						}

						drawText(
							g,
							token.text,
							x,
							baseline,
							token.color
						);

						x += fm.stringWidth(token.text);
					}
				}

				baseline += layout.lineHeight;
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
			config.outlineColor(),
			config.textOutline()
		);
	}

	private static boolean sourceBreaksStillFit(
		List<WordToken> tokens,
		FontMetrics fm,
		int maxWidth)
	{
		boolean hasBreak = false;
		int lineWidth = 0;
		int spaceWidth = fm.stringWidth(" ");
		boolean firstWord = true;

		for (WordToken token : tokens)
		{
			if (token.hardBreak)
			{
				hasBreak = true;
				lineWidth = 0;
				firstWord = true;
				continue;
			}

			int tokenWidth = fm.stringWidth(token.text);
			lineWidth +=
				firstWord
					? tokenWidth
					: spaceWidth + tokenWidth;

			if (lineWidth > maxWidth)
			{
				return false;
			}

			firstWord = false;
		}

		return hasBreak;
	}

	private static List<WordToken> tokeniseAndSplit(
		List<TextSegment> segments,
		FontMetrics fm,
		int maxWidth)
	{
		List<WordToken> out = new ArrayList<>();

		for (TextSegment segment : segments)
		{
			if (segment == null ||
				segment.getText() == null ||
				segment.getText().isEmpty())
			{
				continue;
			}

			String[] forcedLines =
				segment.getText().split("\\n", -1);

			for (int lineIndex = 0;
				lineIndex < forcedLines.length;
				lineIndex++)
			{
				String line = forcedLines[lineIndex].trim();

				if (!line.isEmpty())
				{
					String[] words =
						line.split("[\\t ]+");

					for (String word : words)
					{
						if (word.isEmpty())
						{
							continue;
						}

						appendWordOrChunks(
							out,
							word,
							segment.getColor(),
							fm,
							maxWidth
						);
					}
				}

				if (lineIndex < forcedLines.length - 1)
				{
					out.add(WordToken.hardBreak());
				}
			}
		}

		return out;
	}

	private static void appendWordOrChunks(
		List<WordToken> out,
		String word,
		Color color,
		FontMetrics fm,
		int maxWidth)
	{
		if (fm.stringWidth(word) <= maxWidth)
		{
			out.add(new WordToken(word, color, false));
			return;
		}

		// Pathological long token: split by characters instead of overflowing.
		StringBuilder chunk = new StringBuilder();

		for (int i = 0; i < word.length(); i++)
		{
			char ch = word.charAt(i);
			String candidate = chunk.toString() + ch;

			if (chunk.length() > 0 &&
				fm.stringWidth(candidate) > maxWidth)
			{
				out.add(
					new WordToken(
						chunk.toString(),
						color,
						false
					)
				);
				chunk.setLength(0);
			}

			chunk.append(ch);
		}

		if (chunk.length() > 0)
		{
			out.add(
				new WordToken(
					chunk.toString(),
					color,
					false
				)
			);
		}
	}

	public static final class WrappedLayout
	{
		private static final WrappedLayout EMPTY =
			new WrappedLayout(
				new Font(Font.SANS_SERIF, Font.PLAIN, 12),
				Collections.emptyList(),
				1,
				1,
				0
			);

		private final Font font;
		private final List<VisualLine> lines;
		private final int lineHeight;
		private final int glyphHeight;
		private final int contentHeight;

		private WrappedLayout(
			Font font,
			List<VisualLine> lines,
			int lineHeight,
			int glyphHeight,
			int contentHeight)
		{
			this.font = font;
			this.lines = lines;
			this.lineHeight = lineHeight;
			this.glyphHeight = glyphHeight;
			this.contentHeight = contentHeight;
		}

		public static WrappedLayout empty()
		{
			return EMPTY;
		}

		public int getContentHeight()
		{
			return contentHeight;
		}

		public int getLineHeight()
		{
			return lineHeight;
		}
	}

	private static final class VisualLine
	{
		private final List<WordToken> tokens;
		private final int width;

		private VisualLine(
			List<WordToken> tokens,
			int width)
		{
			this.tokens = tokens;
			this.width = width;
		}
	}

	private static final class WordToken
	{
		private final String text;
		private final Color color;
		private final boolean hardBreak;

		private WordToken(
			String text,
			Color color,
			boolean hardBreak)
		{
			this.text = text;
			this.color = color;
			this.hardBreak = hardBreak;
		}

		private static WordToken hardBreak()
		{
			return new WordToken("", Color.BLACK, true);
		}
	}
}
