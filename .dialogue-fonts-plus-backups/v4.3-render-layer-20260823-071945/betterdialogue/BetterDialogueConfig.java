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
		name = "Font & Weight",
		description = "Font family, size, and per-element style",
		position = 0
	)
	String fontSection = "fontSection";

	@ConfigItem(
		keyName = "fontType",
		name = "Font",
		description = "RuneLite's native font picker: RuneLite fonts, ~/.runelite/fonts, and installed system fonts",
		position = 1,
		section = fontSection
	)
	default FontType fontType()
	{
		return FontType.REGULAR.withFamily("Arial").withSize(14);
	}

	@ConfigItem(
		keyName = "bodyStyle",
		name = "Body weight",
		description = "Style applied to NPC/player/item dialogue body text",
		position = 2,
		section = fontSection
	)
	default ElementFontStyle bodyStyle()
	{
		return ElementFontStyle.INHERIT;
	}

	@ConfigItem(
		keyName = "speakerStyle",
		name = "Speaker weight",
		description = "Style applied to NPC/player names and option-menu titles",
		position = 3,
		section = fontSection
	)
	default ElementFontStyle speakerStyle()
	{
		return ElementFontStyle.INHERIT;
	}

	@ConfigItem(
		keyName = "optionStyle",
		name = "Option weight",
		description = "Style applied to dialogue choices, including Quest Helper's [1]/[2] prefixes",
		position = 4,
		section = fontSection
	)
	default ElementFontStyle optionStyle()
	{
		return ElementFontStyle.INHERIT;
	}

	@ConfigItem(
		keyName = "statusStyle",
		name = "Continue / wait weight",
		description = "Style applied to Click here to continue, Press space, Please wait..., and similar status text",
		position = 5,
		section = fontSection
	)
	default ElementFontStyle statusStyle()
	{
		return ElementFontStyle.INHERIT;
	}

	@ConfigItem(
		keyName = "renderingMode",
		name = "Text rendering",
		description = "System Default follows the Modern Chat-style Java2D path. Grayscale forces AA. Crisp disables AA.",
		position = 6,
		section = fontSection
	)
	default TextRenderingMode renderingMode()
	{
		return TextRenderingMode.SYSTEM_DEFAULT;
	}

	@ConfigSection(
		name = "Colors",
		description = "Transparent means inherit the live game/plugin color",
		position = 10
	)
	String colorSection = "colorSection";

	@ConfigItem(
		keyName = "respectExternalColors",
		name = "Plugin colors win",
		description = "Keep special colors supplied by Quest Helper, inline game markup, or other plugins even when a custom base color is set",
		position = 11,
		section = colorSection
	)
	default boolean respectExternalColors()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "bodyColor",
		name = "Body color",
		description = "Transparent = inherit. With Plugin colors win enabled, inline/special colors are preserved.",
		position = 12,
		section = colorSection
	)
	default Color bodyColor()
	{
		return new Color(0, 0, 0, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "speakerColor",
		name = "Speaker / title color",
		description = "Transparent = inherit the live game/plugin color",
		position = 13,
		section = colorSection
	)
	default Color speakerColor()
	{
		return new Color(0, 0, 0, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "optionColor",
		name = "Option color",
		description = "Transparent = inherit. Quest Helper highlights still win by default.",
		position = 14,
		section = colorSection
	)
	default Color optionColor()
	{
		return new Color(0, 0, 0, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "optionHoverColor",
		name = "Option hover color",
		description = "Transparent = native white hover",
		position = 15,
		section = colorSection
	)
	default Color optionHoverColor()
	{
		return new Color(0, 0, 0, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "statusColor",
		name = "Continue / wait color",
		description = "Transparent = inherit Click here / Press space / Please wait colors from the live widget",
		position = 16,
		section = colorSection
	)
	default Color statusColor()
	{
		return new Color(0, 0, 0, 0);
	}

	@ConfigSection(
		name = "Effects",
		description = "Shared text effects",
		position = 20
	)
	String effectsSection = "effectsSection";

	@Range(min = 0, max = 3)
	@ConfigItem(
		keyName = "textShadow",
		name = "Shadow strength",
		description = "Diagonal shadow offset in pixels. 0 disables it.",
		position = 21,
		section = effectsSection
	)
	default int textShadow()
	{
		return 0;
	}

	@Alpha
	@ConfigItem(
		keyName = "shadowColor",
		name = "Shadow color",
		description = "Shadow color and alpha",
		position = 22,
		section = effectsSection
	)
	default Color shadowColor()
	{
		return new Color(0, 0, 0, 180);
	}

	@Range(min = 0, max = 2)
	@ConfigItem(
		keyName = "textOutline",
		name = "Outline strength",
		description = "Outline radius in pixels. 0 disables it.",
		position = 23,
		section = effectsSection
	)
	default int textOutline()
	{
		return 0;
	}

	@Alpha
	@ConfigItem(
		keyName = "outlineColor",
		name = "Outline color",
		description = "Outline color and alpha",
		position = 24,
		section = effectsSection
	)
	default Color outlineColor()
	{
		return new Color(0, 0, 0, 220);
	}

	@Range(min = -2, max = 8)
	@ConfigItem(
		keyName = "lineSpacing",
		name = "Line spacing",
		description = "Extra pixels between wrapped body lines",
		position = 25,
		section = effectsSection
	)
	default int lineSpacing()
	{
		return 0;
	}

	@ConfigSection(
		name = "Overflow",
		description = "Safe handling for large fonts and long dialogue",
		position = 30
	)
	String overflowSection = "overflowSection";

	@ConfigItem(
		keyName = "overflowScrollbar",
		name = "Overflow scrollbar",
		description = "Show a compact OSRS-like scrollbar when the selected font cannot fit the full dialogue body",
		position = 31,
		section = overflowSection
	)
	default boolean overflowScrollbar()
	{
		return true;
	}

	@ConfigItem(
		keyName = "continuePagesOverflow",
		name = "Continue pages overflow first",
		description = "When body text overflows, Click here / Space pages the visual text first; the actual game dialogue advances only after reaching the bottom",
		position = 32,
		section = overflowSection
	)
	default boolean continuePagesOverflow()
	{
		return true;
	}

	@Range(min = 8, max = 64)
	@ConfigItem(
		keyName = "scrollWheelStep",
		name = "Wheel scroll step",
		description = "Pixels scrolled per mouse-wheel notch",
		position = 33,
		section = overflowSection
	)
	default int scrollWheelStep()
	{
		return 24;
	}

	@ConfigSection(
		name = "Dialogue Types",
		description = "Choose what Dialogue Fonts+ replaces",
		position = 40
	)
	String dialogueTypes = "dialogueTypes";

	@ConfigItem(
		keyName = "replaceNpc",
		name = "NPC Dialogue",
		description = "",
		section = dialogueTypes,
		position = 41
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
		position = 42
	)
	default boolean replacePlayer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "replaceOptions",
		name = "Option Menus",
		description = "Preserves Quest Helper text, colors, listeners, and numeric prefixes",
		section = dialogueTypes,
		position = 43
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
		position = 44
	)
	default boolean replaceSprite()
	{
		return true;
	}

	@ConfigItem(
		keyName = "replaceStatus",
		name = "Continue / Wait Status",
		description = "Replace and style Click here to continue, Press space, Please wait..., and other small dialogue status lines while preserving their native listener/text state",
		section = dialogueTypes,
		position = 45
	)
	default boolean replaceStatus()
	{
		return true;
	}

	@ConfigItem(
		keyName = "optionSelectionFeedback",
		name = "Selection feedback",
		description = "Briefly flashes the selected option after mouse or number-key selection so a slow server response never feels like a missed input",
		section = dialogueTypes,
		position = 46
	)
	default boolean optionSelectionFeedback()
	{
		return true;
	}

	@ConfigSection(
		name = "Diagnostics",
		description = "Developer logging for weird dialogue states",
		position = 50,
		closedByDefault = true
	)
	String diagnosticsSection = "diagnosticsSection";

	@ConfigItem(
		keyName = "diagnosticWidgetLog",
		name = "Dialogue widget log",
		description = "Logs dialogue/widget state only when it actually changes. Recommended for normal debugging.",
		section = diagnosticsSection,
		position = 51
	)
	default boolean diagnosticWidgetLog()
	{
		return true;
	}

	@ConfigItem(
		keyName = "verboseMutationLog",
		name = "Verbose per-frame mutations",
		description = "Developer-only. Logs every temporary opacity/text/color mutation; leave OFF unless diagnosing renderer timing.",
		section = diagnosticsSection,
		position = 52
	)
	default boolean verboseMutationLog()
	{
		return false;
	}
}
