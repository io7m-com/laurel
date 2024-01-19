/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

package com.io7m.laurel.gui.main;

import com.io7m.jade.api.ApplicationDirectories;
import com.io7m.jade.api.ApplicationDirectoryConfiguration;
import com.io7m.laurel.gui.LConfiguration;
import com.io7m.laurel.gui.LGUI;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * The main GUI entrypoint.
 */

public final class LGUIMain
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LGUIMain.class);

  private LGUIMain()
  {

  }

  /**
   * The main GUI entrypoint.
   *
   * @param args Command-line arguments
   */

  public static void main(
    final String[] args)
  {
    final var directoryConfiguration =
      ApplicationDirectoryConfiguration.builder()
        .setApplicationName("com.io7m.laurel")
        .setPortablePropertyName("com.io7m.laurel.portable")
        .build();

    final var directories =
      ApplicationDirectories.get(directoryConfiguration);

    final var configuration =
      new LConfiguration(
        Locale.getDefault(),
        directories
      );

    Platform.startup(() -> {
      try {
        LGUI.start(configuration);
      } catch (final Exception e) {
        LOG.error("startup failed: ", e);
        System.exit(1);
      }
    });
  }
}
