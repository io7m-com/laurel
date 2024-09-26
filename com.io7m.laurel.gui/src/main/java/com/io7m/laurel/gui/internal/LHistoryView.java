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
import com.io7m.laurel.model.LCommandRecord;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;

import java.util.Optional;

import static com.io7m.laurel.gui.internal.LStringConstants.REDO;
import static com.io7m.laurel.gui.internal.LStringConstants.REDO_SPECIFIC;
import static com.io7m.laurel.gui.internal.LStringConstants.UNDO;
import static com.io7m.laurel.gui.internal.LStringConstants.UNDO_SPECIFIC;

/**
 * The history view.
 */

public final class LHistoryView extends LAbstractViewWithModel
{
  private final LStrings strings;

  @FXML private TableView<LCommandRecord> undoList;
  @FXML private TableView<LCommandRecord> redoList;
  @FXML private TableColumn<LCommandRecord, String> undoTimeColumn;
  @FXML private TableColumn<LCommandRecord, String> redoTimeColumn;
  @FXML private TableColumn<LCommandRecord, String> undoValueColumn;
  @FXML private TableColumn<LCommandRecord, String> redoValueColumn;
  @FXML private Button undo;
  @FXML private Button redo;
  @FXML private Tooltip undoTooltip;
  @FXML private Tooltip redoTooltip;

  LHistoryView(
    final RPServiceDirectoryType services,
    final LFileModelScope fileModel)
  {
    super(fileModel);

    this.strings =
      services.requireService(LStrings.class);
  }

  @Override
  protected void onInitialize()
  {
    this.initializeUndoTable();
    this.initializeRedoTable();
  }

  private void initializeRedoTable()
  {
    this.undoList.setEditable(false);

    this.undoTimeColumn.setSortable(false);
    this.undoTimeColumn.setReorderable(false);
    this.undoTimeColumn.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(param.getValue().time().toString());
    });

    this.undoValueColumn.setSortable(false);
    this.undoValueColumn.setReorderable(false);
    this.undoValueColumn.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(param.getValue().description());
    });

    this.undoList.setPlaceholder(new Label(""));
  }

  private void initializeUndoTable()
  {
    this.redoList.setEditable(false);

    this.redoTimeColumn.setSortable(false);
    this.redoTimeColumn.setReorderable(false);
    this.redoTimeColumn.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(param.getValue().time().toString());
    });

    this.redoValueColumn.setSortable(false);
    this.redoValueColumn.setReorderable(false);
    this.redoValueColumn.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(param.getValue().description());
    });

    this.redoList.setPlaceholder(new Label(""));
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
      model.undoText()
        .subscribe((oldValue, newValue) -> {
          Platform.runLater(() -> {
            this.onUndoStateChanged(newValue);
          });
        })
    );

    subscriptions.add(
      model.redoText()
        .subscribe((oldValue, newValue) -> {
          Platform.runLater(() -> {
            this.onRedoStateChanged(newValue);
          });
        })
    );

    subscriptions.add(
      model.undoStack()
        .subscribe((oldValue, newValue) -> {
          Platform.runLater(() -> {
            this.undoList.setItems(FXCollections.observableList(newValue));
          });
        })
    );

    subscriptions.add(
      model.redoStack()
        .subscribe((oldValue, newValue) -> {
          Platform.runLater(() -> {
            this.redoList.setItems(FXCollections.observableList(newValue));
          });
        })
    );
  }

  private void onUndoStateChanged(
    final Optional<String> opt)
  {
    if (opt.isPresent()) {
      this.undoEnable(opt.get());
    } else {
      this.undoDisable();
    }
  }

  private void undoDisable()
  {
    this.undo.setDisable(true);
    this.undoTooltip.setText(this.strings.format(UNDO));
  }

  private void undoEnable(
    final String text)
  {
    this.undo.setDisable(false);
    this.undoTooltip.setText(this.strings.format(UNDO_SPECIFIC, text));
  }

  private void onRedoStateChanged(
    final Optional<String> opt)
  {
    if (opt.isPresent()) {
      this.redoEnable(opt.get());
    } else {
      this.redoDisable();
    }
  }

  private void redoDisable()
  {
    this.redo.setDisable(true);
    this.redoTooltip.setText(this.strings.format(REDO));
  }

  private void redoEnable(
    final String text)
  {
    this.redo.setDisable(false);
    this.redoTooltip.setText(this.strings.format(REDO_SPECIFIC, text));
  }

  @FXML
  private void onUndoSelected()
  {
    this.fileModelNow().undo();
  }

  @FXML
  private void onRedoSelected()
  {
    this.fileModelNow().redo();
  }

  @FXML
  private void onCompactSelected()
  {
    final var alert =
      new Alert(
        Alert.AlertType.CONFIRMATION,
        this.strings.format(LStringConstants.HISTORY_COMPACT_CONFIRM),
        ButtonType.CANCEL,
        ButtonType.APPLY
      );

    LCSS.setCSS(alert.getDialogPane());

    final var r = alert.showAndWait();
    if (r.equals(Optional.of(ButtonType.APPLY))) {
      this.fileModelNow().compact();
    }
  }
}
