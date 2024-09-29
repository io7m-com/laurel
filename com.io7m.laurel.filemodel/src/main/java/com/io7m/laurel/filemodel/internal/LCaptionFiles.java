/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.laurel.filemodel.internal;

import com.io7m.laurel.model.LCaption;
import com.io7m.laurel.model.LCaptionName;
import com.io7m.laurel.model.LGlobalCaption;

import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Functions to parse and serialize caption files.
 */

public final class LCaptionFiles
{
  private static final OpenOption[] OPEN_OPTIONS = {
    StandardOpenOption.CREATE,
    StandardOpenOption.WRITE,
    StandardOpenOption.TRUNCATE_EXISTING,
  };

  private LCaptionFiles()
  {

  }

  /**
   * Parse a caption file.
   *
   * @param attributes The error attributes
   * @param file       The file
   *
   * @return The parsed caption names
   *
   * @throws Exception On errors
   */

  public static List<LCaptionName> parse(
    final Map<String, Object> attributes,
    final Path file)
    throws Exception
  {
    attributes.put("File", file);
    return parseCaptions(attributes, Files.readString(file));
  }

  private static List<LCaptionName> parseCaptions(
    final Map<String, Object> attributes,
    final String rawText)
  {
    final var results =
      new ArrayList<LCaptionName>();

    final var segments =
      List.of(rawText.split(","));

    for (final var text : segments) {
      final var trimmed = text.trim();
      if (trimmed.isBlank()) {
        continue;
      }
      attributes.put("Caption", trimmed);
      results.add(new LCaptionName(trimmed));
    }

    return List.copyOf(results);
  }

  /**
   * Serialize captions.
   *
   * @param attributes     The error attributes
   * @param globalCaptions The global captions
   * @param captions       The captions
   * @param outputFile     The output file
   *
   * @throws Exception On errors
   */

  public static void serialize(
    final Map<String, Object> attributes,
    final List<LGlobalCaption> globalCaptions,
    final List<LCaption> captions,
    final Path outputFile)
    throws Exception
  {
    attributes.put("File", outputFile);

    final var rawLines =
      new ArrayList<String>(globalCaptions.size() + captions.size());
    globalCaptions.forEach(x -> rawLines.add(x.caption().name().text()));
    captions.forEach(x -> rawLines.add(x.name().text()));

    Files.writeString(
      outputFile,
      String.join(",\n", rawLines),
      UTF_8,
      OPEN_OPTIONS
    );
  }
}
