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
import com.io7m.laurel.filemodel.LFileModelType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

import static com.io7m.darco.api.DDatabaseUnit.UNIT;

/**
 * The main controller.
 */

public final class LFileView extends LAbstractViewWithModel
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LFileView.class);

  private final LStrings strings;
  private final LFileChoosers choosers;
  private final Stage stage;
  private final LExporterDialogs exporterDialogs;
  private final LPreferencesType preferences;
  private final RPServiceDirectoryType services;

  private @FXML Parent mainContent;
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
      this.services.requireService(LFileChoosers.class);
    this.exporterDialogs =
      this.services.requireService(LExporterDialogs.class);
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
   * @throws Exception On errors
   */

  public static LViewAndStage<LFileView> openForStage(
    final RPServiceDirectoryType services,
    final LFileModelScope fileScope,
    final Stage stage)
    throws Exception
  {
    final var strings =
      services.requireService(LStrings.class);

    final var xml =
      LFileView.class.getResource(
        "/com/io7m/laurel/gui/internal/main.fxml"
      );
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
        )
      );

    loader.setControllerFactory(param -> {
      return controllers.call((Class<? extends LViewType>) param);
    });

    final var pane = loader.<Pane>load();
    LCSS.setCSS(pane);
    stage.setScene(new Scene(pane));
    stage.setTitle(strings.format(LStringConstants.TITLE));
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
  }

  @Override
  protected void onFileBecameUnavailable()
  {
    Platform.runLater(() -> {
      this.mainContent.setDisable(true);
      this.mainContent.setVisible(false);
    });
  }

  @Override
  protected void onFileBecameAvailable(
    final CloseableCollectionType<?> subscriptions,
    final LFileModelType fileModel)
  {
    Platform.runLater(() -> {
      this.mainContent.setDisable(false);
      this.mainContent.setVisible(true);
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
    this.tryNew();
  }

  private DDatabaseUnit tryNew()
    throws Exception
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
   * The user tried to open an image set.
   *
   * @throws Exception On errors
   */

  @FXML
  public void onOpenSelected()
    throws Exception
  {
    this.tryOpen();
  }

  private DDatabaseUnit tryOpen()
    throws Exception
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
  {

  }

  /**
   * The user tried to save an image set.
   */

  @FXML
  public void onSaveSelected()
  {

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

  }

  /**
   * The user tried to exit.
   */

  @FXML
  public void onExitSelected()
  {

  }

  /**
   * The user tried to export.
   */

  @FXML
  public void onExportSelected()
  {

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

  }

  /**
   * The user tried to undo an operation.
   */

  @FXML
  public void onUndo()
  {

  }

  /**
   * The user tried to redo an operation.
   */

  @FXML
  public void onRedo()
  {

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
