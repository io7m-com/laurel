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
import com.io7m.laurel.model.LMetadataValue;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

/**
 * The metadata view.
 */

public final class LMetadataView extends LAbstractViewWithModel
{
  private final LMetadataEditors editors;

  @FXML private TableView<LMetadataValue> metadataList;
  @FXML private Button metadataRemove;
  @FXML private Button metadataAdd;

  LMetadataView(
    final RPServiceDirectoryType services,
    final LFileModelScope fileModel)
  {
    super(fileModel);

    this.editors =
      services.requireService(LMetadataEditors.class);
  }

  @FXML
  private void onMetadataAddSelected()
  {
    final var edit =
      this.editors.open(new LMetadataValue("", ""));
    final var result =
      edit.result();

    result.ifPresent(meta -> this.fileModelNow().metadataPut(List.of(meta)));
  }

  @FXML
  private void onMetadataRemoveSelected()
  {
    final var selected =
      this.metadataList.getSelectionModel()
        .getSelectedItems();

    if (!selected.isEmpty()) {
      this.fileModelNow().metadataRemove(selected);
    }
  }

  @Override
  protected void onInitialize()
  {
    this.metadataRemove.setDisable(true);
    this.initializeMetadataTable();
  }

  private void initializeMetadataTable()
  {
    this.metadataList.setEditable(false);

    final var columns =
      this.metadataList.getColumns();

    final var colName = (TableColumn<LMetadataValue, String>) columns.get(0);
    colName.setSortable(true);
    colName.setReorderable(false);
    colName.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(param.getValue().name());
    });

    final var colValue = (TableColumn<LMetadataValue, String>) columns.get(1);
    colValue.setSortable(true);
    colValue.setReorderable(false);
    colValue.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(param.getValue().value());
    });

    this.metadataList.getSelectionModel()
      .setSelectionMode(SelectionMode.MULTIPLE);
    this.metadataList.getSelectionModel()
      .selectedItemProperty()
      .subscribe(this::onMetadataSelectionChanged);

    this.metadataList.setPlaceholder(new Label(""));
    this.metadataList.getSortOrder().add(colName);
    this.metadataList.sort();
  }

  private void onMetadataSelectionChanged()
  {
    final var selected =
      this.metadataList.getSelectionModel()
        .getSelectedItems();

    this.metadataRemove.setDisable(selected.isEmpty());
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
      model.metadataList()
        .subscribe((oldValue, newValue) -> {
          Platform.runLater(() -> {
            this.metadataList.setItems(FXCollections.observableList(newValue));
          });
        })
    );
  }
}
