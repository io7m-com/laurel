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
import com.io7m.laurel.model.LCaptionName;
import com.io7m.laurel.model.LGlobalCaption;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * The global prefix captions editor.
 */

public final class LGlobalPrefixCaptions
  extends LAbstractViewWithModel
{
  private final Stage stage;
  private final LCaptionEditors editors;

  @FXML private Button create;
  @FXML private Button modify;
  @FXML private Button delete;
  @FXML private Button up;
  @FXML private Button down;
  @FXML private ListView<LGlobalCaption> captions;

  /**
   * The global prefix captions editor.
   *
   * @param services    The services
   * @param inFileModel The file model
   * @param inStage     The stage
   */

  LGlobalPrefixCaptions(
    final RPServiceDirectoryType services,
    final LFileModelScope inFileModel,
    final Stage inStage)
  {
    super(inFileModel);

    this.editors =
      services.requireService(LCaptionEditors.class);
    this.stage =
      Objects.requireNonNull(inStage, "inStage");
  }

  @Override
  protected void onInitialize()
  {
    this.delete.setDisable(true);
    this.down.setDisable(true);
    this.modify.setDisable(true);
    this.up.setDisable(true);

    this.captions.setCellFactory(v -> new LCaptionListCell());
    this.captions.getSelectionModel()
      .setSelectionMode(SelectionMode.SINGLE);

    this.captions.getSelectionModel()
      .selectedItemProperty()
      .addListener((o, oldCap, newCap) -> {
        this.onCaptionSelectionChanged(newCap);
      });
  }

  @Override
  protected void onFileBecameUnavailable()
  {
    this.captions.setItems(FXCollections.emptyObservableList());
  }

  @Override
  protected void onFileBecameAvailable(
    final CloseableCollectionType<?> subscriptions,
    final LFileModelType fileModel)
  {
    subscriptions.add(
      fileModel.globalCaptionList()
        .subscribe((oldValue, newValue) -> {
          Platform.runLater(() -> {
            this.captions.setItems(FXCollections.observableList(newValue));
          });
        })
    );
  }

  private void onCaptionSelectionChanged(
    final LGlobalCaption caption)
  {
    if (caption != null) {
      this.delete.setDisable(false);
      this.down.setDisable(false);
      this.modify.setDisable(false);
      this.up.setDisable(false);
    } else {
      this.delete.setDisable(true);
      this.down.setDisable(true);
      this.modify.setDisable(true);
      this.up.setDisable(true);
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
      this.fileModelNow().globalCaptionAdd(new LCaptionName(text));
    }
  }

  @FXML
  private void onDeleteCaptionSelected()
  {
    this.fileModelNow()
      .globalCaptionRemove(
        this.captions.getSelectionModel()
          .getSelectedItem()
          .caption()
          .id()
      );
  }

  @FXML
  private void onModifyCaptionSelected()
  {
    final var selected =
      this.captions.getSelectionModel()
        .getSelectedItem();

    final var editor =
      this.editors.open(selected.caption().name().text());

    final var result = editor.result();
    if (result.isPresent()) {
      final var text = result.get();
      this.fileModelNow()
        .globalCaptionModify(
          selected.caption().id(),
          new LCaptionName(text)
        );
    }
  }

  @FXML
  private void onCaptionUpSelected()
  {
    this.fileModelNow()
      .globalCaptionOrderLower(
        this.captions.getSelectionModel()
          .getSelectedItem()
          .caption()
          .id()
      );
  }

  @FXML
  private void onCaptionDownSelected()
  {
    this.fileModelNow()
      .globalCaptionOrderUpper(
        this.captions.getSelectionModel()
          .getSelectedItem()
          .caption()
          .id()
      );
  }

  private static final class LCaptionListCell
    extends ListCell<LGlobalCaption>
  {
    LCaptionListCell()
    {

    }

    @Override
    protected void updateItem(
      final LGlobalCaption caption,
      final boolean isEmpty)
    {
      super.updateItem(caption, isEmpty);
      this.setGraphic(null);
      this.setText(null);
      if (caption != null) {
        this.setText(caption.caption().name().text());
      }
    }
  }
}
