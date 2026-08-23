/*
 * Rendering approach adapted from Modern Chat's TextDrawUtil.
 *
 * Copyright 2023 BenDol
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES.
 */

package com.betterdialogue;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Fast text shadow/outline rendering using Java2D's cached drawString glyph path.
 * This mirrors the practical rendering strategy used by Modern Chat.
 */
public final class TextDrawUtil
{
	private TextDrawUtil()
	{
	}

	public static void drawText(
		Graphics2D g,
		String text,
		int x,
		int baselineY,
		Color textColor,
		Color shadowColor,
		int shadowOffset,
		int outlineThickness)
	{
		if (text == null || text.isEmpty())
		{
			return;
		}

		if (outlineThickness > 0 && shadowColor.getAlpha() > 0)
		{
			g.setColor(shadowColor);
			final int t = outlineThickness;
			for (int dy = -t; dy <= t; dy++)
			{
				for (int dx = -t; dx <= t; dx++)
				{
					if (dx == 0 && dy == 0)
					{
						continue;
					}
					g.drawString(text, x + dx, baselineY + dy);
				}
			}

			g.setColor(textColor);
			g.drawString(text, x, baselineY);
		}
		else if (shadowOffset > 0 && shadowColor.getAlpha() > 0)
		{
			g.setColor(shadowColor);
			g.drawString(text, x + shadowOffset, baselineY + shadowOffset);
			g.setColor(textColor);
			g.drawString(text, x, baselineY);
		}
		else
		{
			g.setColor(textColor);
			g.drawString(text, x, baselineY);
		}
	}
}
