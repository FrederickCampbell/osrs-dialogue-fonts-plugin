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
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

/**
 * Captures dialogue data and suppresses ONLY the native glyph layer before the
 * frame is rendered.
 *
 * NPC/player/sprite body/name widgets are hidden, like Modern Chat hides its
 * legacy layer. Option rows stay interactive but their text is replaced with
 * an invisible non-breaking-space sentinel so click/listener geometry remains.
 *
 * The continue/wait prompt is never touched in v3.
 */
@Slf4j
@Singleton
public class DialogueWidgetManager
{
	private static final int NPC_CHILD_NAME = 4;
	private static final int NPC_CHILD_TEXT = 6;

	private static final int PLAYER_CHILD_NAME = 4;
	private static final int PLAYER_CHILD_TEXT = 6;

	private static final int SPRITE_CHILD_TEXT = 2;

	/**
	 * Non-empty but visually blank. This is intentionally used instead of ""
	 * for option widgets to maximize compatibility with native 1-5 selection
	 * code that may check whether an option's text field is non-empty.
	 */
	private static final String OPTION_SENTINEL = "\u00A0";

	private static final Pattern TAG_STRIP = Pattern.compile("<[^>]*>");

	@Inject
	private Client client;

	@Inject
	private BetterDialogueConfig config;

	@Inject
	private DialogueDiagnostics diagnostics;

	private final Map<Widget, Boolean> savedHiddenStates =
		new IdentityHashMap<>();

	private final Map<Widget, String> savedInteractiveTexts =
		new IdentityHashMap<>();

	private List<TextSegment> cachedNpcBody = Collections.emptyList();
	private String cachedNpcName = "";
	private Rectangle cachedNpcBodyBounds = null;
	private Rectangle cachedNpcNameBounds = null;
	private Color cachedNpcNameColor = new Color(0x800000);

	private List<TextSegment> cachedPlayerBody = Collections.emptyList();
	private String cachedPlayerName = "";
	private Rectangle cachedPlayerBodyBounds = null;
	private Rectangle cachedPlayerNameBounds = null;
	private Color cachedPlayerNameColor = new Color(0x800000);

	private List<TextSegment> cachedSpriteBody = Collections.emptyList();
	private Rectangle cachedSpriteBodyBounds = null;

	private List<String> cachedOptionTexts = Collections.emptyList();
	private Rectangle[] cachedOptionBounds = new Rectangle[0];
	private Color[] cachedOptionColors = new Color[0];
	private String cachedOptionTitle = "";
	private Rectangle cachedOptionTitleBounds = null;
	private Color cachedOptionTitleColor = new Color(0x800000);

	private DialogueType lastSeenType = null;

	public DialogueState captureAndSuppress()
	{
		DialogueState state = detectAndBuild();
		DialogueType currentType = state != null ? state.getType() : null;

		if (currentType != lastSeenType)
		{
			clearCacheFor(lastSeenType);
			lastSeenType = currentType;
		}

		return state;
	}

	public void restoreAll()
	{
		for (Map.Entry<Widget, Boolean> entry :
			savedHiddenStates.entrySet())
		{
			try
			{
				Widget widget = entry.getKey();
				boolean original = entry.getValue();
				if (widget.isSelfHidden() != original)
				{
					diagnostics.recordHiddenMutation(
						"restore",
						widget,
						widget.isSelfHidden(),
						original
					);
					widget.setHidden(original);
				}
			}
			catch (Exception ex)
			{
				log.debug("Unable to restore hidden state", ex);
			}
		}
		savedHiddenStates.clear();

		for (Map.Entry<Widget, String> entry :
			savedInteractiveTexts.entrySet())
		{
			try
			{
				Widget widget = entry.getKey();
				String restore = entry.getValue();
				String current = widget.getText();

				if (restore != null && !restore.equals(current))
				{
					diagnostics.recordTextMutation(
						"restore",
						"RESTORE_INTERACTIVE_TEXT",
						widget,
						current,
						restore
					);
					widget.setText(restore);
				}
			}
			catch (Exception ex)
			{
				log.debug("Unable to restore interactive option text", ex);
			}
		}
		savedInteractiveTexts.clear();

		clearCacheFor(DialogueType.NPC_DIALOGUE);
		clearCacheFor(DialogueType.PLAYER_DIALOGUE);
		clearCacheFor(DialogueType.OPTION_DIALOGUE);
		clearCacheFor(DialogueType.SPRITE_DIALOGUE);
		lastSeenType = null;
	}

	private DialogueState detectAndBuild()
	{
		Widget npcRoot = client.getWidget(InterfaceID.DIALOG_NPC, 0);
		if (isVisible(npcRoot) && config.replaceNpc())
		{
			return buildNpcState();
		}

		Widget playerRoot =
			client.getWidget(InterfaceID.DIALOG_PLAYER, 0);
		if (isVisible(playerRoot) && config.replacePlayer())
		{
			return buildPlayerState();
		}

		Widget optionContainer =
			client.getWidget(InterfaceID.DIALOG_OPTION, 1);
		if (isVisible(optionContainer) && config.replaceOptions())
		{
			return buildOptionState();
		}

		Widget spriteRoot =
			client.getWidget(InterfaceID.DIALOG_SPRITE, 0);
		if (isVisible(spriteRoot) && config.replaceSprite())
		{
			return buildSpriteState();
		}

		return null;
	}

	private DialogueState buildNpcState()
	{
		Widget name =
			client.getWidget(InterfaceID.DIALOG_NPC, NPC_CHILD_NAME);
		Widget body =
			client.getWidget(InterfaceID.DIALOG_NPC, NPC_CHILD_TEXT);

		if (body == null)
		{
			return null;
		}

		captureBody(
			body,
			name,
			true
		);

		hideVisual(body);
		hideVisual(name);

		if (cachedNpcBody.isEmpty() || cachedNpcBodyBounds == null)
		{
			return null;
		}

		return new DialogueState(
			DialogueType.NPC_DIALOGUE,
			cachedNpcName,
			cachedNpcBody,
			null,
			copy(cachedNpcBodyBounds),
			copy(cachedNpcNameBounds),
			null,
			cachedNpcNameColor,
			null,
			null
		);
	}

	private DialogueState buildPlayerState()
	{
		Widget name =
			client.getWidget(
				InterfaceID.DIALOG_PLAYER,
				PLAYER_CHILD_NAME
			);
		Widget body =
			client.getWidget(
				InterfaceID.DIALOG_PLAYER,
				PLAYER_CHILD_TEXT
			);

		if (body == null)
		{
			return null;
		}

		captureBody(
			body,
			name,
			false
		);

		hideVisual(body);
		hideVisual(name);

		if (cachedPlayerBody.isEmpty() ||
			cachedPlayerBodyBounds == null)
		{
			return null;
		}

		return new DialogueState(
			DialogueType.PLAYER_DIALOGUE,
			cachedPlayerName,
			cachedPlayerBody,
			null,
			copy(cachedPlayerBodyBounds),
			copy(cachedPlayerNameBounds),
			null,
			cachedPlayerNameColor,
			null,
			null
		);
	}

	private void captureBody(
		Widget body,
		Widget name,
		boolean npc)
	{
		String rawBody = body.getText();
		Rectangle bodyBounds = body.getBounds();

		if (bodyBounds != null && bodyBounds.width > 0)
		{
			Color bodyColor = color(body.getTextColor());
			if (npc)
			{
				cachedNpcBodyBounds = copy(bodyBounds);
				if (rawBody != null && !rawBody.isEmpty())
				{
					cachedNpcBody =
						parseSegments(rawBody, bodyColor);
				}
			}
			else
			{
				cachedPlayerBodyBounds = copy(bodyBounds);
				if (rawBody != null && !rawBody.isEmpty())
				{
					cachedPlayerBody =
						parseSegments(rawBody, bodyColor);
				}
			}
		}

		if (name != null)
		{
			Rectangle nameBounds = name.getBounds();
			String rawName = name.getText();

			if (npc)
			{
				if (nameBounds != null)
				{
					cachedNpcNameBounds = copy(nameBounds);
				}
				cachedNpcNameColor = color(name.getTextColor());

				if (rawName != null && !rawName.isEmpty())
				{
					cachedNpcName = stripTags(rawName);
				}
			}
			else
			{
				if (nameBounds != null)
				{
					cachedPlayerNameBounds = copy(nameBounds);
				}
				cachedPlayerNameColor = color(name.getTextColor());

				if (rawName != null && !rawName.isEmpty())
				{
					cachedPlayerName = stripTags(rawName);
				}
			}
		}
	}

	private DialogueState buildSpriteState()
	{
		Widget body =
			client.getWidget(
				InterfaceID.DIALOG_SPRITE,
				SPRITE_CHILD_TEXT
			);

		if (body == null)
		{
			return null;
		}

		Rectangle bounds = body.getBounds();
		if (bounds != null && bounds.width > 0)
		{
			cachedSpriteBodyBounds = copy(bounds);
		}

		String raw = body.getText();
		if (raw != null && !raw.isEmpty())
		{
			cachedSpriteBody =
				parseSegments(raw, color(body.getTextColor()));
		}

		hideVisual(body);

		if (cachedSpriteBody.isEmpty() ||
			cachedSpriteBodyBounds == null)
		{
			return null;
		}

		return new DialogueState(
			DialogueType.SPRITE_DIALOGUE,
			null,
			cachedSpriteBody,
			null,
			copy(cachedSpriteBodyBounds),
			null,
			null,
			null,
			null,
			null
		);
	}

	private DialogueState buildOptionState()
	{
		Widget container =
			client.getWidget(InterfaceID.DIALOG_OPTION, 1);

		if (container == null)
		{
			return null;
		}

		Widget[] children = container.getDynamicChildren();
		if (children == null || children.length == 0)
		{
			return null;
		}

		Widget title = children[0];
		if (title != null)
		{
			String rawTitle = title.getText();
			if (rawTitle != null && !rawTitle.isEmpty())
			{
				cachedOptionTitle = stripTags(rawTitle);
			}

			Rectangle titleBounds = title.getBounds();
			if (titleBounds != null)
			{
				cachedOptionTitleBounds = copy(titleBounds);
			}

			cachedOptionTitleColor =
				color(title.getTextColor());

			hideVisual(title);
		}

		List<Widget> liveRows = new ArrayList<>();
		List<String> freshTexts = new ArrayList<>();
		List<Color> freshColors = new ArrayList<>();
		boolean waitPhase = false;

		for (int i = 1; i < children.length; i++)
		{
			Widget row = children[i];
			if (row == null ||
				row.getType() != WidgetType.TEXT ||
				row.isSelfHidden())
			{
				continue;
			}

			liveRows.add(row);

			String raw = row.getText();
			String cleaned =
				raw == null ? "" : stripTags(raw);

			if (isWaitPrompt(cleaned))
			{
				// Let the game's transient wait text render natively.
				waitPhase = true;
				continue;
			}

			if (!cleaned.isEmpty() &&
				!OPTION_SENTINEL.equals(raw))
			{
				freshTexts.add(cleaned);
				freshColors.add(color(row.getTextColor()));
			}

			neutralizeInteractiveText(row);
		}

		if (waitPhase)
		{
			// Do not paint stale options over the game's transient wait state.
			return null;
		}

		if (!freshTexts.isEmpty())
		{
			cachedOptionTexts =
				Collections.unmodifiableList(
					new ArrayList<>(freshTexts)
				);

			cachedOptionColors =
				freshColors.toArray(new Color[0]);
		}

		if (cachedOptionTexts.isEmpty())
		{
			return null;
		}

		int count =
			Math.min(
				cachedOptionTexts.size(),
				liveRows.size()
			);

		Rectangle[] bounds = new Rectangle[count];
		Color[] colors = new Color[count];

		for (int i = 0; i < count; i++)
		{
			Rectangle rowBounds =
				liveRows.get(i).getBounds();

			bounds[i] =
				rowBounds == null
					? null
					: copy(rowBounds);

			colors[i] =
				i < cachedOptionColors.length
					? cachedOptionColors[i]
					: Color.BLACK;
		}

		cachedOptionBounds = bounds;

		return new DialogueState(
			DialogueType.OPTION_DIALOGUE,
			cachedOptionTitle,
			null,
			cachedOptionTexts,
			null,
			copy(cachedOptionTitleBounds),
			copyArray(cachedOptionBounds),
			null,
			cachedOptionTitleColor,
			colors
		);
	}

	private void hideVisual(Widget widget)
	{
		if (widget == null)
		{
			return;
		}

		if (!savedHiddenStates.containsKey(widget))
		{
			savedHiddenStates.put(
				widget,
				widget.isSelfHidden()
			);
		}

		if (!widget.isSelfHidden())
		{
			diagnostics.recordHiddenMutation(
				"before-render",
				widget,
				false,
				true
			);
			widget.setHidden(true);
		}
	}

	private void neutralizeInteractiveText(Widget widget)
	{
		if (widget == null)
		{
			return;
		}

		String current = widget.getText();
		if (current == null ||
			current.isEmpty() ||
			OPTION_SENTINEL.equals(current))
		{
			return;
		}

		// Always keep the most recent real text for clean shutdown restoration.
		savedInteractiveTexts.put(widget, current);

		diagnostics.recordTextMutation(
			"before-render",
			"NEUTRALIZE_INTERACTIVE_TEXT",
			widget,
			current,
			OPTION_SENTINEL
		);

		widget.setText(OPTION_SENTINEL);
	}

	private void clearCacheFor(DialogueType type)
	{
		if (type == null)
		{
			return;
		}

		switch (type)
		{
			case NPC_DIALOGUE:
				cachedNpcBody = Collections.emptyList();
				cachedNpcName = "";
				cachedNpcBodyBounds = null;
				cachedNpcNameBounds = null;
				break;

			case PLAYER_DIALOGUE:
				cachedPlayerBody = Collections.emptyList();
				cachedPlayerName = "";
				cachedPlayerBodyBounds = null;
				cachedPlayerNameBounds = null;
				break;

			case OPTION_DIALOGUE:
				cachedOptionTexts = Collections.emptyList();
				cachedOptionBounds = new Rectangle[0];
				cachedOptionColors = new Color[0];
				cachedOptionTitle = "";
				cachedOptionTitleBounds = null;
				break;

			case SPRITE_DIALOGUE:
				cachedSpriteBody = Collections.emptyList();
				cachedSpriteBodyBounds = null;
				break;

			default:
				break;
		}
	}

	private static boolean isVisible(Widget widget)
	{
		return widget != null && !widget.isHidden();
	}

	private static boolean isWaitPrompt(String text)
	{
		return text != null &&
			("please wait...".equalsIgnoreCase(text) ||
			 "please wait".equalsIgnoreCase(text));
	}

	static String stripTags(String text)
	{
		if (text == null)
		{
			return "";
		}

		String cleaned = text
			.replace("<br>", " ")
			.replace("<lt>", "\uE000")
			.replace("<gt>", "\uE001");

		cleaned =
			TAG_STRIP.matcher(cleaned).replaceAll("");

		return cleaned
			.replace("\uE000", "<")
			.replace("\uE001", ">")
			.replaceAll("\\s{2,}", " ")
			.trim();
	}

	public List<TextSegment> parseSegments(
		String raw,
		Color defaultColor)
	{
		List<TextSegment> segments = new ArrayList<>();

		if (raw == null || raw.isEmpty())
		{
			return segments;
		}

		String text = raw
			.replace("<br>", " ")
			.replace("<lt>", "\uE000")
			.replace("<gt>", "\uE001")
			.replaceAll("\\s{2,}", " ")
			.trim();

		Color currentColor = defaultColor;
		int pos = 0;

		while (pos < text.length())
		{
			int tagStart = text.indexOf('<', pos);

			if (tagStart == -1)
			{
				appendSegment(
					segments,
					restoreAngles(text.substring(pos)),
					currentColor
				);
				break;
			}

			if (tagStart > pos)
			{
				appendSegment(
					segments,
					restoreAngles(
						text.substring(pos, tagStart)
					),
					currentColor
				);
			}

			int tagEnd =
				text.indexOf('>', tagStart);

			if (tagEnd == -1)
			{
				appendSegment(
					segments,
					restoreAngles(
						text.substring(tagStart)
					),
					currentColor
				);
				break;
			}

			String tag =
				text.substring(tagStart + 1, tagEnd);

			if (tag.startsWith("col="))
			{
				try
				{
					currentColor =
						new Color(
							Integer.parseInt(
								tag.substring(4),
								16
							)
						);
				}
				catch (NumberFormatException ignored)
				{
				}
			}
			else if ("/col".equals(tag))
			{
				currentColor = defaultColor;
			}

			// Deliberately ignore <i>, <shad>, etc. Font style comes only
			// from RuneLite's FontType setting.

			pos = tagEnd + 1;
		}

		return segments;
	}

	private static void appendSegment(
		List<TextSegment> segments,
		String text,
		Color color)
	{
		if (text != null && !text.isEmpty())
		{
			segments.add(
				new TextSegment(text, color)
			);
		}
	}

	private static String restoreAngles(String text)
	{
		return text
			.replace("\uE000", "<")
			.replace("\uE001", ">");
	}

	private static Color color(int rgb)
	{
		return new Color(rgb & 0xFFFFFF);
	}

	private static Rectangle copy(Rectangle rectangle)
	{
		return rectangle == null
			? null
			: new Rectangle(rectangle);
	}

	private static Rectangle[] copyArray(Rectangle[] source)
	{
		if (source == null)
		{
			return null;
		}

		Rectangle[] copy =
			new Rectangle[source.length];

		for (int i = 0; i < source.length; i++)
		{
			copy[i] =
				source[i] == null
					? null
					: new Rectangle(source[i]);
		}

		return copy;
	}
}
