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

import com.io7m.darco.api.DDatabaseUnit;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jwheatsheaf.api.JWFileChooserAction;
import com.io7m.jwheatsheaf.api.JWFileChooserConfiguration;
import com.io7m.laurel.filemodel.LFileModelEventType;
import com.io7m.laurel.filemodel.LFileModelStatusError;
import com.io7m.laurel.filemodel.LFileModelStatusIdle;
import com.io7m.laurel.filemodel.LFileModelStatusLoading;
import com.io7m.laurel.filemodel.LFileModelStatusRunningCommand;
import com.io7m.laurel.filemodel.LFileModelStatusType;
import com.io7m.laurel.filemodel.LFileModelType;
import com.io7m.laurel.model.LException;
import com.io7m.miscue.fx.seltzer.MSErrorDialogs;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.darco.api.DDatabaseUnit.UNIT;
import static com.io7m.laurel.gui.internal.LStringConstants.REDO;
import static com.io7m.laurel.gui.internal.LStringConstants.REDO_SPECIFIC;
import static com.io7m.laurel.gui.internal.LStringConstants.TITLE;
import static com.io7m.laurel.gui.internal.LStringConstants.UNDO;
import static com.io7m.laurel.gui.internal.LStringConstants.UNDO_SPECIFIC;

/**
 * The main controller.
 */

public final class LFileView extends LAbstractViewWithModel
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LFileView.class);

  private final LStrings strings;
  private final LFileChoosersType choosers;
  private final Stage stage;
  private final LPreferencesType preferences;
  private final RPServiceDirectoryType services;

  private @FXML Parent mainContent;
  private @FXML Parent root;
  private @FXML MenuItem menuItemNew;
  private @FXML MenuItem menuItemOpen;
  private @FXML MenuItem menuItemClose;
  private @FXML MenuItem menuItemExport;
  private @FXML MenuItem menuItemImport;
  private @FXML MenuItem menuItemExit;
  private @FXML MenuItem menuItemUndo;
  private @FXML MenuItem menuItemRedo;
  private @FXML Label statusLabel;
  private @FXML ProgressBar statusProgress;
  private @FXML LCaptionsView captionsController;

  /**
   * The main controller.
   *
   * @param inServices The service directory
   * @param inStage    The stage
   */

  private LFileView(
    final RPServiceDirectoryType inServices,
    final LFileModelScope inFileModel,
    final Stage inStage)
  {
    super(inFileModel);

    this.services =
      Objects.requireNonNull(inServices, "services");

    this.strings =
      this.services.requireService(LStrings.class);
    this.choosers =
      this.services.requireService(LFileChoosersType.class);
    this.preferences =
      this.services.requireService(LPreferencesType.class);

    this.stage =
      Objects.requireNonNull(inStage, "stage");
  }

  /**
   * Open a new file view for the given stage.
   *
   * @param services  The service directory
   * @param fileScope The file scope
   * @param stage     The stage
   *
   * @return A view and stage
   *
   * @throws LException On errors
   */

  public static LViewAndStage<LFileView> openForStage(
    final RPServiceDirectoryType services,
    final LFileModelScope fileScope,
    final Stage stage)
    throws LException
  {
    final var strings =
      services.requireService(LStrings.class);

    final var resourceName =
      "/com/io7m/laurel/gui/internal/main.fxml";
    final var xml =
      LFileView.class.getResource(resourceName);

    final var resources =
      strings.resources();
    final var loader =
      new FXMLLoader(xml, resources);

    final LViewControllerFactoryType<LViewType> controllers =
      LViewControllerFactoryMapped.create(
        Map.entry(
          LFileView.class,
          () -> {
            return new LFileView(services, fileScope, stage);
          }
        ),
        Map.entry(
          LCaptionsView.class,
          () -> {
            return new LCaptionsView(services, fileScope);
          }
        ),
        Map.entry(
          LCategoriesView.class,
          () -> {
            return new LCategoriesView(services, fileScope);
          }
        ),
        Map.entry(
          LMetadataView.class,
          () -> {
            return new LMetadataView(services, fileScope);
          }
        ),
        Map.entry(
          LHistoryView.class,
          () -> {
            return new LHistoryView(services, fileScope);
          }
        ),
        Map.entry(
          LValidationView.class,
          () -> {
            return new LValidationView(services, fileScope);
          }
        )
      );

    loader.setControllerFactory(param -> {
      return controllers.call((Class<? extends LViewType>) param);
    });

    final Pane pane;
    try {
      pane = loader.load();
    } catch (final IOException e) {
      throw new LException(
        Objects.requireNonNullElse(e.getMessage(), e.getClass().getName()),
        e,
        "error-io",
        Map.of("Resource", resourceName)
      );
    }

    LCSS.setCSS(pane);
    stage.setScene(new Scene(pane));
    stage.setTitle(strings.format(TITLE));
    stage.setWidth(800.0);
    stage.setHeight(600.0);

    return new LViewAndStage<>(loader.getController(), stage);
  }

  @Override
  protected void onInitialize()
  {
    LCSS.setCSS(this.root);

    this.mainContent.setDisable(true);
    this.mainContent.setVisible(false);
    this.menuItemClose.setDisable(true);
    this.menuItemRedo.setDisable(true);
    this.menuItemUndo.setDisable(true);
    this.menuItemExport.setDisable(true);
  }

  @Override
  protected void onFileBecameUnavailable()
  {
    Platform.runLater(() -> {
      this.mainContent.setDisable(true);
      this.mainContent.setVisible(false);
      this.menuItemClose.setDisable(true);
      this.menuItemExport.setDisable(true);
    });
  }

  @Override
  protected void onFileBecameAvailable(
    final CloseableCollectionType<?> subscriptions,
    final LFileModelType fileModel)
  {
    Platform.runLater(() -> {
      this.mainContent.setVisible(true);
      this.menuItemClose.setDisable(false);
      this.menuItemExport.setDisable(false);
    });

    final var eventSubscriber =
      subscriptions.add(
        new LPerpetualSubscriber<LFileModelEventType>(event -> {
          Platform.runLater(() -> {
            this.onFileModelEvent(event);
          });
        })
      );

    fileModel.events().subscribe(eventSubscriber);

    subscriptions.add(
      fileModel.undoText()
        .subscribe((oldValue, newValue) -> {
          Platform.runLater(() -> this.onUndoStateChanged(newValue));
        })
    );

    subscriptions.add(
      fileModel.redoText()
        .subscribe((oldValue, newValue) -> {
          Platform.runLater(() -> this.onRedoStateChanged(newValue));
        })
    );

    subscriptions.add(
      fileModel.status()
        .subscribe((oldValue, newValue) -> {
          Platform.runLater(() -> this.onStatusChanged(newValue));
        })
    );
  }

  private void onStatusChanged(
    final LFileModelStatusType newValue)
  {
    switch (newValue) {
      case final LFileModelStatusError error -> {
        this.mainContent.setDisable(false);
      }
      case final LFileModelStatusIdle idle -> {
        this.mainContent.setDisable(false);
      }
      case final LFileModelStatusLoading loading -> {
        this.mainContent.setDisable(true);
      }
      case final LFileModelStatusRunningCommand command -> {
        this.mainContent.setDisable(false);
      }
    }
  }

  private void onFileModelEvent(
    final LFileModelEventType event)
  {
    this.statusProgress.setProgress(event.progress().orElse(0.0));
    this.statusLabel.setText(event.message());
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
    this.menuItemUndo.setDisable(true);
    this.menuItemUndo.setText(this.strings.format(UNDO));
  }

  private void undoEnable(
    final String text)
  {
    this.menuItemUndo.setDisable(false);
    this.menuItemUndo.setText(this.strings.format(UNDO_SPECIFIC, text));
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
    this.menuItemRedo.setDisable(true);
    this.menuItemRedo.setText(this.strings.format(REDO));
  }

  private void redoEnable(
    final String text)
  {
    this.menuItemRedo.setDisable(false);
    this.menuItemRedo.setText(this.strings.format(REDO_SPECIFIC, text));
  }

  /**
   * The user tried to create a new image set.
   */

  @FXML
  public void onNewSelected()
  {
    try {
      this.tryNew();
    } catch (final LException e) {
      MSErrorDialogs.builder(e)
        .setCSS(LCSS.defaultCSS())
        .setModality(Modality.APPLICATION_MODAL)
        .build()
        .showAndWait();
    }
  }

  private DDatabaseUnit tryNew()
    throws LException
  {
    final var chooser =
      this.choosers.create(
        JWFileChooserConfiguration.builder()
          .setAction(JWFileChooserAction.CREATE)
          .build()
      );

    final var chosen = chooser.showAndWait();
    if (chosen.isEmpty()) {
      return UNIT;
    }

    final var file = chosen.get(0);
    this.choosers.setMostRecentDirectory(file.getParent());

    final var fileScope =
      this.fileModelScope();
    final var existingFileOpt =
      fileScope.get();

    if (existingFileOpt.isEmpty()) {
      LOG.debug("Opening file in the current file scope.");
      fileScope.reopen(file);
    } else {
      LOG.debug("Opening file in a new stage and file scope.");
      final var viewAndStage =
        openForStage(
          this.services,
          LFileModelScope.createNewScope(file),
          new Stage()
        );
      viewAndStage.stage().show();
    }

    return UNIT;
  }

  /**
   * The user tried to open a dataset.
   */

  @FXML
  public void onOpenSelected()
  {
    try {
      this.tryOpen();
    } catch (final LException e) {
      MSErrorDialogs.builder(e)
        .setCSS(LCSS.defaultCSS())
        .setModality(Modality.APPLICATION_MODAL)
        .build()
        .showAndWait();
    }
  }

  private DDatabaseUnit tryOpen()
    throws LException
  {
    final var chooser =
      this.choosers.create(
        JWFileChooserConfiguration.builder()
          .setAction(JWFileChooserAction.OPEN_EXISTING_SINGLE)
          .build()
      );

    final var chosen = chooser.showAndWait();
    if (chosen.isEmpty()) {
      return UNIT;
    }

    final var file = chosen.get(0);
    this.choosers.setMostRecentDirectory(file.getParent());

    final var fileScope =
      this.fileModelScope();
    final var existingFileOpt =
      fileScope.get();

    if (existingFileOpt.isEmpty()) {
      LOG.debug("Opening file in the current file scope.");
      fileScope.reopen(file);
    } else {
      LOG.debug("Opening file in a new stage and file scope.");
      final var viewAndStage =
        openForStage(
          this.services,
          LFileModelScope.createNewScope(file),
          new Stage()
        );
      viewAndStage.stage().show();
    }

    return UNIT;
  }

  /**
   * The user tried to close the image set.
   */

  @FXML
  public void onCloseSelected()
    throws LException
  {
    this.fileModelClose();
  }

  /**
   * The user tried to exit.
   */

  @FXML
  public void onExitSelected()
  {
    Platform.exit();
  }

  /**
   * The user tried to export.
   *
   * @throws Exception On errors
   */

  @FXML
  public void onExportSelected()
    throws Exception
  {
    final var p =
      LExporterView.openForStage(
        this.services,
        this.fileModelScope(),
        new Stage()
      );

    p.stage().showAndWait();
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
    final var p =
      LImporterView.openForStage(this.services, new Stage());

    p.stage().showAndWait();
  }

  /**
   * The user tried to undo an operation.
   */

  @FXML
  public void onUndo()
  {
    this.fileModelNow().undo();
  }

  /**
   * The user tried to redo an operation.
   */

  @FXML
  public void onRedo()
  {
    this.fileModelNow().redo();
  }

  /**
   * The user tried to open the about screen.
   */

  @FXML
  public void onAboutSelected()
  {
    LAbout.open(this.strings);
  }
}
