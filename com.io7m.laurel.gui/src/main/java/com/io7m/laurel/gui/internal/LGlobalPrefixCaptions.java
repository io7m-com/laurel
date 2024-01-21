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

import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * The global prefix captions editor.
 */

public final class LGlobalPrefixCaptions implements LScreenViewType
{
  private final Stage stage;
  private final LControllerType controller;
  private final LCaptionEditors editors;

  @FXML private Button create;
  @FXML private Button modify;
  @FXML private Button delete;
  @FXML private Button up;
  @FXML private Button down;
  @FXML private ListView<String> captions;

  /**
   * The global prefix captions editor.
   *
   * @param services The services
   * @param inStage  The stage
   */

  public LGlobalPrefixCaptions(
    final RPServiceDirectoryType services,
    final Stage inStage)
  {
    this.controller =
      services.requireService(LControllerType.class);
    this.editors =
      services.requireService(LCaptionEditors.class);
    this.stage =
      Objects.requireNonNull(inStage, "inStage");
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.delete.setDisable(true);
    this.down.setDisable(true);
    this.modify.setDisable(true);
    this.up.setDisable(true);

    this.captions.setItems(this.controller.globalPrefixCaptions());
    this.captions.getSelectionModel()
      .selectedItemProperty()
      .addListener((o, oldCap, newCap) -> {
        this.onCaptionSelectionChanged(newCap);
      });
  }

  private void onCaptionSelectionChanged(
    final String caption)
  {
    if (caption != null) {
      this.delete.setDisable(false);
      this.modify.setDisable(false);
    } else {
      this.delete.setDisable(true);
      this.modify.setDisable(true);
    }
  }

  @FXML
  private void onDismissSelected()
  {
    this.stage.close();
  }

  @FXML
  private void onCreateCaptionSelected()
  {
    final var editor =
      this.editors.open("");

    final var result = editor.result();
    if (result.isPresent()) {
      final var text = result.get();
      this.controller.globalPrefixCaptionNew(text);
    }
  }

  @FXML
  private void onDeleteCaptionSelected()
  {
    this.controller.globalPrefixCaptionDelete(
      this.captions.getSelectionModel()
        .getSelectedIndex()
    );
  }

  @FXML
  private void onModifyCaptionSelected()
  {
    final var editor =
      this.editors.open(
        this.captions.getSelectionModel()
          .getSelectedItem()
      );

    final var result = editor.result();
    if (result.isPresent()) {
      final var text = result.get();
      this.controller.globalPrefixCaptionModify(
        this.captions.getSelectionModel()
          .getSelectedIndex(),
        text
      );
    }
  }

  @FXML
  private void onCaptionUpSelected()
  {

  }

  @FXML
  private void onCaptionDownSelected()
  {

  }
}
