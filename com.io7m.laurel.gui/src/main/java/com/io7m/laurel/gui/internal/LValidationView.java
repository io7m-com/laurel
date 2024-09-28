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

import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.laurel.filemodel.LFileModelType;
import com.io7m.laurel.filemodel.LValidationProblemType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.ListView;

import java.util.Objects;

/**
 * The validation view.
 */

public final class LValidationView extends LAbstractViewWithModel
{
  private final RPServiceDirectoryType services;
  private boolean hasValidatedEver;

  @FXML private ListView<LValidationProblemType> problemList;
  @FXML private Parent success;

  /**
   * The validation view.
   *
   * @param inServices  The services
   * @param inFileModel The file model scope
   */

  public LValidationView(
    final RPServiceDirectoryType inServices,
    final LFileModelScope inFileModel)
  {
    super(inFileModel);

    this.services =
      Objects.requireNonNull(inServices, "services");

    this.hasValidatedEver =
      false;
  }

  @Override
  protected void onInitialize()
  {
    this.success.setVisible(false);

    this.problemList.setCellFactory(v -> {
      return new LValidationCell(this.services);
    });
  }

  @Override
  protected void onFileBecameUnavailable()
  {

  }

  @Override
  protected void onFileBecameAvailable(
    final CloseableCollectionType<?> subscriptions,
    final LFileModelType model)
  {
    subscriptions.add(
      model.validationProblems()
        .subscribe((_0, newValue) -> {
          Platform.runLater(() -> {

            /*
             * An empty list will be delivered upon subscribing to the
             * validation list. We avoid displaying a "success" message unless
             * the user has actually explicitly pressed the validation
             * button.
             */

            if (this.hasValidatedEver) {
              this.problemList.setItems(FXCollections.observableList(newValue));

              if (newValue.isEmpty()) {
                this.success.setVisible(true);
              }
            }
          });
        })
    );
  }

  @FXML
  private void onValidationRunSelected()
  {
    this.hasValidatedEver = true;
    this.success.setVisible(false);
    this.fileModelNow().validate();
  }
}
