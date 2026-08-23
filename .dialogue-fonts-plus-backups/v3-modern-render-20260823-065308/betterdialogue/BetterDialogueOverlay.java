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

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

@Singleton
public class BetterDialogueOverlay extends Overlay
{
	private static final Color OPTION_TITLE_COLOR =
		new Color(0x80, 0x00, 0x00);

	private static final Color NAME_COLOR =
		new Color(0x00, 0x00, 0x80);

	private static final int V_PADDING = 4;

	@Inject
	private Client client;

	@Inject
	private BetterDialogueConfig config;

	@Inject
	private FontRenderer fontRenderer;

	@Inject
	private DialogueDiagnostics diagnostics;

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
		if (state == null)
		{
			return null;
		}

		reBlankWidgets(state);
		fontRenderer.applyRenderingHints(graphics);

		switch (state.getType())
		{
			case NPC_DIALOGUE:
				if (config.replaceNpc())
				{
					renderNpcDialogue(graphics, state);
				}
				break;

			case PLAYER_DIALOGUE:
				if (config.replacePlayer())
				{
					renderPlayerDialogue(graphics, state);
				}
				break;

			case OPTION_DIALOGUE:
				if (config.replaceOptions())
				{
					renderOptionDialogue(graphics, state);
				}
				break;

			case SPRITE_DIALOGUE:
				if (config.replaceSprite())
				{
					renderSpriteDialogue(graphics, state);
				}
				break;

			default:
				break;
		}

		return null;
	}

	private void renderNpcDialogue(Graphics2D g, DialogueState state)
	{
		Widget textWidget = state.getTextWidget();
		if (textWidget == null || textWidget.isHidden())
		{
			return;
		}

		Rectangle bounds = textWidget.getBounds();
		if (bounds == null || bounds.width <= 0)
		{
			return;
		}

		renderName(g, state);
		fontRenderer.drawWrappedText(
			g,
			state.getBodySegments(),
			bounds,
			bounds.y + V_PADDING
		);

		if (config.replaceContinuePrompt())
		{
			renderContinueText(
				g,
				state.getContinueWidget(),
				state.getContinueText()
			);
		}
	}

	private void renderPlayerDialogue(Graphics2D g, DialogueState state)
	{
		Widget textWidget = state.getTextWidget();
		if (textWidget == null || textWidget.isHidden())
		{
			return;
		}

		Rectangle bounds = textWidget.getBounds();
		if (bounds == null || bounds.width <= 0)
		{
			return;
		}

		renderName(g, state);
		fontRenderer.drawWrappedText(
			g,
			state.getBodySegments(),
			bounds,
			bounds.y + V_PADDING
		);

		if (config.replaceContinuePrompt())
		{
			renderContinueText(
				g,
				state.getContinueWidget(),
				state.getContinueText()
			);
		}
	}

	private void renderName(Graphics2D g, DialogueState state)
	{
		Widget nameWidget = state.getNameWidget();
		String name = state.getNpcName();

		if (nameWidget == null ||
			nameWidget.isHidden() ||
			name == null ||
			name.isEmpty())
		{
			return;
		}

		Rectangle nameBounds = nameWidget.getBounds();
		if (nameBounds == null || nameBounds.width <= 0)
		{
			return;
		}

		Font font = fontRenderer.getFont();
		fontRenderer.drawCenteredString(
			g,
			name,
			nameBounds,
			centreY(g, nameBounds, font),
			NAME_COLOR,
			font
		);
	}

	private void renderOptionDialogue(Graphics2D g, DialogueState state)
	{
		Widget container = state.getTextWidget();
		if (container == null || container.isHidden())
		{
			return;
		}

		Font optionFont = fontRenderer.getOptionFont();

		Widget titleWidget = state.getNameWidget();
		if (titleWidget != null &&
			!titleWidget.isHidden() &&
			state.getNpcName() != null &&
			!state.getNpcName().isEmpty())
		{
			Rectangle titleBounds = titleWidget.getBounds();
			if (titleBounds != null && titleBounds.width > 0)
			{
				fontRenderer.drawCenteredString(
					g,
					state.getNpcName(),
					titleBounds,
					centreY(g, titleBounds, optionFont),
					OPTION_TITLE_COLOR,
					optionFont
				);
			}
		}

		Widget[] optionWidgets = state.getOptionWidgets();
		List<String> options = state.getOptions();

		if (optionWidgets == null || options == null)
		{
			return;
		}

		Point mouse = client.getMouseCanvasPosition();

		for (int i = 0;
			i < optionWidgets.length && i < options.size();
			i++)
		{
			Widget optWidget = optionWidgets[i];
			if (optWidget == null || optWidget.isHidden())
			{
				continue;
			}

			Rectangle optBounds = optWidget.getBounds();
			if (optBounds == null || optBounds.width <= 0)
			{
				continue;
			}

			boolean hovered =
				mouse != null &&
				optBounds.contains(mouse.getX(), mouse.getY());

			Color textColor = hovered ? Color.WHITE : Color.BLACK;

			fontRenderer.drawCenteredString(
				g,
				options.get(i),
				optBounds,
				centreY(g, optBounds, optionFont),
				textColor,
				optionFont
			);
		}
	}

	private void renderSpriteDialogue(Graphics2D g, DialogueState state)
	{
		Widget textWidget = state.getTextWidget();
		if (textWidget == null || textWidget.isHidden())
		{
			return;
		}

		Rectangle bounds = textWidget.getBounds();
		if (bounds == null || bounds.width <= 0)
		{
			return;
		}

		fontRenderer.drawWrappedText(
			g,
			state.getBodySegments(),
			bounds,
			bounds.y + V_PADDING
		);

		if (config.replaceContinuePrompt())
		{
			renderContinueText(
				g,
				state.getContinueWidget(),
				state.getContinueText()
			);
		}
	}

	private void renderContinueText(
		Graphics2D g,
		Widget widget,
		String fallbackText)
	{
		if (widget == null)
		{
			return;
		}

		String live = widget.getText();
		String text =
			live != null && !live.isEmpty()
				? DialogueWidgetManager.stripTags(live)
				: fallbackText;

		if (text == null || text.isEmpty())
		{
			return;
		}

		Rectangle bounds = widget.getBounds();
		if (bounds == null || bounds.width <= 0)
		{
			return;
		}

		Font font = fontRenderer.getContinueFont();
		g.setFont(font);

		// Overlay rendering is not clipped to the widget, so centering by the
		// actual widget midpoint remains correct even if a long "Press space..."
		// string is wider than the native bitmap-text widget.
		FontMetrics fm = g.getFontMetrics(font);
		int x = bounds.x + (bounds.width - fm.stringWidth(text)) / 2;
		int y = bounds.y + (bounds.height - fm.getHeight()) / 2;

		g.setColor(NAME_COLOR);
		g.drawString(text, x, y + fm.getAscent());
	}

	private static int centreY(
		Graphics2D g,
		Rectangle bounds,
		Font font)
	{
		FontMetrics fm = g.getFontMetrics(font);
		return bounds.y + (bounds.height - fm.getHeight()) / 2;
	}

	private void reBlankWidgets(DialogueState state)
	{
		safeBlank(state.getTextWidget());

		if (state.getType() == DialogueType.OPTION_DIALOGUE)
		{
			safeCamouflage(state.getNameWidget());

			Widget[] optionWidgets = state.getOptionWidgets();
			if (optionWidgets != null)
			{
				for (Widget option : optionWidgets)
				{
					safeCamouflage(option);
				}
			}
			return;
		}

		safeBlank(state.getNameWidget());

		if (config.replaceContinuePrompt())
		{
			safeCamouflage(state.getContinueWidget());
		}
	}

	private void safeBlank(Widget widget)
	{
		if (widget == null)
		{
			return;
		}

		String text = widget.getText();
		if (text != null && !text.isEmpty())
		{
			diagnostics.recordTextMutation(
				"render",
				"BLANK_TEXT",
				widget,
				text,
				""
			);
			widget.setText("");
		}
	}

	private void safeCamouflage(Widget widget)
	{
		if (widget == null)
		{
			return;
		}

		int before = widget.getTextColor();
		if (before != DialogueWidgetManager.OPTION_CAMOUFLAGE_COLOR)
		{
			diagnostics.recordColorMutation(
				"render",
				"CAMOUFLAGE_TEXT",
				widget,
				before,
				DialogueWidgetManager.OPTION_CAMOUFLAGE_COLOR
			);
			widget.setTextColor(
				DialogueWidgetManager.OPTION_CAMOUFLAGE_COLOR
			);
		}
	}
}
