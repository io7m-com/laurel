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


package com.io7m.laurel.gui.internal;

import com.io7m.laurel.gui.LConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedDeque;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * The preferences service.
 */

public final class LPreferences implements LPreferencesType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LPreferences.class);

  private static final OpenOption[] OPEN_OPTIONS =
    {WRITE, CREATE, TRUNCATE_EXISTING};
  private static final int MAXIMUM_RECENT_FILES =
    50;

  private final LConfiguration configuration;
  private final ConcurrentLinkedDeque<Path> recentFiles;
  private final Path preferences;
  private final Path preferencesTmp;

  private LPreferences(
    final LConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.recentFiles =
      new ConcurrentLinkedDeque<>();

    this.preferences =
      this.configuration.directories()
        .configurationDirectory()
        .resolve("Preferences.xml");

    this.preferencesTmp =
      this.configuration.directories()
        .configurationDirectory()
        .resolve("Preferences.xml.tmp");
  }

  /**
   * The preferences service.
   *
   * @param inConfiguration The configuration
   *
   * @return The service
   */

  public static LPreferencesType open(
    final LConfiguration inConfiguration)
  {
    final var preferences = new LPreferences(inConfiguration);
    LOG.info("Preferences: {}", preferences.preferences);

    Thread.ofVirtual().start(preferences::load);
    return preferences;
  }

  private void load()
  {
    final var properties = new Properties();

    try {
      try (var stream = Files.newInputStream(this.preferences)) {
        properties.loadFromXML(stream);
      }
    } catch (final IOException e) {
      // Don't care
    }

    for (int index = 0; index < MAXIMUM_RECENT_FILES; ++index) {
      final var file =
        properties.getProperty(
          "recentFiles.%d".formatted(Integer.valueOf(index))
        );
      if (file != null) {
        this.addRecentFileInner(Paths.get(file));
      }
    }
  }

  @Override
  public void addRecentFile(
    final Path file)
  {
    this.addRecentFileInner(file);
    Thread.ofVirtual().start(this::save);
  }

  private void addRecentFileInner(
    final Path file)
  {
    final var normalized =
      file.toAbsolutePath()
        .normalize();

    this.recentFiles.remove(normalized);
    this.recentFiles.push(normalized);

    if (this.recentFiles.size() > MAXIMUM_RECENT_FILES) {
      this.recentFiles.removeLast();
    }
  }

  private void save()
  {
    try {
      Files.createDirectories(this.preferences.getParent());

      final var properties = new Properties();
      int index = 0;
      for (final var recentFile : this.recentFiles) {
        properties.setProperty(
          "recentFiles.%d".formatted(Integer.valueOf(index)),
          recentFile.toString()
        );
        ++index;
      }

      try (var output =
             Files.newOutputStream(this.preferencesTmp, OPEN_OPTIONS)) {
        properties.storeToXML(output, "");
      }

      Files.move(
        this.preferencesTmp,
        this.preferences,
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.REPLACE_EXISTING
      );
    } catch (final IOException e) {
      // Don't care.
    }
  }

  @Override
  public List<Path> recentFiles()
  {
    return List.copyOf(this.recentFiles);
  }

  @Override
  public String description()
  {
    return "Preferences service.";
  }
}
