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

@ConfigGroup("betterdialogue")
public interface BetterDialogueConfig extends Config
{
	/*
	 * IMPORTANT:
	 * Existing keyName values are intentionally unchanged from v4/v4.1 so an
	 * upgrade only reorganizes the panel; it does not wipe user settings.
	 */

	// ---------------------------------------------------------------------
	// APPEARANCE — the settings most people will actually use
	// ---------------------------------------------------------------------

	@ConfigSection(
		name = "Appearance",
		description = "Font and colors. Transparent colors inherit the final game/plugin color.",
		position = 0
	)
	String appearanceSection = "appearanceSection";

	@ConfigItem(
		keyName = "fontType",
		name = "Font",
		description = "Choose a RuneLite font, a font from ~/.runelite/fonts, or an installed system font.",
		position = 1,
		section = appearanceSection
	)
	default FontType fontType()
	{
		return FontType.REGULAR.withFamily("Arial").withSize(14);
	}

	@ConfigItem(
		keyName = "renderingMode",
		name = "Text smoothing",
		description = "System Default is recommended. Grayscale forces anti-aliasing; Crisp disables it.",
		position = 2,
		section = appearanceSection
	)
	default TextRenderingMode renderingMode()
	{
		return TextRenderingMode.SYSTEM_DEFAULT;
	}

	@ConfigItem(
		keyName = "respectExternalColors",
		name = "Preserve plugin colors",
		description = "Recommended. Dialogue Fonts always uses your selected font; Quest Helper/game/plugin colors and option prefixes can still override the base color/content when needed.",
		position = 3,
		section = appearanceSection
	)
	default boolean respectExternalColors()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "bodyColor",
		name = "Dialogue text",
		description = "Transparent = inherit the live game/plugin color.",
		position = 4,
		section = appearanceSection
	)
	default Color bodyColor()
	{
		return new Color(0, 0, 0, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "speakerColor",
		name = "Speaker / title",
		description = "Transparent = inherit the live game/plugin color.",
		position = 5,
		section = appearanceSection
	)
	default Color speakerColor()
	{
		return new Color(0, 0, 0, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "optionColor",
		name = "Choices",
		description = "Transparent = inherit. Quest Helper highlights still win when Preserve plugin colors is enabled.",
		position = 6,
		section = appearanceSection
	)
	default Color optionColor()
	{
		return new Color(0, 0, 0, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "optionHoverColor",
		name = "Choice hover",
		description = "Transparent = use the native white hover color.",
		position = 7,
		section = appearanceSection
	)
	default Color optionHoverColor()
	{
		return new Color(0, 0, 0, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "statusColor",
		name = "Continue / wait",
		description = "Transparent = inherit the live Click here / Press space / Please wait color.",
		position = 8,
		section = appearanceSection
	)
	default Color statusColor()
	{
		return new Color(0, 0, 0, 0);
	}

	// ---------------------------------------------------------------------
	// DIALOGUE ELEMENTS — what the plugin owns
	// ---------------------------------------------------------------------

	@ConfigSection(
		name = "Dialogue Elements",
		description = "Choose which pieces of the native dialogue UI are redrawn.",
		position = 20
	)
	String elementsSection = "elementsSection";

	@ConfigItem(
		keyName = "replaceNpc",
		name = "NPC dialogue",
		description = "Replace NPC speaker names and dialogue text.",
		section = elementsSection,
		position = 21
	)
	default boolean replaceNpc()
	{
		return true;
	}

	@ConfigItem(
		keyName = "replacePlayer",
		name = "Player dialogue",
		description = "Replace your character name and dialogue text.",
		section = elementsSection,
		position = 22
	)
	default boolean replacePlayer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "replaceOptions",
		name = "Dialogue choices",
		description = "Redraw selectable choices in your selected font while preserving Quest Helper prefixes, colors, listeners, and hotkeys.",
		section = elementsSection,
		position = 23
	)
	default boolean replaceOptions()
	{
		return true;
	}

	@ConfigItem(
		keyName = "replaceSprite",
		name = "Item / narration dialogue",
		description = "Replace item, object, action, narrator/environment message-box, and two-item dialogue text.",
		section = elementsSection,
		position = 24
	)
	default boolean replaceSprite()
	{
		return true;
	}

	@ConfigItem(
		keyName = "replaceStatus",
		name = "Continue / wait text",
		description = "Replace Click here to continue, Press space, Please wait..., and similar bottom status text without changing its native listener/state.",
		section = elementsSection,
		position = 25
	)
	default boolean replaceStatus()
	{
		return true;
	}

	// ---------------------------------------------------------------------
	// INTERACTION — small UX behaviors, not visual styling
	// ---------------------------------------------------------------------

	@ConfigSection(
		name = "Interaction",
		description = "Small feedback and input-quality improvements.",
		position = 30
	)
	String interactionSection = "interactionSection";

	@ConfigItem(
		keyName = "optionSelectionFeedback",
		name = "Choice confirmation flash",
		description = "Briefly highlights the option you selected so server delay never looks like a missed click or hotkey.",
		section = interactionSection,
		position = 31
	)
	default boolean optionSelectionFeedback()
	{
		return true;
	}

	// ---------------------------------------------------------------------
	// ADVANCED TYPOGRAPHY — useful, but not first-screen clutter
	// ---------------------------------------------------------------------

	@ConfigSection(
		name = "Advanced Typography",
		description = "Per-element font style and spacing overrides.",
		position = 40,
		closedByDefault = true
	)
	String typographySection = "typographySection";

	@ConfigItem(
		keyName = "bodyStyle",
		name = "Dialogue style",
		description = "Style for NPC/player/item/narration dialogue text. Inherit uses the style selected in Font.",
		position = 41,
		section = typographySection
	)
	default ElementFontStyle bodyStyle()
	{
		return ElementFontStyle.INHERIT;
	}

	@ConfigItem(
		keyName = "speakerStyle",
		name = "Speaker / title style",
		description = "Style for NPC/player names and dialogue-choice titles.",
		position = 42,
		section = typographySection
	)
	default ElementFontStyle speakerStyle()
	{
		return ElementFontStyle.INHERIT;
	}

	@ConfigItem(
		keyName = "optionStyle",
		name = "Choice style",
		description = "Style for dialogue choices, including Quest Helper [1]/[2] prefixes.",
		position = 43,
		section = typographySection
	)
	default ElementFontStyle optionStyle()
	{
		return ElementFontStyle.INHERIT;
	}

	@ConfigItem(
		keyName = "statusStyle",
		name = "Continue / wait style",
		description = "Style for Click here, Press space, Please wait..., and similar status text.",
		position = 44,
		section = typographySection
	)
	default ElementFontStyle statusStyle()
	{
		return ElementFontStyle.INHERIT;
	}

	@Range(min = -2, max = 8)
	@ConfigItem(
		keyName = "lineSpacing",
		name = "Line spacing",
		description = "Extra pixels between wrapped dialogue lines.",
		position = 45,
		section = typographySection
	)
	default int lineSpacing()
	{
		return 0;
	}

	// ---------------------------------------------------------------------
	// EFFECTS — deliberately collapsed
	// ---------------------------------------------------------------------

	@ConfigSection(
		name = "Effects",
		description = "Optional shadow and outline styling.",
		position = 50,
		closedByDefault = true
	)
	String effectsSection = "effectsSection";

	@Range(min = 0, max = 3)
	@ConfigItem(
		keyName = "textShadow",
		name = "Shadow size",
		description = "Diagonal shadow offset in pixels. 0 disables shadow.",
		position = 51,
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
		description = "Shadow color and opacity.",
		position = 52,
		section = effectsSection
	)
	default Color shadowColor()
	{
		return new Color(0, 0, 0, 180);
	}

	@Range(min = 0, max = 2)
	@ConfigItem(
		keyName = "textOutline",
		name = "Outline size",
		description = "Outline radius in pixels. 0 disables outline.",
		position = 53,
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
		description = "Outline color and opacity.",
		position = 54,
		section = effectsSection
	)
	default Color outlineColor()
	{
		return new Color(0, 0, 0, 220);
	}

	// ---------------------------------------------------------------------
	// LARGE TEXT / OVERFLOW — only relevant when text does not fit
	// ---------------------------------------------------------------------

	@ConfigSection(
		name = "Large Text & Scrolling",
		description = "What happens when your selected font is too large for the native dialogue box.",
		position = 60,
		closedByDefault = true
	)
	String overflowSection = "overflowSection";

	@ConfigItem(
		keyName = "overflowScrollbar",
		name = "Show scrollbar when needed",
		description = "Show a compact OSRS-like scrollbar only when dialogue text actually overflows.",
		position = 61,
		section = overflowSection
	)
	default boolean overflowScrollbar()
	{
		return true;
	}

	@ConfigItem(
		keyName = "continuePagesOverflow",
		name = "Continue scrolls text first",
		description = "When text overflows, Click here / Space pages the text first. The game dialogue advances after you reach the bottom.",
		position = 62,
		section = overflowSection
	)
	default boolean continuePagesOverflow()
	{
		return true;
	}

	@Range(min = 8, max = 64)
	@ConfigItem(
		keyName = "scrollWheelStep",
		name = "Mouse-wheel speed",
		description = "Pixels scrolled per wheel notch.",
		position = 63,
		section = overflowSection
	)
	default int scrollWheelStep()
	{
		return 24;
	}

	// ---------------------------------------------------------------------
	// DIAGNOSTICS — hidden from normal users unless debugging
	// ---------------------------------------------------------------------

	@ConfigSection(
		name = "Diagnostics",
		description = "Logging for debugging unusual dialogue states.",
		position = 70,
		closedByDefault = true
	)
	String diagnosticsSection = "diagnosticsSection";

	@ConfigItem(
		keyName = "diagnosticWidgetLog",
		name = "Widget-state log",
		description = "Log dialogue/widget snapshots when the state actually changes.",
		section = diagnosticsSection,
		position = 71
	)
	default boolean diagnosticWidgetLog()
	{
		return false;
	}

	@ConfigItem(
		keyName = "verboseMutationLog",
		name = "Verbose renderer log",
		description = "Developer-only. Log every temporary per-frame renderer mutation. Leave OFF unless diagnosing timing.",
		section = diagnosticsSection,
		position = 72
	)
	default boolean verboseMutationLog()
	{
		return false;
	}
}


