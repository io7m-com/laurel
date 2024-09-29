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

import com.io7m.laurel.model.LMetadataValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * The metadata editor.
 */

public final class LMetadataEdit implements LViewType
{
  private final LMetadataValue startingMetadata;

  @FXML private TextField metaName;
  @FXML private TextArea metaValue;
  @FXML private Button cancel;
  @FXML private Button save;

  private final Stage stage;
  private Optional<LMetadataValue> result;

  /**
   * The metadata editor.
   *
   * @param inStage The stage
   * @param metadata The starting metadata
   */

  public LMetadataEdit(
    final Stage inStage,
    final LMetadataValue metadata)
  {
    this.stage =
      Objects.requireNonNull(inStage, "stage");
    this.result =
      Optional.empty();
    this.startingMetadata =
      Objects.requireNonNull(metadata, "metadata");
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.metaValue.textProperty()
      .addListener(observable -> {
        this.onTextChanged();
      });

    this.metaName.setText(this.startingMetadata.name());
    this.metaValue.setText(this.startingMetadata.value());
    this.save.setDisable(true);
  }

  /**
   * @return The caption result.
   */

  public Optional<LMetadataValue> result()
  {
    return this.result;
  }

  @FXML
  private void onTextChanged()
  {
    this.save.setDisable(true);

    if (this.metaName.getText().isBlank() || this.metaValue.getText().isBlank()) {
      this.save.setDisable(true);
      return;
    }

    this.save.setDisable(false);
  }

  @FXML
  private void onMetadataCancelled()
  {
    this.stage.close();
  }

  @FXML
  private void onMetadataSave()
  {
    this.result = Optional.of(
      new LMetadataValue(
        this.metaName.getText().trim(),
        this.metaValue.getText().trim()
      )
    );
    this.stage.close();
  }
}
