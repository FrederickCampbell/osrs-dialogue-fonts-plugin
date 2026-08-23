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
import java.awt.Rectangle;
import java.util.List;
import lombok.Data;

/**
 * Render-only snapshot of the currently active dialogue.
 *
 * Native widgets may be hidden or have their visible text neutralized after this
 * snapshot is created, so the overlay never depends on a widget remaining
 * drawable in order to know its geometry.
 */
@Data
public class DialogueState
{
	private final DialogueType type;

	/** NPC/player name, or option-menu title. */
	private final String npcName;

	/** Parsed body text for NPC/player/sprite dialogue. */
	private final List<TextSegment> bodySegments;

	/** Option-menu strings. */
	private final List<String> options;

	private final Rectangle bodyBounds;
	private final Rectangle nameBounds;
	private final Rectangle[] optionBounds;

	private final Color nameColor;
	private final Color optionTitleColor;
	private final Color[] optionColors;
}
