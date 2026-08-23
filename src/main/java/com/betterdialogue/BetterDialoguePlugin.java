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
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Better Dialogue Boxes",
	description = "Compatibility-first custom TrueType OSRS dialogue with Quest Helper preservation and overflow scrolling",
	tags = {
		"dialogue",
		"font",
		"accessibility",
		"text",
		"npc",
		"chat",
		"quest helper",
		"scroll"
	}
)
public class BetterDialoguePlugin extends Plugin
{
	private static final String CONFIG_GROUP =
		"dialoguefontsplus";

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BetterDialogueOverlay overlay;

	@Inject
	private DialogueWidgetManager widgetManager;

	@Inject
	private DialogueDiagnostics diagnostics;

	@Inject
	private DialogueScrollController scrollController;

	@Inject
	private OptionSelectionFeedback optionSelectionFeedback;

	@Inject
	private EventBus eventBus;

	private EventBus.Subscriber restoreFontsSubscriber;

	@Override
	protected void startUp()
	{
		diagnostics.startSession();

		// PluginManager registers @Subscribe methods only AFTER startUp().
		// A second annotated BeforeRender method is illegal because RuneLite
		// requires the exact method name "onBeforeRender". Register the early
		// restore phase programmatically instead.
		restoreFontsSubscriber = eventBus.register(
			BeforeRender.class,
			event -> widgetManager.restoreSuppressedFontsForPluginLogic(),
			1000f
		);

		scrollController.startUp();
		optionSelectionFeedback.startUp();
		overlayManager.add(overlay);

		log.info(
			"Dialogue Fonts+ v4.4.1 started; diagnostics: {}",
			diagnostics.getLogFile()
		);
	}

	@Override
	protected void shutDown()
	{
		if (restoreFontsSubscriber != null)
		{
			eventBus.unregister(restoreFontsSubscriber);
			restoreFontsSubscriber = null;
		}

		overlayManager.remove(overlay);
		widgetManager.restoreAll();
		scrollController.shutDown();
		optionSelectionFeedback.shutDown();
		overlay.setState(null);
		diagnostics.endSession();

		log.debug("Dialogue Fonts+ v4.4.1 stopped");
	}

	/**
	 * Low priority is deliberate. RuneLite invokes higher-priority subscribers
	 * first, so Quest Helper/other plugins get to mutate text/colors/listeners
	 * before Dialogue Fonts+ snapshots the final state.
	 */
	@Subscribe(priority = -1000f)
	public void onBeforeRender(BeforeRender event)
	{
		diagnostics.capturePreMutation();

		DialogueState state =
			widgetManager.captureAndTemporarilySuppress();

		overlay.setState(state);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		widgetManager.restoreAll();

		if ("replaceNpc".equals(event.getKey()) ||
			"replacePlayer".equals(event.getKey()) ||
			"replaceOptions".equals(event.getKey()) ||
			"replaceSprite".equals(event.getKey()) ||
			"replaceStatus".equals(event.getKey()))
		{
			scrollController.reset();
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

