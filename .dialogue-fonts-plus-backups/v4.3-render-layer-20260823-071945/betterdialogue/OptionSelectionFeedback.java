/*
 * Copyright (c) 2026, theOranguzang
 * All rights reserved.
 */

package com.betterdialogue;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;

/**
 * Gives immediate visual acknowledgement when the player selects an option.
 *
 * It NEVER consumes the click/number key and NEVER mutates the option widget.
 * The real Jagex/Quest Helper interaction therefore occurs normally while the
 * overlay remembers the selected row for a tiny ~220ms visual flash.
 */
@Singleton
public class OptionSelectionFeedback
	implements MouseListener, KeyListener
{
	private static final long FLASH_NANOS =
		220_000_000L;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private KeyManager keyManager;

	private Rectangle[] optionBounds =
		new Rectangle[0];

	private Rectangle flashBounds = null;
	private long flashUntilNanos = 0L;

	public void startUp()
	{
		mouseManager.registerMouseListener(this);
		keyManager.registerKeyListener(this);
	}

	public void shutDown()
	{
		mouseManager.unregisterMouseListener(this);
		keyManager.unregisterKeyListener(this);
		clear();
	}

	public void updateOptions(
		Rectangle[] bounds,
		Color[] ignoredColors)
	{
		optionBounds = copy(bounds);
	}

	/**
	 * Stops accepting new option selections but intentionally does NOT erase a
	 * flash already in progress. This is what makes the acknowledgement visible
	 * during the immediate transition to "Please wait..." or the next dialogue.
	 */
	public void clearOptions()
	{
		optionBounds = new Rectangle[0];
	}

	public void clear()
	{
		optionBounds = new Rectangle[0];
		flashBounds = null;
		flashUntilNanos = 0L;
	}

	public boolean isFlashing()
	{
		if (flashBounds == null ||
			System.nanoTime() >= flashUntilNanos)
		{
			flashBounds = null;
			return false;
		}

		return true;
	}

	public Rectangle getFlashBounds()
	{
		return isFlashing()
			? new Rectangle(flashBounds)
			: null;
	}

	private void flash(int optionIndex)
	{
		if (optionIndex < 0 ||
			optionIndex >= optionBounds.length ||
			optionBounds[optionIndex] == null)
		{
			return;
		}

		flashBounds =
			new Rectangle(optionBounds[optionIndex]);

		flashUntilNanos =
			System.nanoTime() + FLASH_NANOS;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		for (int i = 0;
			i < optionBounds.length;
			i++)
		{
			Rectangle row = optionBounds[i];

			if (row != null &&
				row.contains(
					event.getX(),
					event.getY()
				))
			{
				flash(i);
				break;
			}
		}

		// Never consume: the original option listener must receive the click.
		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
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
	public MouseEvent mouseDragged(MouseEvent event)
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
		int code = event.getKeyCode();

		if (code >= KeyEvent.VK_1 &&
			code <= KeyEvent.VK_9)
		{
			int index =
				code - KeyEvent.VK_1;

			flash(index);
		}

		// Never consume: native/Quest Helper hotkeys continue unchanged.
	}

	@Override
	public void keyReleased(KeyEvent event)
	{
	}

	@Override
	public void keyTyped(KeyEvent event)
	{
	}

	private static Rectangle[] copy(
		Rectangle[] source)
	{
		if (source == null)
		{
			return new Rectangle[0];
		}

		Rectangle[] out =
			new Rectangle[source.length];

		for (int i = 0; i < source.length; i++)
		{
			out[i] =
				source[i] == null
					? null
					: new Rectangle(source[i]);
		}

		return out;
	}
}
