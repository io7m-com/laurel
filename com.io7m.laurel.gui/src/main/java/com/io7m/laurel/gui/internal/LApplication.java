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


package com.io7m.laurel.gui.internal;

import com.io7m.jattribute.core.Attributes;
import com.io7m.laurel.gui.LConfiguration;
import com.io7m.repetoir.core.RPServiceDirectory;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * The main JavaFX application.
 */

public final class LApplication extends Application
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LApplication.class);

  private final LConfiguration configuration;

  /**
   * The main JavaFX application.
   *
   * @param inConfiguration The configuration
   */

  public LApplication(
    final LConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
  }

  @Override
  public void start(
    final Stage stage)
    throws Exception
  {
    final var mainXML =
      LApplication.class.getResource(
        "/com/io7m/laurel/gui/internal/main.fxml");
    Objects.requireNonNull(mainXML, "mainXML");

    final var attributes = Attributes.create(throwable -> {
      LOG.debug("Error assigning attribute value: ", throwable);
    });

    final var services = new RPServiceDirectory();
    final var strings = new LStrings(this.configuration.locale());
    services.register(LStrings.class, strings);

    final var preferences = LPreferences.open(this.configuration);
    services.register(LPreferencesType.class, preferences);

    final var comparisonViews = new LCaptionComparisonViews(strings);
    services.register(LCaptionComparisonViews.class, comparisonViews);

    final var captionEditors = new LCaptionEditors(strings);
    services.register(LCaptionEditors.class, captionEditors);

    final var categoryEditors = new LCategoryEditors(strings);
    services.register(LCategoryEditors.class, categoryEditors);

    final var metadataEditors = new LMetadataEditors(strings);
    services.register(LMetadataEditors.class, metadataEditors);

    final var choosers = new LFileChoosers(services);
    services.register(LFileChoosersType.class, choosers);

    final var viewAndStage =
      LFileView.openForStage(
        services,
        LFileModelScope.createEmptyScope(),
        stage
      );

    viewAndStage.stage().show();
  }
}
