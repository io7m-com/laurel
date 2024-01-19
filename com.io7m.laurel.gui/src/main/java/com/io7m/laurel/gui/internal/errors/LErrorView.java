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

package com.io7m.laurel.gui.internal.errors;

import com.io7m.laurel.gui.internal.LScreenViewType;
import com.io7m.seltzer.api.SStructuredErrorType;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * A controller for the error screen.
 */

public final class LErrorView
  implements LScreenViewType
{
  private final SStructuredErrorType<String> error;
  private final Stage stage;

  @FXML private TableColumn<Map.Entry<String, String>, String> errorNameColumn;
  @FXML private TableColumn<Map.Entry<String, String>, String> errorValueColumn;
  @FXML private VBox errorContainer;
  @FXML private ImageView errorIcon;
  @FXML private Label errorTaskTitle;
  @FXML private Label errorTaskMessage;
  @FXML private TableView<Map.Entry<String, String>> errorAttributes;

  /**
   * A controller for the error screen.
   *
   * @param inError         The error
   * @param inStage         The containing window
   */

  public LErrorView(
    final SStructuredErrorType<String> inError,
    final Stage inStage)
  {
    this.error =
      Objects.requireNonNull(inError, "error");
    this.stage =
      Objects.requireNonNull(inStage, "stage");
  }

  @Override
  public void initialize(
    final URL location,
    final ResourceBundle resources)
  {
    this.errorNameColumn.setCellValueFactory(
      param -> new ReadOnlyObjectWrapper<>(param.getValue().getKey()));
    this.errorValueColumn.setCellValueFactory(
      param -> new ReadOnlyObjectWrapper<>(param.getValue().getValue()));

    this.errorTaskTitle.setText(this.error.errorCode());
    this.errorTaskMessage.setText(this.error.message());

    final var attributes = this.error.attributes();
    if (attributes.isEmpty()) {
      this.errorContainer.getChildren().remove(this.errorAttributes);
    } else {
      this.errorAttributes.setItems(
        FXCollections.observableList(
          attributes.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .toList()
        )
      );
    }
  }

  @FXML
  private void onDismissSelected()
  {
    this.stage.close();
  }

  @FXML
  private void onReportSelected()
  {

  }
}
