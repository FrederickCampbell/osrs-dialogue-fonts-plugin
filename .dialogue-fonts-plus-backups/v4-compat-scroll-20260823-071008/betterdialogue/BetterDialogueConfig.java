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
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.FontType;
import net.runelite.client.config.Range;

@ConfigGroup("dialoguefontsplus")
public interface BetterDialogueConfig extends Config
{
	@ConfigSection(
		name = "Font",
		description = "Dialogue font and rendering",
		position = 0
	)
	String fontSection = "fontSection";

	@ConfigItem(
		keyName = "fontType",
		name = "Font",
		description = "RuneLite's native font picker: RuneLite, ~/.runelite/fonts, and installed system fonts",
		position = 1,
		section = fontSection
	)
	default FontType fontType()
	{
		return FontType.REGULAR.withFamily("Arial").withSize(14);
	}

	@ConfigItem(
		keyName = "renderingMode",
		name = "Text rendering",
		description = "System Default matches Modern Chat's Java2D path. Grayscale forces grayscale AA. Crisp disables AA.",
		position = 2,
		section = fontSection
	)
	default TextRenderingMode renderingMode()
	{
		return TextRenderingMode.SYSTEM_DEFAULT;
	}

	@Range(min = 0, max = 2)
	@ConfigItem(
		keyName = "textShadow",
		name = "Text shadow",
		description = "Modern Chat-style diagonal shadow in pixels. 0 is cleanest on the parchment dialogue box.",
		position = 3,
		section = fontSection
	)
	default int textShadow()
	{
		return 0;
	}

	@Range(min = 0, max = 1)
	@ConfigItem(
		keyName = "textOutline",
		name = "Text outline",
		description = "Stamped 1px outline. Overrides diagonal shadow when enabled.",
		position = 4,
		section = fontSection
	)
	default int textOutline()
	{
		return 0;
	}

	@Alpha
	@ConfigItem(
		keyName = "shadowColor",
		name = "Shadow / outline color",
		description = "Color used by the optional shadow or outline.",
		position = 5,
		section = fontSection
	)
	default Color shadowColor()
	{
		return new Color(0, 0, 0, 180);
	}

	@Range(min = -2, max = 8)
	@ConfigItem(
		keyName = "lineSpacing",
		name = "Line spacing",
		description = "Extra pixels between wrapped dialogue lines.",
		position = 6,
		section = fontSection
	)
	default int lineSpacing()
	{
		return 0;
	}

	@ConfigSection(
		name = "Dialogue Types",
		description = "Choose which dialogue text is replaced",
		position = 10
	)
	String dialogueTypes = "dialogueTypes";

	@ConfigItem(
		keyName = "replaceNpc",
		name = "NPC Dialogue",
		description = "",
		section = dialogueTypes,
		position = 11
	)
	default boolean replaceNpc()
	{
		return true;
	}

	@ConfigItem(
		keyName = "replacePlayer",
		name = "Player Dialogue",
		description = "",
		section = dialogueTypes,
		position = 12
	)
	default boolean replacePlayer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "replaceOptions",
		name = "Option Menus",
		description = "Uses invisible non-breaking-space sentinels so native option widgets remain interactive without visible ghost text.",
		section = dialogueTypes,
		position = 13
	)
	default boolean replaceOptions()
	{
		return true;
	}

	@ConfigItem(
		keyName = "replaceSprite",
		name = "Item/Action Dialogue",
		description = "",
		section = dialogueTypes,
		position = 14
	)
	default boolean replaceSprite()
	{
		return true;
	}

	@ConfigSection(
		name = "Diagnostics",
		description = "Developer logging for dialogue widgets",
		position = 20,
		closedByDefault = true
	)
	String diagnosticsSection = "diagnosticsSection";

	@ConfigItem(
		keyName = "diagnosticWidgetLog",
		name = "Dialogue widget log",
		description = "Logs the raw dialogue widget tree before suppression plus every hide/text mutation.",
		section = diagnosticsSection,
		position = 21
	)
	default boolean diagnosticWidgetLog()
	{
		return true;
	}
}
