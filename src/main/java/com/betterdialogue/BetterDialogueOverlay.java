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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

@Singleton
public class BetterDialogueOverlay extends Overlay
{
	private static final Color VANILLA_BODY =
		new Color(0x000000);

	private static final Color VANILLA_SPEAKER =
		new Color(0x800000);

	private static final Color VANILLA_STATUS =
		new Color(0x0000FF);

	private static final Color VANILLA_OPTION =
		new Color(0x000000);

	private static final Color VANILLA_HOVER =
		Color.WHITE;

	private static final int SCROLLBAR_WIDTH = 7;
	private static final int SCROLLBAR_GAP = 4;

	@Inject
	private Client client;

	@Inject
	private BetterDialogueConfig config;

	@Inject
	private FontRenderer fontRenderer;

	@Inject
	private DialogueWidgetManager widgetManager;

	@Inject
	private DialogueScrollController scrollController;

	@Inject
	private OptionSelectionFeedback optionSelectionFeedback;

	private volatile DialogueState currentState;

	@Inject
	BetterDialogueOverlay()
	{
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGH);
	}

	public void setState(DialogueState state)
	{
		currentState = state;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		DialogueState state = currentState;

		fontRenderer.applyRenderingMode(graphics);

		// Put the confirmation wash behind the current text so the option glyphs
		// remain perfectly crisp.
		drawSelectionFeedback(graphics);

		if (state == null)
		{
			scrollController.reset();
			optionSelectionFeedback.clearOptions();
			return null;
		}

		if (state.getType() != DialogueType.OPTION_DIALOGUE)
		{
			optionSelectionFeedback.clearOptions();
		}

		switch (state.getType())
		{
			case NPC_DIALOGUE:
				renderCharacterDialogue(graphics, state);
				break;

			case PLAYER_DIALOGUE:
				renderCharacterDialogue(graphics, state);
				break;

			case OPTION_DIALOGUE:
				renderOptions(graphics, state);
				break;

			case SPRITE_DIALOGUE:
				renderSpriteDialogue(graphics, state);
				break;

			default:
				break;
		}

		// Native font IDs intentionally remain -1 through the rest of this frame.
		// The next high-priority BeforeRender restores them before plugin logic.
		return null;
	}

	private void renderCharacterDialogue(
		Graphics2D g,
		DialogueState state)
	{
		if (state.getSpeakerBounds() != null &&
			state.getSpeakerOrTitle() != null &&
			!state.getSpeakerOrTitle().isEmpty())
		{
			fontRenderer.drawSingleLine(
				g,
				state.getSpeakerOrTitle(),
				state.getSpeakerBounds(),
				resolveSpeakerColor(
					state.getSpeakerColor()
				),
				config.speakerStyle()
			);
		}

		renderBodyAndStatus(g, state);
	}

	private void renderSpriteDialogue(
		Graphics2D g,
		DialogueState state)
	{
		renderBodyAndStatus(g, state);
	}

	private void renderBodyAndStatus(
		Graphics2D g,
		DialogueState state)
	{
		Rectangle body =
			state.getBodyBounds();

		if (body != null &&
			state.getBodySegments() != null &&
			!state.getBodySegments().isEmpty())
		{
			List<TextSegment> resolved =
				resolveBodySegments(state);

			int fullWidth =
				Math.max(1, body.width - 8);

			FontRenderer.WrappedLayout layout =
				fontRenderer.layout(
					g,
					resolved,
					fullWidth,
					config.bodyStyle()
				);

			boolean overflow =
				layout.getContentHeight() >
					Math.max(1, body.height - 4);

			Rectangle textViewport =
				new Rectangle(body);

			if (overflow &&
				config.overflowScrollbar())
			{
				textViewport.width =
					Math.max(
						1,
						textViewport.width -
							SCROLLBAR_WIDTH -
							SCROLLBAR_GAP -
							4
					);

				layout =
					fontRenderer.layout(
						g,
						resolved,
						Math.max(
							1,
							textViewport.width - 8
						),
						config.bodyStyle()
					);

				overflow =
					layout.getContentHeight() >
						Math.max(
							1,
							body.height - 4
						);
			}

			scrollController.updateLayout(
				state.getDialogueKey(),
				body,
				state.getStatusBounds(),
				state.getStatusText(),
				layout.getContentHeight(),
				layout.getLineHeight()
			);

			fontRenderer.drawWrapped(
				g,
				layout,
				textViewport,
				scrollController.getScrollOffset(),
				!overflow
			);

			if (overflow &&
				config.overflowScrollbar())
			{
				drawScrollbar(
					g,
					body
				);
			}
			else
			{
				scrollController.updateScrollbar(
					null,
					null
				);
			}
		}
		else
		{
			scrollController.reset();
		}

		if (config.replaceStatus() &&
			state.getStatusBounds() != null &&
			state.getStatusText() != null &&
			!state.getStatusText().isEmpty())
		{
			fontRenderer.drawSingleLine(
				g,
				state.getStatusText(),
				state.getStatusBounds(),
				resolveStatusColor(
					state.getStatusColor()
				),
				config.statusStyle()
			);
		}
	}

	private void renderOptions(
		Graphics2D g,
		DialogueState state)
	{
		scrollController.reset();

		if (state.getStatusBounds() != null &&
			state.getStatusText() != null &&
			!state.getStatusText().isEmpty())
		{
			optionSelectionFeedback.clearOptions();

			fontRenderer.drawSingleLine(
				g,
				state.getStatusText(),
				state.getStatusBounds(),
				resolveStatusColor(
					state.getStatusColor()
				),
				config.statusStyle()
			);
			return;
		}

		if (state.getSpeakerBounds() != null &&
			state.getSpeakerOrTitle() != null &&
			!state.getSpeakerOrTitle().isEmpty())
		{
			fontRenderer.drawSingleLine(
				g,
				state.getSpeakerOrTitle(),
				state.getSpeakerBounds(),
				resolveSpeakerColor(
					state.getOptionTitleColor()
				),
				config.speakerStyle()
			);
		}

		List<String> options = state.getOptions();
		Rectangle[] bounds = state.getOptionBounds();
		Color[] colors = state.getOptionColors();

		optionSelectionFeedback.updateOptions(
			bounds,
			colors
		);

		if (options == null || bounds == null)
		{
			return;
		}

		Point mouse =
			client.getMouseCanvasPosition();

		int count =
			Math.min(
				options.size(),
				bounds.length
			);

		for (int i = 0; i < count; i++)
		{
			Rectangle row = bounds[i];

			if (row == null)
			{
				continue;
			}

			boolean hovered =
				mouse != null &&
				row.contains(
					mouse.getX(),
					mouse.getY()
				);

			Color nativeColor =
				colors != null &&
				i < colors.length &&
				colors[i] != null
					? colors[i]
					: VANILLA_OPTION;

			Color color;

			boolean externallyStyled =
				isExternallyStyledOption(
					options.get(i),
					nativeColor
				);

			if (hovered &&
				!(config.respectExternalColors() &&
					externallyStyled))
			{
				color =
					hasOverride(
						config.optionHoverColor()
					)
						? config.optionHoverColor()
						: VANILLA_HOVER;
			}
			else
			{
				color =
					resolveOptionColor(
						options.get(i),
						nativeColor
					);
			}

			fontRenderer.drawSingleLine(
				g,
				options.get(i),
				row,
				color,
				config.optionStyle()
			);
		}
	}


	private void drawSelectionFeedback(Graphics2D g)
	{
		if (!config.optionSelectionFeedback() ||
			!optionSelectionFeedback.isFlashing())
		{
			return;
		}

		Rectangle row =
			optionSelectionFeedback.getFlashBounds();

		if (row == null)
		{
			return;
		}

		// Short, native-ish acknowledgement: warm parchment/gold wash,
		// strong brown border, and a tiny left-side chevron.
		Color fill =
			new Color(0xFF, 0xD7, 0x7A, 72);

		Color edge =
			new Color(0x61, 0x43, 0x20, 230);

		Color brightEdge =
			new Color(0xF0, 0xD0, 0x82, 235);

		int x = row.x + 2;
		int y = row.y + 1;
		int w = Math.max(1, row.width - 4);
		int h = Math.max(1, row.height - 2);

		g.setColor(fill);
		g.fillRoundRect(x, y, w, h, 5, 5);

		g.setColor(edge);
		g.drawRoundRect(x, y, w - 1, h - 1, 5, 5);

		g.setColor(brightEdge);
		g.drawLine(
			x + 1,
			y + 1,
			x + w - 3,
			y + 1
		);

		int cy = row.y + row.height / 2;
		Polygon chevron = new Polygon();
		chevron.addPoint(row.x + 7, cy - 4);
		chevron.addPoint(row.x + 12, cy);
		chevron.addPoint(row.x + 7, cy + 4);

		g.setColor(edge);
		g.fillPolygon(chevron);
	}

	private void drawScrollbar(
		Graphics2D g,
		Rectangle body)
	{
		int maxScroll =
			scrollController.getMaxScroll();

		if (maxScroll <= 0)
		{
			scrollController.updateScrollbar(
				null,
				null
			);
			return;
		}

		int x =
			body.x +
			body.width -
			SCROLLBAR_WIDTH -
			1;

		int arrowH = 7;

		Rectangle track =
			new Rectangle(
				x,
				body.y + arrowH + 1,
				SCROLLBAR_WIDTH,
				Math.max(
					8,
					body.height -
						(arrowH * 2) -
						2
				)
			);

		int viewport =
			Math.max(1, body.height);

		int content =
			viewport + maxScroll;

		int thumbH =
			Math.max(
				10,
				(int) Math.round(
					track.height *
						(viewport / (double) content)
				)
			);

		thumbH =
			Math.min(
				track.height,
				thumbH
			);

		int travel =
			Math.max(
				0,
				track.height - thumbH
			);

		int thumbY =
			track.y +
			(maxScroll == 0
				? 0
				: (int) Math.round(
					travel *
					(scrollController.getScrollOffset() /
						(double) maxScroll)
				));

		Rectangle thumb =
			new Rectangle(
				track.x,
				thumbY,
				track.width,
				thumbH
			);

		// OSRS-ish parchment/dark-brown micro scrollbar; deliberately subtle.
		Color trackColor =
			new Color(0x4A, 0x3C, 0x26, 120);

		Color thumbColor =
			new Color(0x8E, 0x77, 0x4C, 210);

		Color edgeColor =
			new Color(0x2F, 0x26, 0x18, 220);

		g.setColor(trackColor);
		g.fillRect(
			track.x,
			track.y,
			track.width,
			track.height
		);

		g.setColor(edgeColor);
		g.drawRect(
			track.x,
			track.y,
			track.width - 1,
			track.height - 1
		);

		g.setColor(thumbColor);
		g.fillRect(
			thumb.x + 1,
			thumb.y + 1,
			Math.max(1, thumb.width - 2),
			Math.max(1, thumb.height - 2)
		);

		g.setColor(edgeColor);
		g.drawRect(
			thumb.x,
			thumb.y,
			thumb.width - 1,
			thumb.height - 1
		);

		drawArrow(
			g,
			x,
			body.y,
			SCROLLBAR_WIDTH,
			arrowH,
			true,
			edgeColor
		);

		drawArrow(
			g,
			x,
			body.y + body.height - arrowH,
			SCROLLBAR_WIDTH,
			arrowH,
			false,
			edgeColor
		);

		scrollController.updateScrollbar(
			track,
			thumb
		);
	}

	private static void drawArrow(
		Graphics2D g,
		int x,
		int y,
		int width,
		int height,
		boolean up,
		Color color)
	{
		int midX = x + width / 2;

		Polygon triangle = new Polygon();

		if (up)
		{
			triangle.addPoint(midX, y + 1);
			triangle.addPoint(x + 1, y + height - 1);
			triangle.addPoint(
				x + width - 2,
				y + height - 1
			);
		}
		else
		{
			triangle.addPoint(
				x + 1,
				y + 1
			);
			triangle.addPoint(
				x + width - 2,
				y + 1
			);
			triangle.addPoint(
				midX,
				y + height - 1
			);
		}

		g.setColor(color);
		g.fillPolygon(triangle);
	}

	private List<TextSegment> resolveBodySegments(
		DialogueState state)
	{
		List<TextSegment> resolved =
			new ArrayList<>();

		Color base =
			state.getBodyBaseColor() != null
				? state.getBodyBaseColor()
				: VANILLA_BODY;

		for (TextSegment segment :
			state.getBodySegments())
		{
			Color nativeColor =
				segment.getColor() != null
					? segment.getColor()
					: base;

			Color chosen =
				resolveBodyColor(
					nativeColor,
					base
				);

			resolved.add(
				new TextSegment(
					segment.getText(),
					chosen
				)
			);
		}

		return resolved;
	}

	private Color resolveBodyColor(
		Color nativeColor,
		Color baseColor)
	{
		Color custom = config.bodyColor();

		if (!hasOverride(custom))
		{
			return nativeColor;
		}

		if (config.respectExternalColors())
		{
			boolean inlineSpecial =
				!sameRgb(
					nativeColor,
					baseColor
				);

			boolean specialBase =
				!sameRgb(
					baseColor,
					VANILLA_BODY
				);

			if (inlineSpecial || specialBase)
			{
				return nativeColor;
			}
		}

		return custom;
	}

	private Color resolveSpeakerColor(
		Color nativeColor)
	{
		Color safe =
			nativeColor != null
				? nativeColor
				: VANILLA_SPEAKER;

		Color custom = config.speakerColor();

		if (!hasOverride(custom))
		{
			return safe;
		}

		if (config.respectExternalColors() &&
			!sameRgb(safe, VANILLA_SPEAKER))
		{
			return safe;
		}

		return custom;
	}

	private boolean isExternallyStyledOption(
		String text,
		Color nativeColor)
	{
		Color safe =
			nativeColor != null
				? nativeColor
				: VANILLA_OPTION;

		boolean numbered =
			text != null &&
				text.matches("^\\[\\d+\\]\\s.*");

		boolean specialColor =
			!sameRgb(
				safe,
				VANILLA_OPTION
			) &&
			!sameRgb(
				safe,
				VANILLA_HOVER
			);

		return numbered || specialColor;
	}

	private Color resolveOptionColor(
		String text,
		Color nativeColor)
	{
		Color safe =
			nativeColor != null
				? nativeColor
				: VANILLA_OPTION;

		Color custom =
			config.optionColor();

		if (!hasOverride(custom))
		{
			return safe;
		}

		if (config.respectExternalColors() &&
			isExternallyStyledOption(text, safe))
		{
			return safe;
		}

		return custom;
	}

	private Color resolveStatusColor(
		Color nativeColor)
	{
		Color safe =
			nativeColor != null
				? nativeColor
				: VANILLA_STATUS;

		Color custom =
			config.statusColor();

		if (!hasOverride(custom))
		{
			return safe;
		}

		if (config.respectExternalColors() &&
			!sameRgb(safe, VANILLA_STATUS))
		{
			return safe;
		}

		return custom;
	}

	private static boolean hasOverride(Color color)
	{
		return color != null &&
			color.getAlpha() > 0;
	}

	private static boolean sameRgb(
		Color a,
		Color b)
	{
		if (a == null || b == null)
		{
			return false;
		}

		return (a.getRGB() & 0xFFFFFF) ==
			(b.getRGB() & 0xFFFFFF);
	}
}

