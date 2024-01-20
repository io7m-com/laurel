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

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javafx.stage.Modality.APPLICATION_MODAL;

/**
 * The About screen.
 */

public final class LAbout implements LScreenViewType
{
  private final Stage stage;

  private @FXML Label version;

  /**
   * The About screen.
   *
   * @param inStage The stage
   */

  public LAbout(
    final Stage inStage)
  {
    this.stage =
      Objects.requireNonNull(inStage, "inStage");
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    try (var stream = LAbout.class.getResourceAsStream(
      "/com/io7m/laurel/gui/internal/version.txt")) {
      this.version.setText(new String(stream.readAllBytes(), UTF_8));
    } catch (final Exception e) {
      this.version.setText("");
    }
  }

  @FXML
  private void onDismiss()
  {
    this.stage.close();
  }

  /**
   * The About screen.
   *
   * @param strings The strings
   *
   * @return The about screen
   */

  public static LAbout open(
    final LStrings strings)
  {
    try {
      final var stage = new Stage();

      final var layout =
        LExporterDialogs.class.getResource(
          "/com/io7m/laurel/gui/internal/about.fxml");

      Objects.requireNonNull(layout, "layout");

      final var loader =
        new FXMLLoader(layout, strings.resources());

      final var about = new LAbout(stage);
      loader.setControllerFactory(param -> {
        return about;
      });

      final Pane pane = loader.load();
      LCSS.setCSS(pane);

      final var width = 650.0;
      final var height = 432.0;
      stage.initModality(APPLICATION_MODAL);
      stage.setTitle(strings.format("about"));
      stage.setWidth(width);
      stage.setMaxWidth(width);
      stage.setMinWidth(width);
      stage.setMinHeight(height);
      stage.setHeight(height);
      stage.setMaxHeight(height);
      stage.setScene(new Scene(pane));
      stage.showAndWait();

      return about;
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
