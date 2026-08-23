package com.betterdialogue;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Rectangle;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@Slf4j
@Singleton
public class DialogueDiagnostics
{
	private static final DateTimeFormatter TIME =
		DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	private static final int MAX_TOP_LEVEL_CHILD = 32;
	private static final int MAX_DEPTH = 6;

	@Inject
	private Client client;

	@Inject
	private BetterDialogueConfig config;

	private final Path logDirectory =
		Paths.get(System.getProperty("user.home"), ".runelite", "dialogue-fonts-plus");

	private final Path logFile = logDirectory.resolve("dialogue-widget-log.txt");

	private String lastSnapshot = "";
	private boolean sessionStarted = false;

	public Path getLogFile()
	{
		return logFile;
	}

	public synchronized void startSession()
	{
		if (!config.diagnosticWidgetLog())
		{
			return;
		}

		try
		{
			Files.createDirectories(logDirectory);
			String header =
				"Dialogue Fonts+ diagnostic session\n" +
				"Started: " + LocalDateTime.now().format(TIME) + "\n" +
				"NOTE: PRE snapshots are captured before Dialogue Fonts+ mutates widgets.\n" +
				"Log: " + logFile + "\n" +
				"============================================================\n";
			Files.write(
				logFile,
				header.getBytes(StandardCharsets.UTF_8),
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING
			);
			sessionStarted = true;
		}
		catch (IOException ex)
		{
			log.warn("Dialogue Fonts+: unable to start diagnostic log {}", logFile, ex);
		}
	}

	public synchronized void endSession()
	{
		if (!sessionStarted)
		{
			return;
		}

		append("\n============================================================\nEnded: " +
			LocalDateTime.now().format(TIME) + "\n");
		sessionStarted = false;
	}

	/**
	 * Captures the raw widget tree before DialogueWidgetManager blanks or
	 * camouflages anything. A new snapshot is written only when it changes.
	 */
	public synchronized void capturePreMutation()
	{
		if (!config.diagnosticWidgetLog())
		{
			return;
		}

		ensureSession();

		String snapshot = buildActiveSnapshot();
		if (snapshot.equals(lastSnapshot))
		{
			return;
		}

		if (snapshot.isEmpty())
		{
			if (!lastSnapshot.isEmpty())
			{
				append("\n[" + now() + "] DIALOGUE CLOSED\n");
			}
		}
		else
		{
			append("\n[" + now() + "] PRE-MUTATION WIDGET SNAPSHOT\n" + snapshot);
		}

		lastSnapshot = snapshot;
	}

	public synchronized void recordTextMutation(
		String source,
		String action,
		Widget widget,
		String before,
		String after)
	{
		if (!config.diagnosticWidgetLog())
		{
			return;
		}

		ensureSession();
		append(
			"[" + now() + "] MUTATION " + source + " " + action + " " +
				compactWidget(widget) +
				" before=\"" + escape(before) + "\"" +
				" after=\"" + escape(after) + "\"\n"
		);
	}

	public synchronized void recordColorMutation(
		String source,
		String action,
		Widget widget,
		int before,
		int after)
	{
		if (!config.diagnosticWidgetLog())
		{
			return;
		}

		ensureSession();
		append(
			"[" + now() + "] MUTATION " + source + " " + action + " " +
				compactWidget(widget) +
				String.format(" color=#%06X -> #%06X%n", before & 0xFFFFFF, after & 0xFFFFFF)
		);
	}

	private void ensureSession()
	{
		if (!sessionStarted)
		{
			startSession();
		}
	}

	private String buildActiveSnapshot()
	{
		StringBuilder out = new StringBuilder();

		appendGroupIfVisible(out, "NPC", InterfaceID.DIALOG_NPC, 0);
		appendGroupIfVisible(out, "PLAYER", InterfaceID.DIALOG_PLAYER, 0);
		appendGroupIfVisible(out, "OPTION", InterfaceID.DIALOG_OPTION, 1);
		appendGroupIfVisible(out, "SPRITE", InterfaceID.DIALOG_SPRITE, 0);

		return out.toString();
	}

	private void appendGroupIfVisible(
		StringBuilder out,
		String name,
		int groupId,
		int visibilityChild)
	{
		Widget visibleRoot = client.getWidget(groupId, visibilityChild);
		if (visibleRoot == null || visibleRoot.isHidden())
		{
			return;
		}

		out.append("\n--- ").append(name)
			.append(" group=").append(groupId)
			.append(" ---\n");

		Set<Widget> visited =
			Collections.newSetFromMap(new IdentityHashMap<>());

		for (int child = 0; child <= MAX_TOP_LEVEL_CHILD; child++)
		{
			Widget widget = client.getWidget(groupId, child);
			if (widget != null)
			{
				appendWidgetRecursive(out, widget, "S " + groupId + "." + child, 0, visited);
			}
		}
	}

	private void appendWidgetRecursive(
		StringBuilder out,
		Widget widget,
		String path,
		int depth,
		Set<Widget> visited)
	{
		if (widget == null || depth > MAX_DEPTH || !visited.add(widget))
		{
			return;
		}

		out.append(describeWidget(path, widget)).append('\n');

		appendChildren(out, widget.getStaticChildren(), path + "/S", depth, visited);
		appendChildren(out, widget.getDynamicChildren(), path + "/D", depth, visited);
		appendChildren(out, widget.getNestedChildren(), path + "/N", depth, visited);
	}

	private void appendChildren(
		StringBuilder out,
		Widget[] children,
		String prefix,
		int depth,
		Set<Widget> visited)
	{
		if (children == null)
		{
			return;
		}

		for (int i = 0; i < children.length; i++)
		{
			Widget child = children[i];
			if (child != null)
			{
				appendWidgetRecursive(out, child, prefix + "[" + i + "]", depth + 1, visited);
			}
		}
	}

	private String describeWidget(String path, Widget w)
	{
		Rectangle b = w.getBounds();
		String bounds = b == null
			? "null"
			: b.x + "," + b.y + "," + b.width + "," + b.height;

		return String.format(
			"%s id=0x%08X parent=0x%08X index=%d type=%d hidden=%s selfHidden=%s listener=%s " +
				"fontId=%d color=#%06X shadow=%s lineHeight=%d align=%d/%d rel=%d,%d size=%dx%d bounds=%s text=\"%s\"",
			path,
			w.getId(),
			w.getParentId(),
			w.getIndex(),
			w.getType(),
			w.isHidden(),
			w.isSelfHidden(),
			w.hasListener(),
			w.getFontId(),
			w.getTextColor() & 0xFFFFFF,
			w.getTextShadowed(),
			w.getLineHeight(),
			w.getXTextAlignment(),
			w.getYTextAlignment(),
			w.getRelativeX(),
			w.getRelativeY(),
			w.getWidth(),
			w.getHeight(),
			bounds,
			escape(w.getText())
		);
	}

	private String compactWidget(Widget w)
	{
		if (w == null)
		{
			return "widget=null";
		}

		Rectangle b = w.getBounds();
		return String.format(
			"id=0x%08X parent=0x%08X index=%d fontId=%d bounds=%s",
			w.getId(),
			w.getParentId(),
			w.getIndex(),
			w.getFontId(),
			b == null ? "null" : (b.x + "," + b.y + "," + b.width + "," + b.height)
		);
	}

	private static String escape(String s)
	{
		if (s == null)
		{
			return "<null>";
		}

		return s
			.replace("\\", "\\\\")
			.replace("\r", "\\r")
			.replace("\n", "\\n")
			.replace("\"", "\\\"");
	}

	private static String now()
	{
		return LocalDateTime.now().format(TIME);
	}

	private void append(String text)
	{
		try
		{
			Files.createDirectories(logDirectory);
			Files.write(
				logFile,
				text.getBytes(StandardCharsets.UTF_8),
				StandardOpenOption.CREATE,
				StandardOpenOption.APPEND
			);
		}
		catch (IOException ex)
		{
			log.warn("Dialogue Fonts+: failed writing diagnostic log {}", logFile, ex);
		}
	}
}
