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

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Resolves Dialogue Fonts+ font names against:
 *  1) .ttf/.otf files in ~/.runelite/fonts
 *  2) installed/system Java AWT font families
 *
 * Custom files are rescanned automatically every few seconds and do not require
 * a rebuild or RuneLite restart.
 */
@Slf4j
@Singleton
public class FontResolver
{
	private static final long RESCAN_INTERVAL_NANOS = 3_000_000_000L;

	private final Path fontDirectory =
		Paths.get(System.getProperty("user.home"), ".runelite", "fonts");

	private volatile Map<String, Font> customFonts = Collections.emptyMap();
	private volatile Map<String, String> systemFamilies = Collections.emptyMap();
	private volatile String lastFingerprint = "";
	private volatile long generation = 0;
	private volatile long nextScanNanos = 0;

	@Inject
	FontResolver()
	{
	}

	public synchronized void refreshNow()
	{
		reloadSystemFamilies();
		reloadCustomFonts(true);
		nextScanNanos = System.nanoTime() + RESCAN_INTERVAL_NANOS;
	}

	public long getGeneration()
	{
		maybeRefresh();
		return generation;
	}

	public Path getFontDirectory()
	{
		return fontDirectory;
	}

	public Font resolve(String requestedName, int style, int size)
	{
		maybeRefresh();

		String requested = requestedName == null ? "" : requestedName.trim();
		if (requested.isEmpty())
		{
			requested = "SansSerif";
		}

		String key = normalize(requested);
		Font custom = customFonts.get(key);
		if (custom != null)
		{
			return custom.deriveFont(style, (float) size);
		}

		String family = systemFamilies.get(key);
		if (family != null)
		{
			return new Font(family, style, size);
		}

		// Java logical fonts remain valid even if they are not listed exactly
		// like ordinary installed families.
		if (isLogicalFont(requested))
		{
			return new Font(requested, style, size);
		}

		log.warn(
			"Dialogue Fonts+: font '{}' was not found. Use an installed font family or place a .ttf/.otf in {}. Falling back to SansSerif.",
			requested, fontDirectory);
		return new Font("SansSerif", style, size);
	}

	private void maybeRefresh()
	{
		long now = System.nanoTime();
		if (now < nextScanNanos)
		{
			return;
		}

		synchronized (this)
		{
			now = System.nanoTime();
			if (now < nextScanNanos)
			{
				return;
			}
			if (systemFamilies.isEmpty())
			{
				reloadSystemFamilies();
			}
			reloadCustomFonts(false);
			nextScanNanos = now + RESCAN_INTERVAL_NANOS;
		}
	}

	private void reloadSystemFamilies()
	{
		Map<String, String> families = new HashMap<>();
		for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
		{
			families.put(normalize(family), family);
		}

		// Java logical names, guaranteed to resolve cross-platform.
		for (String logical : new String[]{"SansSerif", "Serif", "Monospaced", "Dialog", "DialogInput"})
		{
			families.put(normalize(logical), logical);
		}

		systemFamilies = Collections.unmodifiableMap(families);
		generation++;
		log.debug("Dialogue Fonts+: indexed {} system/logical font families", families.size());
	}

	private void reloadCustomFonts(boolean force)
	{
		try
		{
			Files.createDirectories(fontDirectory);
			String fingerprint = buildFingerprint();
			if (!force && fingerprint.equals(lastFingerprint))
			{
				return;
			}

			Map<String, Font> loaded = new HashMap<>();
			try (Stream<Path> files = Files.list(fontDirectory))
			{
				for (Path file : files
					.filter(Files::isRegularFile)
					.filter(FontResolver::isSupportedFontFile)
					.sorted()
					.collect(Collectors.toList()))
				{
					try
					{
						Font font = Font.createFont(Font.TRUETYPE_FONT, file.toFile());
						GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);

						String filename = file.getFileName().toString();
						String stem = stripExtension(filename);

						// Filename is the most deterministic selector.
						loaded.put(normalize(filename), font);
						loaded.put(normalize(stem), font);

						// Family/full font names make the config nicer to use.
						loaded.put(normalize(font.getFamily()), font);
						loaded.put(normalize(font.getFontName()), font);

						log.info("Dialogue Fonts+: loaded custom font {} -> {}",
							filename, font.getFontName());
					}
					catch (Exception ex)
					{
						log.warn("Dialogue Fonts+: could not load custom font {}", file, ex);
					}
				}
			}

			customFonts = Collections.unmodifiableMap(loaded);
			lastFingerprint = fingerprint;
			generation++;
			log.info("Dialogue Fonts+: custom font scan complete ({} selectors) from {}",
				loaded.size(), fontDirectory);
		}
		catch (IOException ex)
		{
			log.warn("Dialogue Fonts+: failed to scan custom font directory {}", fontDirectory, ex);
		}
	}

	private String buildFingerprint() throws IOException
	{
		try (Stream<Path> files = Files.list(fontDirectory))
		{
			return files
				.filter(Files::isRegularFile)
				.filter(FontResolver::isSupportedFontFile)
				.sorted()
				.map(path ->
				{
					try
					{
						return path.getFileName() + "|" + Files.size(path) + "|" +
							Files.getLastModifiedTime(path).toMillis();
					}
					catch (IOException e)
					{
						return path.getFileName().toString();
					}
				})
				.collect(Collectors.joining(";"));
		}
	}

	private static boolean isSupportedFontFile(Path path)
	{
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		return name.endsWith(".ttf") || name.endsWith(".otf");
	}

	private static String stripExtension(String name)
	{
		int dot = name.lastIndexOf('.');
		return dot > 0 ? name.substring(0, dot) : name;
	}

	private static String normalize(String value)
	{
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private static boolean isLogicalFont(String name)
	{
		String key = normalize(name);
		return key.equals("sansserif") ||
			key.equals("serif") ||
			key.equals("monospaced") ||
			key.equals("dialog") ||
			key.equals("dialoginput");
	}
}
