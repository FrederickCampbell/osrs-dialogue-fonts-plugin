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

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.BeforeRender;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Dialogue Fonts+",
	description = "Ghost-free TrueType OSRS dialogue using RuneLite's native font picker",
	tags = {
		"dialogue",
		"font",
		"accessibility",
		"text",
		"npc",
		"chat",
		"readable",
		"custom font",
		"system font"
	}
)
public class BetterDialoguePlugin extends Plugin
{
	private static final String CONFIG_GROUP = "dialoguefontsplus";

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BetterDialogueOverlay overlay;

	@Inject
	private DialogueWidgetManager widgetManager;

	@Inject
	private DialogueDiagnostics diagnostics;

	@Override
	protected void startUp()
	{
		diagnostics.startSession();
		overlayManager.add(overlay);

		log.info(
			"Dialogue Fonts+ v3 started; diagnostic log: {}",
			diagnostics.getLogFile()
		);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		widgetManager.restoreAll();
		overlay.setState(null);
		diagnostics.endSession();

		log.debug("Dialogue Fonts+ v3 stopped");
	}

	/**
	 * BeforeRender fires after the client tick/scripts but before the frame is
	 * drawn. This is the critical timing fix: native glyphs are suppressed
	 * before Jagex's widgets can paint them, instead of trying to erase them
	 * later from an ABOVE_WIDGETS overlay.
	 */
	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		diagnostics.capturePreMutation();

		DialogueState state =
			widgetManager.captureAndSuppress();

		overlay.setState(state);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		String key = event.getKey();
		if ("replaceNpc".equals(key) ||
			"replacePlayer".equals(key) ||
			"replaceOptions".equals(key) ||
			"replaceSprite".equals(key))
		{
			// Immediately restore native widgets when a replacement toggle
			// changes; the next BeforeRender suppresses only those still enabled.
			widgetManager.restoreAll();
			overlay.setState(null);
		}
	}

	@Provides
	BetterDialogueConfig provideConfig(
		ConfigManager configManager)
	{
		return configManager.getConfig(
			BetterDialogueConfig.class
		);
	}
}
