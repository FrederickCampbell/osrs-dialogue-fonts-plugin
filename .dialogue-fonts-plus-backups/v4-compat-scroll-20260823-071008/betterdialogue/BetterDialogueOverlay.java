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
import java.awt.Rectangle;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

/**
 * Pure renderer. Native suppression happens BEFORE frame rendering in
 * BetterDialoguePlugin.onBeforeRender(), never here.
 */
@Singleton
public class BetterDialogueOverlay extends Overlay
{
	@Inject
	private Client client;

	@Inject
	private BetterDialogueConfig config;

	@Inject
	private FontRenderer fontRenderer;

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

		fontRenderer.applyRenderingMode(graphics);

		switch (state.getType())
		{
			case NPC_DIALOGUE:
				if (config.replaceNpc())
				{
					renderCharacterDialogue(graphics, state);
				}
				break;

			case PLAYER_DIALOGUE:
				if (config.replacePlayer())
				{
					renderCharacterDialogue(graphics, state);
				}
				break;

			case OPTION_DIALOGUE:
				if (config.replaceOptions())
				{
					renderOptions(graphics, state);
				}
				break;

			case SPRITE_DIALOGUE:
				if (config.replaceSprite())
				{
					renderBody(graphics, state);
				}
				break;

			default:
				break;
		}

		return null;
	}

	private void renderCharacterDialogue(
		Graphics2D g,
		DialogueState state)
	{
		renderName(g, state);
		renderBody(g, state);
	}

	private void renderName(
		Graphics2D g,
		DialogueState state)
	{
		Rectangle bounds = state.getNameBounds();
		String name = state.getNpcName();

		if (bounds == null ||
			name == null ||
			name.isEmpty())
		{
			return;
		}

		Color color =
			state.getNameColor() != null
				? state.getNameColor()
				: new Color(0x800000);

		fontRenderer.drawCenteredString(
			g,
			name,
			bounds,
			color
		);
	}

	private void renderBody(
		Graphics2D g,
		DialogueState state)
	{
		Rectangle bounds = state.getBodyBounds();
		if (bounds == null)
		{
			return;
		}

		fontRenderer.drawWrappedCentered(
			g,
			state.getBodySegments(),
			bounds
		);
	}

	private void renderOptions(
		Graphics2D g,
		DialogueState state)
	{
		String title = state.getNpcName();
		Rectangle titleBounds = state.getNameBounds();

		if (title != null &&
			!title.isEmpty() &&
			titleBounds != null)
		{
			Color titleColor =
				state.getOptionTitleColor() != null
					? state.getOptionTitleColor()
					: new Color(0x800000);

			fontRenderer.drawCenteredString(
				g,
				title,
				titleBounds,
				titleColor
			);
		}

		List<String> options = state.getOptions();
		Rectangle[] bounds = state.getOptionBounds();
		Color[] colors = state.getOptionColors();

		if (options == null || bounds == null)
		{
			return;
		}

		Point mouse = client.getMouseCanvasPosition();
		int count = Math.min(options.size(), bounds.length);

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

			Color normal =
				colors != null &&
				i < colors.length &&
				colors[i] != null
					? colors[i]
					: Color.BLACK;

			Color color =
				hovered
					? Color.WHITE
					: normal;

			fontRenderer.drawCenteredString(
				g,
				options.get(i),
				row,
				color
			);
		}
	}
}
