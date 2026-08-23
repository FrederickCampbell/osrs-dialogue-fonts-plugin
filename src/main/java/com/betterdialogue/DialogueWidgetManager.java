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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetType;

/**
 * Compatibility-first last-mile dialogue capture.
 *
 * Dialogue Fonts owns rendering only. Jagex/Quest Helper/other plugins keep
 * ownership of semantic widget state:
 * - text is NEVER rewritten
 * - colors are NEVER rewritten
 * - listeners/hotkeys are NEVER rewritten
 * - Quest Helper [1]/[2]/etc. prefixes remain live
 *
 * Before native rasterization, only the widget's native font archive ID is
 * temporarily set to -1. The live text/color/listeners remain untouched.
 *
 * A high-priority BeforeRender callback restores those font IDs before other
 * plugins run; a low-priority callback captures their final state and suppresses
 * the native font again. This creates a render-only suppression window.
 */
@Slf4j
@Singleton
public class DialogueWidgetManager
{
	private static final int NPC_CHILD_NAME = 4;
	private static final int NPC_CHILD_STATUS = 5;
	private static final int NPC_CHILD_TEXT = 6;

	private static final int PLAYER_CHILD_NAME = 4;
	private static final int PLAYER_CHILD_STATUS = 5;
	private static final int PLAYER_CHILD_TEXT = 6;

	private static final int SPRITE_CHILD_TEXT = 2;
	private static final int DOUBLE_SPRITE_CHILD_TEXT = 2;

	private static final int MAX_SCAN_CHILD = 96;
	private static final int MAX_SCAN_DEPTH = 5;

	private static final Pattern TAG_STRIP =
		Pattern.compile("<[^>]*>");

	@Inject
	private Client client;

	@Inject
	private BetterDialogueConfig config;

	@Inject
	private DialogueDiagnostics diagnostics;

	private final Map<Widget, Integer> suppressedFontIds =
		new IdentityHashMap<>();

	private String lastMesboxText = "";

	public DialogueState captureAndTemporarilySuppress()
	{
		// Safety net. Normally the high-priority BeforeRender callback already
		// restored these before Quest Helper/other plugin handlers ran.
		restoreSuppressedFontsForPluginLogic();

		Widget npcRoot =
			client.getWidget(InterfaceID.DIALOG_NPC, 0);

		if (isVisible(npcRoot) && config.replaceNpc())
		{
			return buildCharacterState(
				DialogueType.NPC_DIALOGUE,
				InterfaceID.DIALOG_NPC,
				NPC_CHILD_NAME,
				NPC_CHILD_TEXT,
				NPC_CHILD_STATUS
			);
		}

		Widget playerRoot =
			client.getWidget(InterfaceID.DIALOG_PLAYER, 0);

		if (isVisible(playerRoot) &&
			config.replacePlayer())
		{
			return buildCharacterState(
				DialogueType.PLAYER_DIALOGUE,
				InterfaceID.DIALOG_PLAYER,
				PLAYER_CHILD_NAME,
				PLAYER_CHILD_TEXT,
				PLAYER_CHILD_STATUS
			);
		}

		Widget optionContainer =
			client.getWidget(InterfaceID.DIALOG_OPTION, 1);

		if (isVisible(optionContainer) &&
			config.replaceOptions())
		{
			return buildOptionState(optionContainer);
		}

		Widget spriteRoot =
			client.getWidget(InterfaceID.DIALOG_SPRITE, 0);

		if (isVisible(spriteRoot) &&
			config.replaceSprite())
		{
			return buildSpriteState(spriteRoot);
		}

		Widget doubleSpriteBody =
			client.getWidget(
				WidgetID.DIALOG_DOUBLE_SPRITE_GROUP_ID,
				DOUBLE_SPRITE_CHILD_TEXT
			);

		if (renderableTextWidget(doubleSpriteBody) &&
			config.replaceSprite())
		{
			return buildDoubleSpriteState();
		}

		if (config.replaceSprite() &&
			!lastMesboxText.isEmpty())
		{
			DialogueState mesbox =
				buildMesboxState();

			if (mesbox != null)
			{
				return mesbox;
			}
		}

		return null;
	}

	public void restoreSuppressedFontsForPluginLogic()
	{
		if (suppressedFontIds.isEmpty())
		{
			return;
		}

		for (Map.Entry<Widget, Integer> entry :
			new ArrayList<>(suppressedFontIds.entrySet()))
		{
			try
			{
				Widget widget = entry.getKey();
				int originalFontId = entry.getValue();

				if (widget.getFontId() == -1)
				{
					diagnostics.recordFontMutation(
						"before-plugin-render",
						"RESTORE_NATIVE_FONT",
						widget,
						-1,
						originalFontId
					);

					widget.setFontId(originalFontId);
				}
			}
			catch (Exception ex)
			{
				log.debug(
					"Unable to restore dialogue widget font",
					ex
				);
			}
		}

		suppressedFontIds.clear();
	}

	public void restoreAll()
	{
		restoreSuppressedFontsForPluginLogic();
		clearMesbox();
	}

	private DialogueState buildCharacterState(
		DialogueType type,
		int groupId,
		int nameChild,
		int bodyChild,
		int statusChild)
	{
		Widget name =
			client.getWidget(groupId, nameChild);

		Widget body =
			client.getWidget(groupId, bodyChild);

		if (!renderableTextWidget(body))
		{
			return null;
		}

		Widget status =
			findStatusWidget(
				groupId,
				statusChild,
				null
			);

		String rawBody = body.getText();
		String rawName =
			name != null ? name.getText() : null;

		List<TextSegment> segments =
			parseSegments(
				rawBody,
				color(body.getTextColor())
			);

		if (segments.isEmpty())
		{
			return null;
		}

		String speaker =
			rawName == null
				? ""
				: stripTags(rawName);

		String statusText =
			status == null ||
			status.getText() == null
				? ""
				: stripTags(status.getText());

		Rectangle bodyBounds = copy(body.getBounds());
		Rectangle nameBounds =
			name == null ? null : copy(name.getBounds());

		Rectangle statusBounds =
			status == null ? null : copy(status.getBounds());

		Color bodyBaseColor =
			color(body.getTextColor());

		Color nameColor =
			name == null
				? new Color(0x800000)
				: color(name.getTextColor());

		Color statusColor =
			status == null
				? new Color(0x0000FF)
				: color(status.getTextColor());

		String key =
			type.name() + "|" +
			speaker + "|" +
			flattenSegments(segments) + "|" +
			rectKey(bodyBounds);

		suppressGlyphs(body);

		if (config.replaceNpc() ||
			config.replacePlayer())
		{
			suppressGlyphs(name);
		}

		if (config.replaceStatus())
		{
			suppressGlyphs(status);
		}

		return new DialogueState(
			type,
			speaker,
			segments,
			null,
			statusText,
			bodyBounds,
			nameBounds,
			null,
			statusBounds,
			bodyBaseColor,
			nameColor,
			null,
			null,
			statusColor,
			key
		);
	}


	private DialogueState buildSpriteState(
		Widget spriteRoot)
	{
		return buildTextOnlyState(
			DialogueType.SPRITE_DIALOGUE,
			InterfaceID.DIALOG_SPRITE,
			SPRITE_CHILD_TEXT,
			spriteRoot,
			false
		);
	}

	private DialogueState buildDoubleSpriteState()
	{
		return buildTextOnlyState(
			DialogueType.SPRITE_DIALOGUE,
			WidgetID.DIALOG_DOUBLE_SPRITE_GROUP_ID,
			DOUBLE_SPRITE_CHILD_TEXT,
			client.getWidget(
				WidgetID.DIALOG_DOUBLE_SPRITE_GROUP_ID,
				0
			),
			false
		);
	}

	public void onMesbox(String text)
	{
		lastMesboxText = stripTags(text);
	}

	public void clearMesbox()
	{
		lastMesboxText = "";
	}




	private DialogueState buildMesboxState()
	{
		if (lastMesboxText.isEmpty())
		{
			return null;
		}

		Widget body =
			v8FindChatboxText(
				lastMesboxText,
				false
			);

		if (body == null)
		{
			return null;
		}

		Widget status =
			v8FindChatboxText(
				"Click here to continue",
				true
			);

		if (status == null)
		{
			status =
				v8FindChatboxText(
					"Please wait...",
					true
				);
		}

		if (status == body)
		{
			status = null;
		}

		List<TextSegment> segments =
			parseSegments(
				body.getText(),
				color(body.getTextColor())
			);

		if (segments.isEmpty())
		{
			return null;
		}

		Rectangle bodyBounds =
			copy(body.getBounds());

		String statusText =
			status == null ||
			status.getText() == null
				? ""
				: stripTags(status.getText());

		String key =
			DialogueType.MESSAGE_BOX.name() +
			"|" +
			flattenSegments(segments) +
			"|" +
			rectKey(bodyBounds);

		suppressGlyphs(body);

		if (config.replaceStatus())
		{
			suppressGlyphs(status);
		}

		return new DialogueState(
			DialogueType.MESSAGE_BOX,
			"",
			segments,
			null,
			statusText,
			bodyBounds,
			null,
			null,
			status == null
				? null
				: copy(status.getBounds()),
			color(body.getTextColor()),
			null,
			null,
			null,
			status == null
				? new Color(0x0000FF)
				: color(status.getTextColor()),
			key
		);
	}

	private Widget findMesboxBody(
		Widget widget,
		String expected,
		int depth,
		Set<Widget> visited)
	{
		if (widget == null ||
			depth > MAX_SCAN_DEPTH + 4 ||
			!visited.add(widget) ||
			widget.isHidden())
		{
			return null;
		}

		if (widget.getType() == WidgetType.TEXT)
		{
			String actual = stripTags(widget.getText());

			if (!actual.isEmpty() &&
				(actual.equals(expected) ||
				 actual.contains(expected) ||
				 expected.contains(actual)))
			{
				return widget;
			}
		}

		Widget found =
			findMesboxBodyInChildren(
				widget.getStaticChildren(),
				expected,
				depth,
				visited
			);

		if (found != null)
		{
			return found;
		}

		found =
			findMesboxBodyInChildren(
				widget.getDynamicChildren(),
				expected,
				depth,
				visited
			);

		if (found != null)
		{
			return found;
		}

		return findMesboxBodyInChildren(
			widget.getNestedChildren(),
			expected,
			depth,
			visited
		);
	}

	private Widget findMesboxBodyInChildren(
		Widget[] children,
		String expected,
		int depth,
		Set<Widget> visited)
	{
		if (children == null)
		{
			return null;
		}

		for (Widget child : children)
		{
			Widget found =
				findMesboxBody(
					child,
					expected,
					depth + 1,
					visited
				);

			if (found != null)
			{
				return found;
			}
		}

		return null;
	}

	private DialogueState buildTextOnlyWidgetState(
		DialogueType type,
		Widget body,
		Widget status)
	{
		if (!renderableTextWidget(body))
		{
			return null;
		}

		List<TextSegment> segments =
			parseSegments(
				body.getText(),
				color(body.getTextColor())
			);

		if (segments.isEmpty())
		{
			return null;
		}

		Rectangle bodyBounds = copy(body.getBounds());

		String statusText =
			status == null ||
			status.getText() == null
				? ""
				: stripTags(status.getText());

		String key =
			type.name() +
			"|" +
			flattenSegments(segments) +
			"|" +
			rectKey(bodyBounds);

		suppressGlyphs(body);

		if (config.replaceStatus())
		{
			suppressGlyphs(status);
		}

		return new DialogueState(
			type,
			"",
			segments,
			null,
			statusText,
			bodyBounds,
			null,
			null,
			status == null
				? null
				: copy(status.getBounds()),
			color(body.getTextColor()),
			null,
			null,
			null,
			status == null
				? new Color(0x0000FF)
				: color(status.getTextColor()),
			key
		);
	}

	private DialogueState buildTextOnlyState(
		DialogueType type,
		int groupId,
		int bodyChild,
		Widget root,
		boolean strongStatusOnly)
	{
		Widget body =
			client.getWidget(groupId, bodyChild);

		if (!renderableTextWidget(body))
		{
			return null;
		}

		Widget status =
			findStatusWidget(
				groupId,
				-1,
				root
			);

		if (status == body)
		{
			status = null;
		}

		if (strongStatusOnly &&
			status != null &&
			scoreStatusWidget(status) < 100)
		{
			status = null;
		}

		List<TextSegment> segments =
			parseSegments(
				body.getText(),
				color(body.getTextColor())
			);

		if (segments.isEmpty())
		{
			return null;
		}

		Rectangle bodyBounds = copy(body.getBounds());

		String statusText =
			status == null ||
			status.getText() == null
				? ""
				: stripTags(status.getText());

		String key =
			type.name() +
			"|" +
			flattenSegments(segments) +
			"|" +
			rectKey(bodyBounds);

		suppressGlyphs(body);

		if (config.replaceStatus())
		{
			suppressGlyphs(status);
		}

		return new DialogueState(
			type,
			"",
			segments,
			null,
			statusText,
			bodyBounds,
			null,
			null,
			status == null
				? null
				: copy(status.getBounds()),
			color(body.getTextColor()),
			null,
			null,
			null,
			status == null
				? new Color(0x0000FF)
				: color(status.getTextColor()),
			key
		);
	}


	private DialogueState buildOptionState(
		Widget container)
	{
		List<Widget> rows =
			collectOptionTextRows(container);

		if (rows.isEmpty())
		{
			return null;
		}

		Widget title = findOptionTitle(rows);

		String titleText =
			title == null ||
			title.getText() == null
				? ""
				: stripTags(title.getText());

		Rectangle titleBounds =
			title == null
				? null
				: copy(title.getBounds());

		Color titleColor =
			title == null
				? new Color(0x800000)
				: color(title.getTextColor());

		List<String> options = new ArrayList<>();
		List<Rectangle> bounds = new ArrayList<>();
		List<Color> colors = new ArrayList<>();
		List<Widget> allTextRows = new ArrayList<>();

		Widget waitWidget = null;
		String waitText = "";
		Color waitColor = new Color(0x0000FF);

		for (Widget row : rows)
		{
			if (row == title)
			{
				continue;
			}

			String raw = row.getText();

			if (raw == null || raw.isEmpty())
			{
				continue;
			}

			String cleaned = stripTags(raw);

			if (cleaned.isEmpty())
			{
				continue;
			}

			allTextRows.add(row);

			if (isWaitPrompt(cleaned))
			{
				waitWidget = row;
				waitText = cleaned;
				waitColor = color(row.getTextColor());
				continue;
			}

			options.add(cleaned);
			bounds.add(copy(row.getBounds()));
			colors.add(color(row.getTextColor()));
		}

		// Quest Helper can mutate either ordinary or nested CHATMENU children.
		// Suppress exactly the widgets we captured after those mutations; text,
		// colors, listeners, prefixes, and hotkeys remain owned by Quest Helper.
		suppressGlyphs(title);

		for (Widget row : allTextRows)
		{
			suppressGlyphs(row);
		}

		if (waitWidget != null)
		{
			return new DialogueState(
				DialogueType.OPTION_DIALOGUE,
				"",
				null,
				Collections.emptyList(),
				waitText,
				null,
				null,
				new Rectangle[0],
				copy(waitWidget.getBounds()),
				null,
				null,
				null,
				new Color[0],
				waitColor,
				DialogueType.OPTION_DIALOGUE.name() +
					"|WAIT|" + waitText
			);
		}

		if (options.isEmpty())
		{
			return null;
		}

		StringBuilder key =
			new StringBuilder(
				DialogueType.OPTION_DIALOGUE.name()
			);

		key.append('|').append(titleText);

		for (String option : options)
		{
			key.append('|').append(option);
		}

		return new DialogueState(
			DialogueType.OPTION_DIALOGUE,
			titleText,
			null,
			Collections.unmodifiableList(options),
			"",
			null,
			titleBounds,
			bounds.toArray(new Rectangle[0]),
			null,
			null,
			null,
			titleColor,
			colors.toArray(new Color[0]),
			null,
			key.toString()
		);
	}

	private List<Widget> collectOptionTextRows(
		Widget container)
	{
		List<Widget> rows = new ArrayList<>();

		Set<Widget> seen =
			Collections.newSetFromMap(
				new IdentityHashMap<>()
			);

		addOptionRows(
			rows,
			seen,
			container.getChildren()
		);

		addOptionRows(
			rows,
			seen,
			container.getStaticChildren()
		);

		addOptionRows(
			rows,
			seen,
			container.getDynamicChildren()
		);

		addOptionRows(
			rows,
			seen,
			container.getNestedChildren()
		);

		rows.sort((left, right) ->
		{
			Rectangle a = left.getBounds();
			Rectangle b = right.getBounds();

			int ay = a == null ? Integer.MAX_VALUE : a.y;
			int by = b == null ? Integer.MAX_VALUE : b.y;

			if (ay != by)
			{
				return Integer.compare(ay, by);
			}

			int ax = a == null ? Integer.MAX_VALUE : a.x;
			int bx = b == null ? Integer.MAX_VALUE : b.x;

			return Integer.compare(ax, bx);
		});

		return rows;
	}

	private void addOptionRows(
		List<Widget> rows,
		Set<Widget> seen,
		Widget[] candidates)
	{
		if (candidates == null)
		{
			return;
		}

		for (Widget row : candidates)
		{
			if (row == null ||
				!seen.add(row) ||
				!renderableTextWidget(row) ||
				row.getType() != WidgetType.TEXT)
			{
				continue;
			}

			String cleaned = stripTags(row.getText());

			if (!cleaned.isEmpty())
			{
				rows.add(row);
			}
		}
	}

	private Widget findOptionTitle(
		List<Widget> rows)
	{
		for (Widget row : rows)
		{
			String text =
				stripTags(row.getText())
					.toLowerCase(Locale.ROOT);

			if (text.contains("select an option") ||
				text.contains("choose an option"))
			{
				return row;
			}
		}

		for (Widget row : rows)
		{
			if (!row.hasListener())
			{
				return row;
			}
		}

		return null;
	}

	private static boolean isWaitPrompt(String text)
	{
		if (text == null)
		{
			return false;
		}

		String normalized =
			text.trim().toLowerCase(Locale.ROOT);

		return normalized.equals("please wait") ||
			normalized.equals("please wait...");
	}

	private void suppressGlyphs(Widget widget)
	{
		if (!renderableTextWidget(widget))
		{
			return;
		}

		if (suppressedFontIds.containsKey(widget))
		{
			return;
		}

		int before = widget.getFontId();

		if (before < 0)
		{
			return;
		}

		suppressedFontIds.put(widget, before);

		diagnostics.recordFontMutation(
			"before-native-render",
			"SUPPRESS_NATIVE_FONT",
			widget,
			before,
			-1
		);

		widget.setFontId(-1);
	}

	private Widget findStatusWidget(
		int groupId,
		int fallbackChild,
		Widget explicitRoot)
	{
		Widget fallback =
			fallbackChild >= 0
				? client.getWidget(groupId, fallbackChild)
				: null;

		StatusCandidate best =
			new StatusCandidate();

		Set<Widget> visited =
			Collections.newSetFromMap(
				new IdentityHashMap<>()
			);

		if (explicitRoot != null)
		{
			scanStatus(
				explicitRoot,
				0,
				visited,
				best
			);
		}

		for (int child = 0;
			child <= MAX_SCAN_CHILD;
			child++)
		{
			Widget widget =
				client.getWidget(groupId, child);

			scanStatus(
				widget,
				0,
				visited,
				best
			);
		}

		return best.widget != null
			? best.widget
			: fallback;
	}

	private void scanStatus(
		Widget widget,
		int depth,
		Set<Widget> visited,
		StatusCandidate best)
	{
		if (widget == null ||
			depth > MAX_SCAN_DEPTH ||
			!visited.add(widget))
		{
			return;
		}

		int score = scoreStatusWidget(widget);

		if (score > best.score)
		{
			best.score = score;
			best.widget = widget;
		}

		scanStatusChildren(
			widget.getStaticChildren(),
			depth,
			visited,
			best
		);

		scanStatusChildren(
			widget.getDynamicChildren(),
			depth,
			visited,
			best
		);

		scanStatusChildren(
			widget.getNestedChildren(),
			depth,
			visited,
			best
		);
	}

	private void scanStatusChildren(
		Widget[] children,
		int depth,
		Set<Widget> visited,
		StatusCandidate best)
	{
		if (children == null)
		{
			return;
		}

		for (Widget child : children)
		{
			scanStatus(
				child,
				depth + 1,
				visited,
				best
			);
		}
	}

	private int scoreStatusWidget(Widget widget)
	{
		if (!renderableTextWidget(widget))
		{
			return 0;
		}

		String raw = widget.getText();

		if (raw == null || raw.isEmpty())
		{
			return 0;
		}

		String text =
			stripTags(raw)
				.toLowerCase(Locale.ROOT);

		int score = 0;

		if (text.contains("click here to continue"))
		{
			score = 120;
		}
		else if (text.contains("click to continue"))
		{
			score = 118;
		}
		else if (text.contains("press space") &&
			text.contains("continue"))
		{
			score = 116;
		}
		else if (text.contains("spacebar") &&
			text.contains("continue"))
		{
			score = 114;
		}
		else if (text.contains("please wait"))
		{
			score = 112;
		}
		else if (text.equals("continue"))
		{
			score = 100;
		}
		else if (widget.hasListener() &&
			widget.getHeight() <= 24)
		{
			// Future-proof fallback for other small status strings.
			score = 50;
		}

		if (score > 0 && widget.hasListener())
		{
			score += 5;
		}

		return score;
	}

	private static boolean renderableTextWidget(
		Widget widget)
	{
		return widget != null &&
			!widget.isHidden();
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
		List<TextSegment> segments =
			new ArrayList<>();

		if (raw == null || raw.isEmpty())
		{
			return segments;
		}

		String text = raw
			.replace("<br>", "\n")
			.replace("<lt>", "\uE000")
			.replace("<gt>", "\uE001")
			.replaceAll("[\\t ]{2,}", " ")
			.trim();

		Color currentColor = defaultColor;
		int pos = 0;

		while (pos < text.length())
		{
			int tagStart =
				text.indexOf('<', pos);

			if (tagStart == -1)
			{
				appendSegment(
					segments,
					restoreAngles(
						text.substring(pos)
					),
					currentColor
				);
				break;
			}

			if (tagStart > pos)
			{
				appendSegment(
					segments,
					restoreAngles(
						text.substring(
							pos,
							tagStart
						)
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
				text.substring(
					tagStart + 1,
					tagEnd
				);

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

			// Intentionally ignore style tags. Weight/italic come from
			// Dialogue Fonts settings, not game markup.
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

	private static String flattenSegments(
		List<TextSegment> segments)
	{
		StringBuilder out = new StringBuilder();

		for (TextSegment segment : segments)
		{
			out.append(segment.getText());
		}

		return out.toString();
	}

	private static boolean isVisible(Widget widget)
	{
		return widget != null &&
			!widget.isHidden();
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

	private static String rectKey(Rectangle rectangle)
	{
		if (rectangle == null)
		{
			return "null";
		}

		return rectangle.x + "," +
			rectangle.y + "," +
			rectangle.width + "," +
			rectangle.height;
	}

	private static final class StatusCandidate
	{
		private Widget widget;
		private int score;
	}


	private static boolean v6MesboxMatches(
		Widget widget,
		String expected)
	{
		if (!renderableTextWidget(widget))
		{
			return false;
		}

		String actual =
			v6NormalizeMesbox(
				widget.getText()
			);

		String wanted =
			v6NormalizeMesbox(expected);

		if (actual.isEmpty() ||
			wanted.isEmpty())
		{
			return false;
		}

		if (actual.equals(wanted))
		{
			return true;
		}

		int shorter =
			Math.min(
				actual.length(),
				wanted.length()
			);

		return shorter >= 12 &&
			(actual.contains(wanted) ||
			 wanted.contains(actual));
	}

	private static boolean v6UsableMesboxBody(
		Widget widget)
	{
		if (!renderableTextWidget(widget))
		{
			return false;
		}

		String text =
			v6NormalizeMesbox(
				widget.getText()
			);

		return !text.isEmpty() &&
			!v6StatusText(text);
	}

	private static boolean v6StatusText(
		String raw)
	{
		String text =
			v6NormalizeMesbox(raw)
				.toLowerCase(Locale.ROOT);

		return text.contains(
				"click here to continue"
			) ||
			text.contains(
				"click to continue"
			) ||
			(text.contains("press space") &&
			 text.contains("continue")) ||
			(text.contains("spacebar") &&
			 text.contains("continue")) ||
			text.contains("please wait");
	}

	private static String v6NormalizeMesbox(
		String raw)
	{
		if (raw == null)
		{
			return "";
		}

		String withBreakSpaces =
			raw.replaceAll(
				"(?i)<br\\s*/?>",
				" "
			);

		return stripTags(withBreakSpaces)
			.replace('\u00A0', ' ')
			.replaceAll("\\s+", " ")
			.trim();
	}


	private Widget v7FindMesboxText(
		Widget widget,
		String expected,
		int depth,
		Set<Widget> visited)
	{
		if (widget == null ||
			depth > MAX_SCAN_DEPTH + 8 ||
			!visited.add(widget))
		{
			return null;
		}

		if (v7MesboxMatches(
			widget,
			expected))
		{
			return widget;
		}

		Widget found =
			v7FindMesboxTextInChildren(
				widget.getChildren(),
				expected,
				depth,
				visited
			);

		if (found != null)
		{
			return found;
		}

		found =
			v7FindMesboxTextInChildren(
				widget.getStaticChildren(),
				expected,
				depth,
				visited
			);

		if (found != null)
		{
			return found;
		}

		found =
			v7FindMesboxTextInChildren(
				widget.getDynamicChildren(),
				expected,
				depth,
				visited
			);

		if (found != null)
		{
			return found;
		}

		return v7FindMesboxTextInChildren(
			widget.getNestedChildren(),
			expected,
			depth,
			visited
		);
	}

	private Widget v7FindMesboxTextInChildren(
		Widget[] children,
		String expected,
		int depth,
		Set<Widget> visited)
	{
		if (children == null)
		{
			return null;
		}

		for (Widget child : children)
		{
			Widget found =
				v7FindMesboxText(
					child,
					expected,
					depth + 1,
					visited
				);

			if (found != null)
			{
				return found;
			}
		}

		return null;
	}

	private static boolean v7MesboxMatches(
		Widget widget,
		String expected)
	{
		if (widget == null ||
			widget.getType() != WidgetType.TEXT ||
			widget.getText() == null)
		{
			return false;
		}

		String actual =
			v7Normalize(
				widget.getText()
			);

		String wanted =
			v7Normalize(expected);

		if (actual.isEmpty() ||
			wanted.isEmpty() ||
			v7StatusText(actual))
		{
			return false;
		}

		if (actual.equals(wanted))
		{
			return true;
		}

		int shorter =
			Math.min(
				actual.length(),
				wanted.length()
			);

		return shorter >= 10 &&
			(actual.contains(wanted) ||
			 wanted.contains(actual));
	}

	private Widget v7FindStatus(
		Widget widget,
		int depth,
		Set<Widget> visited,
		Widget body)
	{
		if (widget == null ||
			depth > MAX_SCAN_DEPTH + 8 ||
			!visited.add(widget))
		{
			return null;
		}

		if (widget != body &&
			widget.getType() == WidgetType.TEXT &&
			widget.getText() != null &&
			v7StatusText(widget.getText()) &&
			renderableTextWidget(widget))
		{
			return widget;
		}

		Widget found =
			v7FindStatusInChildren(
				widget.getChildren(),
				depth,
				visited,
				body
			);

		if (found != null)
		{
			return found;
		}

		found =
			v7FindStatusInChildren(
				widget.getStaticChildren(),
				depth,
				visited,
				body
			);

		if (found != null)
		{
			return found;
		}

		found =
			v7FindStatusInChildren(
				widget.getDynamicChildren(),
				depth,
				visited,
				body
			);

		if (found != null)
		{
			return found;
		}

		return v7FindStatusInChildren(
			widget.getNestedChildren(),
			depth,
			visited,
			body
		);
	}

	private Widget v7FindStatusInChildren(
		Widget[] children,
		int depth,
		Set<Widget> visited,
		Widget body)
	{
		if (children == null)
		{
			return null;
		}

		for (Widget child : children)
		{
			Widget found =
				v7FindStatus(
					child,
					depth + 1,
					visited,
					body
				);

			if (found != null)
			{
				return found;
			}
		}

		return null;
	}

	private static boolean v7StatusText(
		String raw)
	{
		String text =
			v7Normalize(raw)
				.toLowerCase(Locale.ROOT);

		return text.contains(
				"click here to continue"
			) ||
			text.contains(
				"click to continue"
			) ||
			(text.contains("press space") &&
			 text.contains("continue")) ||
			(text.contains("spacebar") &&
			 text.contains("continue")) ||
			text.contains("please wait");
	}

	private static String v7Normalize(
		String raw)
	{
		if (raw == null)
		{
			return "";
		}

		String withBreakSpaces =
			raw.replaceAll(
				"(?i)<br\\s*/?>",
				" "
			);

		return stripTags(withBreakSpaces)
			.replace('\u00A0', ' ')
			.replaceAll("\\s+", " ")
			.trim();
	}


	private Widget v8FindChatboxText(
		String expected,
		boolean exactStatus)
	{
		Widget hiddenFallback = null;

		for (int child = 0;
			child < MAX_SCAN_CHILD + 64;
			child++)
		{
			Widget top =
				client.getWidget(
					net.runelite.api.widgets.WidgetID.CHATBOX_GROUP_ID,
					child
				);

			if (top == null)
			{
				continue;
			}

			Set<Widget> visited =
				Collections.newSetFromMap(
					new IdentityHashMap<>()
				);

			Widget visible =
				v8FindMatchingTextRecursive(
					top,
					expected,
					exactStatus,
					true,
					0,
					visited
				);

			if (visible != null)
			{
				return visible;
			}

			if (hiddenFallback == null)
			{
				visited =
					Collections.newSetFromMap(
						new IdentityHashMap<>()
					);

				hiddenFallback =
					v8FindMatchingTextRecursive(
						top,
						expected,
						exactStatus,
						false,
						0,
						visited
					);
			}
		}

		return hiddenFallback;
	}

	private Widget v8FindMatchingTextRecursive(
		Widget widget,
		String expected,
		boolean exactStatus,
		boolean requireVisible,
		int depth,
		Set<Widget> visited)
	{
		if (widget == null ||
			depth > MAX_SCAN_DEPTH + 10 ||
			!visited.add(widget))
		{
			return null;
		}

		if (widget.getType() == WidgetType.TEXT &&
			widget.getText() != null &&
			(!requireVisible ||
			 renderableTextWidget(widget)))
		{
			String actual =
				v8Normalize(
					widget.getText()
				);

			String wanted =
				v8Normalize(expected);

			boolean match =
				exactStatus
					? actual.equalsIgnoreCase(wanted)
					: v8BodyTextMatches(
						actual,
						wanted
					);

			if (match)
			{
				return widget;
			}
		}

		Widget found =
			v8FindInChildren(
				widget.getChildren(),
				expected,
				exactStatus,
				requireVisible,
				depth,
				visited
			);

		if (found != null)
		{
			return found;
		}

		found =
			v8FindInChildren(
				widget.getStaticChildren(),
				expected,
				exactStatus,
				requireVisible,
				depth,
				visited
			);

		if (found != null)
		{
			return found;
		}

		found =
			v8FindInChildren(
				widget.getDynamicChildren(),
				expected,
				exactStatus,
				requireVisible,
				depth,
				visited
			);

		if (found != null)
		{
			return found;
		}

		return v8FindInChildren(
			widget.getNestedChildren(),
			expected,
			exactStatus,
			requireVisible,
			depth,
			visited
		);
	}

	private Widget v8FindInChildren(
		Widget[] children,
		String expected,
		boolean exactStatus,
		boolean requireVisible,
		int depth,
		Set<Widget> visited)
	{
		if (children == null)
		{
			return null;
		}

		for (Widget child : children)
		{
			Widget found =
				v8FindMatchingTextRecursive(
					child,
					expected,
					exactStatus,
					requireVisible,
					depth + 1,
					visited
				);

			if (found != null)
			{
				return found;
			}
		}

		return null;
	}

	private static boolean v8BodyTextMatches(
		String actual,
		String wanted)
	{
		if (actual.isEmpty() ||
			wanted.isEmpty())
		{
			return false;
		}

		if (actual.equals(wanted))
		{
			return true;
		}

		int shorter =
			Math.min(
				actual.length(),
				wanted.length()
			);

		return shorter >= 10 &&
			(actual.contains(wanted) ||
			 wanted.contains(actual));
	}

	private static String v8Normalize(
		String raw)
	{
		if (raw == null)
		{
			return "";
		}

		String withBreakSpaces =
			raw.replaceAll(
				"(?i)<br\\s*/?>",
				" "
			);

		return stripTags(withBreakSpaces)
			.replace('\u00A0', ' ')
			.replaceAll("\\s+", " ")
			.trim();
	}
}

