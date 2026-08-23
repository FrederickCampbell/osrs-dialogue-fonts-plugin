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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PluginHubComplianceTest
{
	private static final Path MAIN_SOURCE =
		Paths.get("src", "main", "java");

	private static final List<String> FORBIDDEN = Arrays.asList(
		"client.menuAction(",
		"Thread.sleep(",
		"java.awt.Robot",
		"new Robot(",
		"java.awt.Desktop",
		"java.lang.reflect",
		".setAccessible(",
		"sun.misc.Unsafe",
		"jdk.internal.misc.Unsafe",
		"com.sun.jna",
		"ProcessBuilder",
		"Runtime.getRuntime().exec",
		"System.load(",
		"System.loadLibrary(",
		"Class.forName(",
		"URLClassLoader",
		"HttpURLConnection",
		"java.net.http",
		"ObjectInputStream",
		"ObjectOutputStream",
		"java.nio.file.Files"
	);

	@Test
	public void productionSourceAvoidsReviewSensitiveApis()
		throws IOException
	{
		List<String> violations = new ArrayList<>();

		try (Stream<Path> paths = Files.walk(MAIN_SOURCE))
		{
			paths.filter(
				path -> path.toString().endsWith(".java")
			).forEach(path -> scan(path, violations));
		}

		assertTrue(
			"Plugin Hub compliance violations: " + violations,
			violations.isEmpty()
		);
	}

	@Test
	public void standardBuildMetadataIsPresent()
		throws IOException
	{
		String properties = new String(
			Files.readAllBytes(
				Paths.get("runelite-plugin.properties")
			),
			StandardCharsets.UTF_8
		);

		String build = new String(
			Files.readAllBytes(Paths.get("build.gradle")),
			StandardCharsets.UTF_8
		);

		assertTrue(properties.contains("build=standard"));
		assertTrue(build.contains("options.release.set(11)"));
		assertFalse(
			Files.exists(
				Paths.get(
					"src", "main", "resources", "META-INF", "services",
					"net.runelite.client.plugins.Plugin"
				)
			)
		);
		assertFalse(
			Files.exists(
				Paths.get(
					"src", "main", "java", "com", "betterdialogue",
					"DialogueDiagnostics.java"
				)
			)
		);
	}

	private static void scan(
		Path path,
		List<String> violations)
	{
		try
		{
			String text = new String(
				Files.readAllBytes(path),
				StandardCharsets.UTF_8
			);

			for (String needle : FORBIDDEN)
			{
				if (text.contains(needle))
				{
					violations.add(
						path + " -> " + needle
					);
				}
			}
		}
		catch (IOException ex)
		{
			throw new RuntimeException(ex);
		}
	}
}
