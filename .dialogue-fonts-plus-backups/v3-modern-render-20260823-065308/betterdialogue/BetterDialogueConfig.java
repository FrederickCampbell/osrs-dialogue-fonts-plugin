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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.FontType;

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
		description = "RuneLite's native font picker: built-in, custom ~/.runelite/fonts, and installed system fonts",
		position = 1,
		section = fontSection
	)
	default FontType fontType()
	{
		// Regular by construction: italic is explicitly OFF until the user enables it.
		return FontType.REGULAR.withFamily("Arial").withSize(14);
	}

	@ConfigItem(
		keyName = "antiAlias",
		name = "Smooth text",
		description = "Use grayscale anti-aliasing. Unlike LCD subpixel AA, this stays clean under GPU/stretched scaling.",
		position = 2,
		section = fontSection
	)
	default boolean antiAlias()
	{
		return true;
	}

	@ConfigSection(
		name = "Dialogue Types",
		description = "Toggle which dialogue elements get replaced",
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
		description = "",
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

	@ConfigItem(
		keyName = "replaceContinuePrompt",
		name = "Continue Prompt",
		description = "Replace Click/Press-space/Please-wait prompts too. Off leaves the native game prompt untouched (recommended).",
		section = dialogueTypes,
		position = 15
	)
	default boolean replaceContinuePrompt()
	{
		return false;
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
		description = "Logs dialogue widget text/IDs/bounds/font IDs and every mutation made by Dialogue Fonts+.",
		section = diagnosticsSection,
		position = 21
	)
	default boolean diagnosticWidgetLog()
	{
		return true;
	}
}
