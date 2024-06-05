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

import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.jwheatsheaf.api.JWFileChooserAction;
import com.io7m.jwheatsheaf.api.JWFileChooserConfiguration;
import com.io7m.jwheatsheaf.oxygen.JWOxygenIconSet;
import com.io7m.laurel.gui.internal.errors.LErrorDialogs;
import com.io7m.laurel.gui.internal.model.LMUndoState;
import com.io7m.laurel.gui.internal.model.LModelFileStatusType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static com.io7m.laurel.gui.internal.LMainView.Exit.DO_NOT_EXIT;
import static com.io7m.laurel.gui.internal.LMainView.Exit.EXIT;
import static com.io7m.laurel.gui.internal.LStringConstants.REDO;
import static com.io7m.laurel.gui.internal.LStringConstants.REDO_SPECIFIC;
import static com.io7m.laurel.gui.internal.LStringConstants.UNDO;
import static com.io7m.laurel.gui.internal.LStringConstants.UNDO_SPECIFIC;
import static javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST;

/**
 * The main controller.
 */

public final class LMainView implements LScreenViewType
{
  private final LControllerType controller;
  private final LStrings strings;
  private final LFileChoosers choosers;
  private final LErrorDialogs errorDialogs;
  private final Stage stage;
  private final LExporterDialogs exporterDialogs;
  private final LPreferencesType preferences;
  private final LMergeDialogs mergeDialogs;

  private @FXML Parent root;
  private @FXML MenuItem menuItemNew;
  private @FXML MenuItem menuItemOpen;
  private @FXML MenuItem menuItemSave;
  private @FXML MenuItem menuItemSaveAs;
  private @FXML MenuItem menuItemClose;
  private @FXML MenuItem menuItemExport;
  private @FXML MenuItem menuItemImport;
  private @FXML MenuItem menuItemExit;
  private @FXML MenuItem menuItemUndo;
  private @FXML MenuItem menuItemRedo;
  private @FXML Label statusLabel;

  /**
   * The main controller.
   *
   * @param services The service directory
   * @param inStage  The stage
   */

  public LMainView(
    final RPServiceDirectoryType services,
    final Stage inStage)
  {
    this.controller =
      services.requireService(LControllerType.class);
    this.strings =
      services.requireService(LStrings.class);
    this.choosers =
      services.requireService(LFileChoosers.class);
    this.errorDialogs =
      services.requireService(LErrorDialogs.class);
    this.exporterDialogs =
      services.requireService(LExporterDialogs.class);
    this.mergeDialogs =
      services.requireService(LMergeDialogs.class);
    this.preferences =
      services.requireService(LPreferencesType.class);

    this.stage =
      Objects.requireNonNull(inStage, "stage");
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    LCSS.setCSS(this.root);

    this.controller.model()
      .fileStatus()
      .subscribe((oldValue, newValue) -> this.handleModelFileStatus(newValue));
    this.controller.busy()
      .subscribe((oldValue, newValue) -> this.handleBusy());

    this.controller.errors()
      .subscribe(new LPerpetualSubscriber<>(error -> {
        Platform.runLater(() -> this.errorDialogs.open(error));
      }));

    this.controller.undoState()
      .subscribe((oldValue, newValue) -> {
        this.handleUndoChanged(newValue);
      });

    this.stage.addEventFilter(WINDOW_CLOSE_REQUEST, event -> {
      switch (this.onHandleExitRequest()) {
        case EXIT -> {
        }
        case DO_NOT_EXIT -> event.consume();
      }
    });

    this.handleUndoChanged(this.controller.undoState().getValue());
  }

  private void handleUndoChanged(
    final LMUndoState undoState)
  {
    Platform.runLater(() -> {
      final var undoStack = undoState.undoStack();
      if (undoStack.isEmpty()) {
        this.menuItemUndo.setDisable(true);
        this.menuItemUndo.setText(this.strings.format(UNDO));
      } else {
        final var op = undoStack.getFirst();
        this.menuItemUndo.setDisable(false);
        this.menuItemUndo.setText(
          this.strings.format(UNDO_SPECIFIC, op.description()));
      }

      final var redoStack = undoState.redoStack();
      if (redoStack.isEmpty()) {
        this.menuItemRedo.setDisable(true);
        this.menuItemRedo.setText(this.strings.format(REDO));
      } else {
        final var op = redoStack.getFirst();
        this.menuItemRedo.setDisable(false);
        this.menuItemRedo.setText(
          this.strings.format(REDO_SPECIFIC, op.description()));
      }
    });
  }

  private void handleBusy()
  {
    Platform.runLater(() -> {
      this.root.setDisable(this.controller.isBusy());
    });
  }

  private void handleModelFileStatus(
    final LModelFileStatusType status)
  {
    Platform.runLater(() -> {
      switch (status) {
        case final LModelFileStatusType.None none -> {
          this.stage.setTitle(this.strings.format(LStringConstants.TITLE));
          this.menuItemSave.setDisable(true);
          this.menuItemSaveAs.setDisable(true);
          this.menuItemClose.setDisable(true);
          this.menuItemExport.setDisable(true);
        }

        case final LModelFileStatusType.Saved saved -> {
          this.stage.setTitle(
            this.strings.format(
              LStringConstants.TITLE_SAVED,
              saved.file().toAbsolutePath().toString()
            )
          );
          this.menuItemSave.setDisable(true);
          this.menuItemSaveAs.setDisable(false);
          this.menuItemClose.setDisable(false);
          this.menuItemExport.setDisable(false);
        }

        case final LModelFileStatusType.Unsaved unsaved -> {
          this.stage.setTitle(
            this.strings.format(
              LStringConstants.TITLE_UNSAVED,
              unsaved.file().toAbsolutePath().toString()
            )
          );
          this.menuItemSave.setDisable(false);
          this.menuItemSaveAs.setDisable(false);
          this.menuItemClose.setDisable(false);
          this.menuItemExport.setDisable(false);
        }
      }
    });
  }

  /**
   * The user tried to create a new image set.
   *
   * @throws Exception On errors
   */

  @FXML
  public void onNewSelected()
    throws Exception
  {
    if (this.controller.isSaved()) {
      this.tryNew();
      return;
    }

    switch (this.onConfirmUnsaved()) {
      case CANCEL -> {
        return;
      }
      case DISCARD -> {
        this.tryNew();
        return;
      }
      case SAVE -> {
        this.controller.save();
        this.tryNew();
        return;
      }
    }
  }

  private void tryNew()
    throws Exception
  {
    final var fileChooser =
      this.choosers.fileChoosers()
        .create(
          JWFileChooserConfiguration.builder()
            .setModality(Modality.APPLICATION_MODAL)
            .setAction(JWFileChooserAction.CREATE)
            .setRecentFiles(this.preferences.recentFiles())
            .setConfirmFileSelection(true)
            .setTitle(this.strings.format(LStringConstants.SELECT_NEW_FILE))
            .setCssStylesheet(LCSS.defaultCSS().toURL())
            .setFileImageSet(new JWOxygenIconSet())
            .build()
        );

    final var files = fileChooser.showAndWait();
    if (files.isEmpty()) {
      return;
    }

    for (final var file : files) {
      this.preferences.addRecentFile(file);
    }

    this.controller.newSet(files.get(0));
  }

  /**
   * The user tried to open an image set.
   *
   * @throws Exception On errors
   */

  @FXML
  public void onOpenSelected()
    throws Exception
  {
    if (this.controller.isSaved()) {
      this.tryOpen();
      return;
    }

    switch (this.onConfirmUnsaved()) {
      case CANCEL -> {
        return;
      }
      case DISCARD -> {
        this.tryOpen();
        return;
      }
      case SAVE -> {
        this.controller.save();
        this.tryOpen();
        return;
      }
    }
  }

  private boolean tryOpen()
    throws Exception
  {
    final var fileChooser =
      this.choosers.fileChoosers()
        .create(
          JWFileChooserConfiguration.builder()
            .setModality(Modality.APPLICATION_MODAL)
            .setAction(JWFileChooserAction.OPEN_EXISTING_SINGLE)
            .setRecentFiles(this.preferences.recentFiles())
            .setCssStylesheet(LCSS.defaultCSS().toURL())
            .setFileImageSet(new JWOxygenIconSet())
            .build()
        );

    final var files = fileChooser.showAndWait();
    if (files.isEmpty()) {
      return false;
    }

    for (final var file : files) {
      this.preferences.addRecentFile(file);
    }

    this.controller.open(files.get(0));
    return true;
  }

  enum ConfirmUnsaved
  {
    CANCEL,
    DISCARD,
    SAVE
  }

  private ConfirmUnsaved onConfirmUnsaved()
  {
    final var cancel =
      new ButtonType(this.strings.format(LStringConstants.CANCEL));
    final var discard =
      new ButtonType(this.strings.format(LStringConstants.DISCARD));
    final var save =
      new ButtonType(this.strings.format(LStringConstants.SAVE));

    final var alert =
      new Alert(
        Alert.AlertType.CONFIRMATION,
        this.strings.format(LStringConstants.CONFIRM_UNSAVED),
        cancel,
        discard,
        save
      );

    alert.getDialogPane()
      .getStylesheets()
      .add(LCSS.defaultCSS().toString());

    final var resultOpt = alert.showAndWait();
    if (resultOpt.isEmpty()) {
      return ConfirmUnsaved.CANCEL;
    }

    final var resultButton = resultOpt.get();
    if (resultButton.equals(cancel)) {
      return ConfirmUnsaved.CANCEL;
    }

    if (resultButton.equals(discard)) {
      return ConfirmUnsaved.DISCARD;
    }

    if (resultButton.equals(save)) {
      return ConfirmUnsaved.SAVE;
    }

    throw new UnreachableCodeException();
  }

  /**
   * The user tried to close the image set.
   */

  @FXML
  public void onCloseSelected()
  {
    if (this.controller.isSaved()) {
      this.controller.closeSet();
      return;
    }

    switch (this.onConfirmUnsaved()) {
      case CANCEL -> {
        return;
      }
      case DISCARD -> {
        this.controller.closeSet();
        return;
      }
      case SAVE -> {
        this.controller.save();
        this.controller.closeSet();
        return;
      }
    }
  }

  /**
   * The user tried to save an image set.
   */

  @FXML
  public void onSaveSelected()
  {
    this.controller.save();
  }

  /**
   * The user tried to save an image set.
   *
   * @throws Exception On errors
   */

  @FXML
  public void onSaveAsSelected()
    throws Exception
  {
    final var fileChooser =
      this.choosers.fileChoosers()
        .create(
          JWFileChooserConfiguration.builder()
            .setModality(Modality.APPLICATION_MODAL)
            .setAction(JWFileChooserAction.CREATE)
            .setRecentFiles(this.preferences.recentFiles())
            .setConfirmFileSelection(true)
            .setCssStylesheet(LCSS.defaultCSS().toURL())
            .setFileImageSet(new JWOxygenIconSet())
            .build()
        );

    final var files = fileChooser.showAndWait();
    if (files.isEmpty()) {
      return;
    }

    for (final var file : files) {
      this.preferences.addRecentFile(file);
    }

    this.controller.save(files.get(0));
  }

  /**
   * The user tried to exit.
   */

  @FXML
  public void onExitSelected()
  {
    switch (this.onHandleExitRequest()) {
      case EXIT -> Platform.exit();
      case DO_NOT_EXIT -> {
      }
    }
  }

  enum Exit
  {
    EXIT,
    DO_NOT_EXIT
  }

  private Exit onHandleExitRequest()
  {
    if (this.controller.isSaved()) {
      return EXIT;
    }

    return switch (this.onConfirmUnsaved()) {
      case CANCEL -> {
        yield DO_NOT_EXIT;
      }
      case DISCARD -> {
        yield EXIT;
      }
      case SAVE -> {
        this.controller.save();
        yield EXIT;
      }
    };
  }

  /**
   * The user tried to export.
   */

  @FXML
  public void onExportSelected()
  {
    final var exporter =
      this.exporterDialogs.open();
    final var result =
      exporter.result();

    if (result.isPresent()) {
      this.controller.export(result.get());
    }
  }

  /**
   * The user tried to import.
   *
   * @throws Exception On errors
   */

  @FXML
  public void onImportSelected()
    throws Exception
  {
    if (this.controller.isSaved()) {
      this.tryImport();
      return;
    }

    switch (this.onConfirmUnsaved()) {
      case CANCEL -> {
        return;
      }
      case DISCARD -> {
        this.tryImport();
        return;
      }
      case SAVE -> {
        this.controller.save();
        this.tryImport();
        return;
      }
    }
  }

  private void tryImport()
    throws MalformedURLException
  {
    final var fileChooser =
      this.choosers.fileChoosers()
        .create(
          JWFileChooserConfiguration.builder()
            .setModality(Modality.APPLICATION_MODAL)
            .setAction(JWFileChooserAction.OPEN_EXISTING_SINGLE)
            .setTitle(this.strings.format("import.select"))
            .setRecentFiles(this.preferences.recentFiles())
            .setCssStylesheet(LCSS.defaultCSS().toURL())
            .setFileImageSet(new JWOxygenIconSet())
            .build()
        );

    final var files = fileChooser.showAndWait();
    if (files.isEmpty()) {
      return;
    }

    for (final var file : files) {
      this.preferences.addRecentFile(file);
    }

    this.controller.importDirectory(files.get(0));
  }

  /**
   * The user tried to undo an operation.
   */

  @FXML
  public void onUndo()
  {
    this.controller.undo();
  }

  /**
   * The user tried to redo an operation.
   */

  @FXML
  public void onRedo()
  {
    this.controller.redo();
  }

  /**
   * The user tried to open the about screen.
   */

  @FXML
  public void onAboutSelected()
  {
    LAbout.open(this.strings);
  }

  /**
   * The user tried to merge.
   */

  @FXML
  public void onMergeSelected()
  {
    if (this.controller.isSaved()) {
      this.controller.closeSet();
      this.tryMerge();
      return;
    }

    switch (this.onConfirmUnsaved()) {
      case CANCEL -> {
        return;
      }
      case DISCARD -> {
        this.controller.closeSet();
        this.tryMerge();
        return;
      }
      case SAVE -> {
        this.controller.save();
        this.controller.closeSet();
        this.tryMerge();
        return;
      }
    }
  }

  private void tryMerge()
  {
    this.mergeDialogs.open();
  }
}
