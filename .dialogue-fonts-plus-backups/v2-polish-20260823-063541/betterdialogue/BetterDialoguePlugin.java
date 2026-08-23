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
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.ClientTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
	name = "Dialogue Fonts+",
	description = "Dialogue Fonts fork with installed-system and ~/.runelite/fonts TrueType/OpenType support",
	tags = {"dialogue", "font", "accessibility", "text", "npc", "chat", "readable", "custom font", "system font"}
)
public class BetterDialoguePlugin extends Plugin
{
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BetterDialogueOverlay overlay;

	@Inject
	private DialogueWidgetManager widgetManager;

	@Inject
	private FontResolver fontResolver;

	@Override
	protected void startUp()
	{
		fontResolver.refreshNow();
		overlayManager.add(overlay);
		log.info("Dialogue Fonts+ started; custom font folder: {}", fontResolver.getFontDirectory());
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		widgetManager.restoreAll();
		overlay.setState(null);
		log.debug("Dialogue Fonts+ stopped");
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		DialogueState state = widgetManager.getCurrentDialogue();
		overlay.setState(state);
	}

	@Provides
	BetterDialogueConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BetterDialogueConfig.class);
	}
}
