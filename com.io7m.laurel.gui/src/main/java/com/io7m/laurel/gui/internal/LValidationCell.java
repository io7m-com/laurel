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

import com.io7m.laurel.filemodel.LValidationProblemType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;

import java.io.IOException;

/**
 * A validation cell.
 */

public final class LValidationCell
  extends ListCell<LValidationProblemType>
{
  private final Parent root;
  private final LValidationCellController controller;

  /**
   * A validation cell.
   *
   * @param services The service directory
   */

  public LValidationCell(
    final RPServiceDirectoryType services)
  {
    try {
      final FXMLLoader loader =
        new FXMLLoader(
          LValidationCell.class.getResource(
            "/com/io7m/laurel/gui/internal/validationCell.fxml")
        );

      final var strings =
        services.requireService(LStrings.class);

      loader.setResources(strings.resources());
      loader.setControllerFactory(param -> {
        return new LValidationCellController();
      });
      this.root = loader.load();
      this.controller = loader.getController();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  protected void updateItem(
    final LValidationProblemType item,
    final boolean empty)
  {
    super.updateItem(item, empty);

    this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

    if (empty || item == null) {
      this.setGraphic(null);
      this.setText(null);
      this.controller.unsetItem();
      return;
    }

    this.controller.setItem(item);
    this.setGraphic(this.root);
    this.setText(null);
  }
}
