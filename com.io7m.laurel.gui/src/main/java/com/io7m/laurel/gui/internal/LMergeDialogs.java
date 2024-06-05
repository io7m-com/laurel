/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import com.io7m.repetoir.core.RPServiceDirectoryType;
import com.io7m.repetoir.core.RPServiceType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

import static javafx.stage.Modality.APPLICATION_MODAL;

/**
 * A service for creating merge dialogs.
 */

public final class LMergeDialogs implements RPServiceType
{
  private final LStrings strings;
  private final RPServiceDirectoryType services;

  /**
   * A service for creating merge dialogs.
   *
   * @param inServices The services
   * @param inStrings  The string resources
   */

  public LMergeDialogs(
    final RPServiceDirectoryType inServices,
    final LStrings inStrings)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.strings =
      Objects.requireNonNull(inStrings, "inStrings");
  }

  /**
   * Open a dialog.
   *
   * @return The dialog
   */

  public LMergeView open()
  {
    try {
      final var stage = new Stage();

      final var layout =
        LMergeDialogs.class.getResource(
          "/com/io7m/laurel/gui/internal/merge.fxml");

      Objects.requireNonNull(layout, "layout");

      final var loader =
        new FXMLLoader(layout, this.strings.resources());

      final var merger = new LMergeView(this.services, stage);
      loader.setControllerFactory(param -> {
        return merger;
      });

      final Pane pane = loader.load();
      LCSS.setCSS(pane);

      stage.initModality(APPLICATION_MODAL);
      stage.setTitle(this.strings.format("mergeTitle"));
      stage.setWidth(640.0);
      stage.setHeight(480.0);
      stage.setMinWidth(640.0);
      stage.setMinHeight(480.0);
      stage.setScene(new Scene(pane));
      stage.showAndWait();

      return merger;
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public String toString()
  {
    return String.format(
      "[LMergeDialogs 0x%08x]",
      Integer.valueOf(this.hashCode())
    );
  }

  @Override
  public String description()
  {
    return "Merge dialog service";
  }
}
