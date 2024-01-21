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
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.io7m.laurel.model.LImageCaption.VALID_CAPTION;

/**
 * The caption editor.
 */

public final class LCaptionEdit implements LScreenViewType
{
  private final String startingCaption;

  @FXML private Label error;
  @FXML private TextField textArea;
  @FXML private Button cancel;
  @FXML private Button save;
  @FXML private Parent errorContainer;

  private final Stage stage;
  private Optional<String> result;

  /**
   * The caption editor.
   *
   * @param inStage The stage
   * @param caption The starting caption
   */

  public LCaptionEdit(
    final Stage inStage,
    final String caption)
  {
    this.stage =
      Objects.requireNonNull(inStage, "stage");
    this.result =
      Optional.empty();
    this.startingCaption =
      Objects.requireNonNull(caption, "caption");
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.errorContainer.setVisible(false);
    this.textArea.setText(this.startingCaption);
    this.error.setText("");
    this.save.setDisable(true);
  }

  /**
   * Set the text to be edited.
   *
   * @param text The text
   */

  public void setText(final String text)
  {
    this.textArea.setText(text);
  }

  /**
   * @return The caption result.
   */

  public Optional<String> result()
  {
    return this.result;
  }

  @FXML
  private void onTextChanged()
  {
    this.save.setDisable(true);

    if (VALID_CAPTION.matcher(this.textArea.getText()).matches()) {
      this.save.setDisable(false);
      this.error.setText("");
      this.errorContainer.setVisible(false);
    } else {
      this.error.setText("Caption must match %s".formatted(VALID_CAPTION));
      this.errorContainer.setVisible(true);
    }
  }

  @FXML
  private void onCaptionCancelled()
  {
    this.stage.close();
  }

  @FXML
  private void onCaptionSave()
  {
    this.result = Optional.of(this.textArea.getText());
    this.stage.close();
  }
}
