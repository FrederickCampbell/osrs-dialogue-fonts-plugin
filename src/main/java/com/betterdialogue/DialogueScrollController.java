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

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.input.MouseWheelListener;

/**
 * Scroll/paging controller for render-only overflow.
 *
 * It never fabricates a Jagex dialogue page and never touches numbered option
 * hotkeys. Space/click interception happens ONLY while body text overflows and
 * there is still more visual text below the viewport.
 */
@Singleton
public class DialogueScrollController
	implements MouseWheelListener, MouseListener, KeyListener
{
	@Inject
	private MouseManager mouseManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private BetterDialogueConfig config;

	private Rectangle bodyBounds = null;
	private Rectangle statusBounds = null;
	private Rectangle scrollbarTrack = null;
	private Rectangle scrollbarThumb = null;

	private int maxScroll = 0;
	private int scrollOffset = 0;
	private int pageStep = 48;
	private int lineStep = 16;

	private String dialogueKey = "";
	private String statusText = "";

	private boolean dragging = false;
	private boolean swallowStatusClick = false;
	private boolean swallowSpaceSequence = false;
	private int dragStartMouseY = 0;
	private int dragStartScroll = 0;

	public void startUp()
	{
		mouseManager.registerMouseWheelListener(this);
		mouseManager.registerMouseListener(this);
		keyManager.registerKeyListener(this);
	}

	public void shutDown()
	{
		mouseManager.unregisterMouseWheelListener(this);
		mouseManager.unregisterMouseListener(this);
		keyManager.unregisterKeyListener(this);
		reset();
	}

	public void reset()
	{
		bodyBounds = null;
		statusBounds = null;
		scrollbarTrack = null;
		scrollbarThumb = null;
		maxScroll = 0;
		scrollOffset = 0;
		pageStep = 48;
		lineStep = 16;
		dialogueKey = "";
		statusText = "";
		dragging = false;
		swallowStatusClick = false;
		swallowSpaceSequence = false;
	}

	public void updateLayout(
		String newDialogueKey,
		Rectangle newBodyBounds,
		Rectangle newStatusBounds,
		String newStatusText,
		int contentHeight,
		int lineHeight)
	{
		String safeKey =
			newDialogueKey == null ? "" : newDialogueKey;

		if (!safeKey.equals(dialogueKey))
		{
			dialogueKey = safeKey;
			scrollOffset = 0;
			dragging = false;
		}

		bodyBounds =
			newBodyBounds == null
				? null
				: new Rectangle(newBodyBounds);

		statusBounds =
			newStatusBounds == null
				? null
				: new Rectangle(newStatusBounds);

		statusText =
			newStatusText == null ? "" : newStatusText;

		int viewportHeight =
			bodyBounds == null ? 0 : bodyBounds.height;

		maxScroll =
			Math.max(
				0,
				contentHeight - viewportHeight + 4
			);

		scrollOffset =
			clamp(scrollOffset, 0, maxScroll);

		lineStep =
			Math.max(1, lineHeight);

		int visibleWholeLines =
			Math.max(
				1,
				Math.max(1, viewportHeight - 4) /
					lineStep
			);

		pageStep =
			Math.max(
				lineStep,
				Math.max(
					lineStep,
					(visibleWholeLines - 1) *
						lineStep
				)
			);
	}

	public int getScrollOffset()
	{
		return scrollOffset;
	}

	public int getMaxScroll()
	{
		return maxScroll;
	}

	public boolean hasOverflow()
	{
		return maxScroll > 0;
	}

	public void updateScrollbar(
		Rectangle track,
		Rectangle thumb)
	{
		scrollbarTrack =
			track == null ? null : new Rectangle(track);

		scrollbarThumb =
			thumb == null ? null : new Rectangle(thumb);
	}

	private boolean canPageContinue()
	{
		if (!config.continuePagesOverflow() ||
			maxScroll <= 0 ||
			scrollOffset >= maxScroll)
		{
			return false;
		}

		String text =
			statusText.toLowerCase(Locale.ROOT);

		return text.contains("continue") ||
			text.contains("press space");
	}

	private void pageDown()
	{
		scrollOffset =
			snapClamped(
				scrollOffset + pageStep
			);
	}

	private void pageUp()
	{
		scrollOffset =
			snapClamped(
				scrollOffset - pageStep
			);
	}

	@Override
	public MouseWheelEvent mouseWheelMoved(
		MouseWheelEvent event)
	{
		if (maxScroll <= 0 ||
			bodyBounds == null ||
			!bodyBounds.contains(
				event.getX(),
				event.getY()
			))
		{
			return event;
		}

		int requestedStep =
			Math.max(
				lineStep,
				config.scrollWheelStep()
			);

		scrollOffset =
			snapClamped(
				scrollOffset +
					event.getWheelRotation() *
						requestedStep
			);

		event.consume();
		return event;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		// Clear a stale click-sequence guard before evaluating a new press.
		swallowStatusClick = false;

		if (maxScroll <= 0)
		{
			return event;
		}

		if (scrollbarThumb != null &&
			scrollbarThumb.contains(
				event.getX(),
				event.getY()
			))
		{
			dragging = true;
			dragStartMouseY = event.getY();
			dragStartScroll = scrollOffset;
			event.consume();
			return event;
		}

		if (scrollbarTrack != null &&
			scrollbarTrack.contains(
				event.getX(),
				event.getY()
			))
		{
			int trackTop = scrollbarTrack.y;
			int trackRange =
				Math.max(
					1,
					scrollbarTrack.height -
						(scrollbarThumb == null
							? 0
							: scrollbarThumb.height)
				);

			int thumbHeight =
				scrollbarThumb == null
					? 0
					: scrollbarThumb.height;

			int target =
				event.getY() -
				trackTop -
				thumbHeight / 2;

			scrollOffset =
				snapClamped(
					(int) Math.round(
						maxScroll *
						(target / (double) trackRange)
					)
				);

			event.consume();
			return event;
		}

		if (canPageContinue() &&
			statusBounds != null &&
			statusBounds.contains(
				event.getX(),
				event.getY()
			))
		{
			pageDown();
			swallowStatusClick = true;
			event.consume();
		}

		return event;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent event)
	{
		if (!dragging ||
			scrollbarTrack == null ||
			scrollbarThumb == null)
		{
			return event;
		}

		int trackRange =
			Math.max(
				1,
				scrollbarTrack.height -
					scrollbarThumb.height
			);

		int deltaY =
			event.getY() - dragStartMouseY;

		int deltaScroll =
			(int) Math.round(
				maxScroll *
				(deltaY / (double) trackRange)
			);

		scrollOffset =
			clamp(
				dragStartScroll + deltaScroll,
				0,
				maxScroll
			);

		event.consume();
		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		if (dragging)
		{
			dragging = false;
			scrollOffset =
				snapClamped(scrollOffset);
			event.consume();
		}

		if (swallowStatusClick)
		{
			event.consume();
		}

		return event;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		if (swallowStatusClick)
		{
			event.consume();
			swallowStatusClick = false;
		}
		return event;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent event)
	{
		return event;
	}

	@Override
	public void keyPressed(KeyEvent event)
	{
		if (maxScroll <= 0)
		{
			return;
		}

		switch (event.getKeyCode())
		{
			case KeyEvent.VK_PAGE_DOWN:
				pageDown();
				event.consume();
				break;

			case KeyEvent.VK_PAGE_UP:
				pageUp();
				event.consume();
				break;

			case KeyEvent.VK_SPACE:
				if (canPageContinue())
				{
					pageDown();
					swallowSpaceSequence = true;
					event.consume();
				}
				break;

			default:
				// Number keys and Quest Helper option hotkeys pass through.
				break;
		}
	}

	@Override
	public void keyReleased(KeyEvent event)
	{
		if (event.getKeyCode() == KeyEvent.VK_SPACE &&
			swallowSpaceSequence)
		{
			event.consume();
			swallowSpaceSequence = false;
		}
	}

	@Override
	public void keyTyped(KeyEvent event)
	{
		if (event.getKeyChar() == ' ' &&
			swallowSpaceSequence)
		{
			event.consume();
		}
	}

	private int snapClamped(int value)
	{
		int clamped =
			clamp(value, 0, maxScroll);

		if (clamped <= 0 ||
			clamped >= maxScroll ||
			lineStep <= 1)
		{
			return clamped;
		}

		int snapped =
			(int) Math.round(
				clamped / (double) lineStep
			) * lineStep;

		return clamp(snapped, 0, maxScroll);
	}

	private static int clamp(
		int value,
		int min,
		int max)
	{
		return Math.max(
			min,
			Math.min(max, value)
		);
	}
}

