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
import com.io7m.laurel.gui.internal.errors.LErrorDialogs;
import com.io7m.repetoir.core.RPServiceDirectory;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
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

    final var errors = new LErrorDialogs(strings);
    services.register(LErrorDialogs.class, errors);

    final var captionEditors = new LCaptionEditors(strings);
    services.register(LCaptionEditors.class, captionEditors);

    final var exporters = new LExporterDialogs(services, strings);
    services.register(LExporterDialogs.class, exporters);

    final var mergers = new LMergeDialogs(services, strings);
    services.register(LMergeDialogs.class, mergers);

    final var choosers = new LFileChoosers();
    services.register(LFileChoosers.class, choosers);

    final var controller = new LController();
    services.register(LControllerType.class, controller);

    final var views =
      new LScreenViewFactory(
        services,
        attributes,
        this.configuration.directories(),
        stage
      );
    services.register(LScreenViewFactory.class, views);

    final var mainLoader = new FXMLLoader(mainXML, strings.resources());
    mainLoader.setControllerFactory(views);

    final Pane pane = mainLoader.load();
    stage.setTitle(strings.format("title"));
    stage.setMinWidth(640.0);
    stage.setMinHeight(480.0);
    stage.setWidth(1280.0);
    stage.setHeight(720.0);
    stage.setScene(new Scene(pane));
    stage.show();
  }
}
