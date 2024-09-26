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
import com.io7m.jwheatsheaf.api.JWFileChooserAction;
import com.io7m.jwheatsheaf.api.JWFileChooserConfiguration;
import com.io7m.laurel.filemodel.LFileModelType;
import com.io7m.laurel.filemodel.LImageCaptionsAssignment;
import com.io7m.laurel.model.LCaption;
import com.io7m.laurel.model.LCaptionName;
import com.io7m.laurel.model.LImageWithID;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static javafx.stage.Modality.APPLICATION_MODAL;

/**
 * The captions view.
 */

public final class LCaptionsView extends LAbstractViewWithModel
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LCaptionsView.class);

  private final RPServiceDirectoryType services;
  private final LStrings strings;
  private final LFileChoosers choosers;
  private final LCaptionEditors editors;
  private final LPreferencesType preferences;
  private final LCaptionComparisonViews comparisons;

  @FXML private TableView<LCaption> captionsUnassignedView;
  @FXML private TableView<LCaption> captionsAssignedView;
  @FXML private TableView<LImageWithID> imagesAll;
  @FXML private ImageView imageView;
  @FXML private Parent errorImageLoad;
  @FXML private ProgressBar imageProgress;
  @FXML private Button imageAdd;
  @FXML private Button imageDelete;
  @FXML private Button imageCaptionAssign;
  @FXML private Button imageCaptionUnassign;
  @FXML private Button captionNew;
  @FXML private Button captionDelete;
  @FXML private TextField captionAvailableSearch;
  @FXML private TextField imageSearch;
  @FXML private ContextMenu assignedCaptionsContextMenu;
  @FXML private MenuItem assignedCaptionsContextMenuCopy;
  @FXML private MenuItem assignedCaptionsContextMenuPaste;
  @FXML private MenuItem imagesCompareCaptions;

  private Stage imageDisplayWindow;
  private LImageView imageDisplay;

  /**
   * The captions view.
   *
   * @param inServices The service directory
   */

  LCaptionsView(
    final RPServiceDirectoryType inServices,
    final LFileModelScope inFileModel)
  {
    super(inFileModel);

    this.services =
      Objects.requireNonNull(inServices, "services");
    this.strings =
      inServices.requireService(LStrings.class);
    this.choosers =
      inServices.requireService(LFileChoosers.class);
    this.editors =
      inServices.requireService(LCaptionEditors.class);
    this.preferences =
      inServices.requireService(LPreferencesType.class);
    this.comparisons =
      inServices.requireService(LCaptionComparisonViews.class);
  }

  @Override
  protected void onInitialize()
  {
    this.imageDisplayWindow = new Stage();
    this.imageDisplay =
      LImageView.create(
        this.imageDisplayWindow,
        this.fileModelScope(),
        this.strings
      );

    this.assignedCaptionsContextMenuPaste.setDisable(true);

    this.imageProgress.setVisible(false);
    this.errorImageLoad.setVisible(false);

    this.imageDelete.setDisable(true);
    this.imageCaptionAssign.setDisable(true);
    this.imageCaptionUnassign.setDisable(true);
    this.captionNew.setDisable(false);
    this.captionDelete.setDisable(true);

    this.initializeImagesTable();
    this.initializeCaptionsAssignedTable();
    this.initializeCaptionsUnassignedTable();
  }

  @Override
  protected void onFileBecameUnavailable()
  {

  }

  @Override
  protected void onFileBecameAvailable(
    final CloseableCollectionType<?> subscriptions,
    final LFileModelType fileModel)
  {
    subscriptions.add(
      fileModel.imageList().subscribe((oldValue, newValue) -> {
        Platform.runLater(() -> {
          this.imagesAll.setItems(
            FXCollections.observableList(newValue)
          );
        });
      })
    );

    subscriptions.add(
      fileModel.imageCaptionsAssigned().subscribe((oldValue, newValue) -> {
        Platform.runLater(() -> {
          this.captionsAssignedView.setItems(
            FXCollections.observableList(newValue)
          );
        });
      })
    );

    subscriptions.add(
      fileModel.imageCaptionsUnassigned().subscribe((oldValue, newValue) -> {
        Platform.runLater(() -> {
          this.captionsUnassignedView.setItems(
            FXCollections.observableList(newValue)
          );
        });
      })
    );
  }

  private void onCaptionsAssignedCopiedChanged(
    final Observable observable)
  {
    //    final var copied = this.controller.captionsAssignedCopied();
    //    LOG.debug("Captions copied: {}", copied);
    //    this.assignedCaptionsContextMenuPaste.setDisable(copied.isEmpty());
  }

  private void initializeCaptionsUnassignedTable()
  {
    this.captionsUnassignedView.setPlaceholder(
      new Label(this.strings.format(LStringConstants.CAPTION_NONE_UNASSIGNED)));
    this.captionsUnassignedView.setColumnResizePolicy(
      TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
    this.captionsUnassignedView.setEditable(false);
    this.captionsUnassignedView.getSelectionModel()
      .setSelectionMode(SelectionMode.MULTIPLE);
    this.captionsUnassignedView.getSelectionModel()
      .selectedItemProperty()
      .subscribe(this::onCaptionUnassignedSelected);

    final var columns =
      this.captionsUnassignedView.getColumns();

    final var colText =
      (TableColumn<LCaption, String>) columns.get(0);
    final var colCount =
      (TableColumn<LCaption, Long>) columns.get(1);

    colText.setSortable(true);
    colText.setReorderable(false);
    colText.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(
        param.getValue()
          .name()
          .text()
      );
    });

    colCount.setPrefWidth(16.0);
    colCount.setSortable(true);
    colCount.setReorderable(false);
    colCount.setCellValueFactory(param -> {
      return new ReadOnlyObjectWrapper<>(
        Long.valueOf(param.getValue().count())
      );
    });

    this.captionsUnassignedView.getSortOrder().add(colText);
    this.captionsUnassignedView.sort();
  }

  private void initializeCaptionsAssignedTable()
  {
    this.captionsAssignedView.setPlaceholder(
      new Label(""));
    this.captionsAssignedView.setEditable(false);
    this.captionsAssignedView.getSelectionModel()
      .setSelectionMode(SelectionMode.MULTIPLE);
    this.captionsAssignedView.getSelectionModel()
      .selectedItemProperty()
      .subscribe(this::onCaptionAssignedSelected);

    final var columns =
      this.captionsAssignedView.getColumns();

    final var colText =
      (TableColumn<LCaption, String>) columns.get(0);
    final var colCount =
      (TableColumn<LCaption, Long>) columns.get(1);

    colText.setSortable(true);
    colText.setReorderable(false);
    colText.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(
        param.getValue()
          .name()
          .text()
      );
    });

    colCount.setPrefWidth(16.0);
    colCount.setSortable(true);
    colCount.setReorderable(false);
    colCount.setCellValueFactory(param -> {
      return new ReadOnlyObjectWrapper<>(
        Long.valueOf(param.getValue().count())
      );
    });

    this.captionsAssignedView.getSortOrder().add(colText);
    this.captionsAssignedView.sort();
  }

  private void initializeImagesTable()
  {
    this.imagesAll.setEditable(false);

    final var columns =
      this.imagesAll.getColumns();

    final var colText = (TableColumn<LImageWithID, String>) columns.get(0);
    colText.setSortable(true);
    colText.setReorderable(false);
    colText.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(
        param.getValue()
          .image()
          .file()
          .map(x -> x.getFileName().toString())
          .orElse("")
      );
    });

    this.imagesAll.getSelectionModel()
      .setSelectionMode(SelectionMode.MULTIPLE);
    this.imagesAll.getSelectionModel()
      .selectedItemProperty()
      .subscribe(this::onImageSelected);

    this.imagesAll.getSortOrder().add(colText);
    this.imagesAll.sort();
  }

  private void onCaptionUnassignedSelected(
    final LCaption caption)
  {
    this.updateImageCaptionUnassignButton();
    this.updateImageCaptionAssignButton();

    if (caption == null) {
      this.captionDelete.setDisable(true);
      return;
    }

    this.captionDelete.setDisable(false);
  }

  private void onCaptionAssignedSelected(
    final LCaption tag)
  {
    this.updateImageCaptionAssignButton();
    this.updateImageCaptionUnassignButton();
  }

  private void updateImageCaptionUnassignButton()
  {
    final var imageSelected =
      !this.imagesAll.getSelectionModel()
        .getSelectedItems()
        .isEmpty();

    final var captionSelected =
      !this.captionsAssignedView.getSelectionModel()
        .getSelectedItems()
        .isEmpty();

    if (imageSelected && captionSelected) {
      this.imageCaptionUnassign.setDisable(false);
    } else {
      this.imageCaptionUnassign.setDisable(true);
    }
  }

  private void updateImageCaptionAssignButton()
  {
    final var imageSelected =
      !this.imagesAll.getSelectionModel()
        .getSelectedItems()
        .isEmpty();

    final var captionSelected =
      !this.captionsUnassignedView.getSelectionModel()
        .getSelectedItems()
        .isEmpty();

    if (imageSelected && captionSelected) {
      this.imageCaptionAssign.setDisable(false);
    } else {
      this.imageCaptionAssign.setDisable(true);
    }
  }

  private void onImageSelected(
    final LImageWithID image)
  {
    this.updateImageCaptionUnassignButton();
    this.updateImageCaptionAssignButton();
    this.updateImageCaptionsCompareMenuItem();

    final var fileModelOpt =
      this.fileModelScope().get();

    if (fileModelOpt.isEmpty()) {
      return;
    }

    final var fileModel = fileModelOpt.get();
    if (image == null) {
      fileModel.imageSelect(Optional.empty());
      this.imageView.setImage(null);
      this.imageDelete.setDisable(true);
      return;
    }

    this.imageDelete.setDisable(false);

    fileModel.imageSelect(Optional.of(image.id()));
    fileModel.imageStream(image.id())
      .thenAccept(inputStreamOpt -> {
        Platform.runLater(() -> {
          LImages.imageLoad(
            inputStreamOpt,
            this.imageProgress,
            this.imageView,
            this.errorImageLoad,
            256.0,
            256.0
          );
        });
      });
  }

  private void updateImageCaptionsCompareMenuItem()
  {
    final var selected =
      List.copyOf(this.imagesAll.getSelectionModel().getSelectedItems());

    if (selected.size() == 2) {
      this.imagesCompareCaptions.setDisable(false);
    } else {
      this.imagesCompareCaptions.setDisable(true);
    }
  }

  @FXML
  private void onImageAdd()
    throws Exception
  {
    final var fileChooser =
      this.choosers.create(
        JWFileChooserConfiguration.builder()
          .setModality(APPLICATION_MODAL)
          .setAction(JWFileChooserAction.OPEN_EXISTING_MULTIPLE)
          .setCssStylesheet(LCSS.defaultCSS().toURL())
          .setRecentFiles(this.preferences.recentFiles())
          .build()
      );

    final var files = fileChooser.showAndWait();
    if (files.isEmpty()) {
      return;
    }

    for (final var file : files) {
      this.preferences.addRecentFile(file);
    }

    final var fileModel = this.fileModelNow();
    for (final var file : files) {
      fileModel.imageAdd(
        file.getFileName().toString(),
        file.toAbsolutePath(),
        Optional.of(file.toUri())
      );
    }
  }

  @FXML
  private void onImageDelete()
  {
    final var images =
      this.imagesAll.getSelectionModel()
        .getSelectedItems()
        .stream()
        .map(LImageWithID::id)
        .toList();

    this.fileModelNow()
      .imagesDelete(images);
  }

  @FXML
  private void onImageCaptionAssign()
  {
    final var images =
      this.imagesAll.getSelectionModel()
        .getSelectedItems()
        .stream()
        .map(LImageWithID::id)
        .toList();

    final var tags =
      this.captionsUnassignedView.getSelectionModel()
        .getSelectedItems()
        .stream()
        .map(LCaption::id)
        .collect(Collectors.toSet());

    final var assignments =
      images.stream()
        .map(x -> new LImageCaptionsAssignment(x, tags))
        .toList();

    this.fileModelNow().imageCaptionsAssign(assignments);
    this.captionsAssignedView.requestFocus();
  }

  @FXML
  private void onImageCaptionUnassign()
  {
    final var images =
      this.imagesAll.getSelectionModel()
        .getSelectedItems()
        .stream()
        .map(LImageWithID::id)
        .toList();

    final var tags =
      this.captionsAssignedView.getSelectionModel()
        .getSelectedItems()
        .stream()
        .map(LCaption::id)
        .collect(Collectors.toSet());

    final var assignments =
      images.stream()
        .map(x -> new LImageCaptionsAssignment(x, tags))
        .toList();

    this.fileModelNow().imageCaptionsUnassign(assignments);
    this.captionsUnassignedView.requestFocus();
  }

  @FXML
  private void onCaptionNew()
  {
    final var editor =
      this.editors.open("");
    final var result =
      editor.result();

    if (result.isPresent()) {
      final var text = result.get();
      this.fileModelNow().captionAdd(new LCaptionName(text));
    }
  }

  @FXML
  private void onCaptionDelete()
  {
    final var captionsAvailable =
      this.captionsUnassignedView.getSelectionModel()
        .getSelectedItems();

    if (!captionsAvailable.isEmpty()) {
      this.fileModelNow()
        .captionRemove(
          captionsAvailable.stream()
            .map(LCaption::id)
            .collect(Collectors.toSet())
        );
    }
  }

  @FXML
  private void onCaptionSearchChanged()
  {
    //    this.controller.captionsUnassignedSetFilter(
    //      this.captionAvailableSearch.getText().trim()
    //    );
  }

  @FXML
  private void onImageSearchChanged()
  {
    // this.controller.imagesSetFilter(this.imageSearch.getText().trim());
  }

  @FXML
  private void onCaptionGlobal()
  {
    try {
      final var stage = new Stage();

      final var layout =
        LCaptionsView.class.getResource(
          "/com/io7m/laurel/gui/internal/globalPrefixCaptions.fxml");

      Objects.requireNonNull(layout, "layout");

      final var loader =
        new FXMLLoader(layout, this.strings.resources());

      final LViewControllerFactoryType<LViewType> controllers =
        LViewControllerFactoryMapped.create(
          Map.entry(
            LGlobalPrefixCaptions.class,
            () -> {
              return new LGlobalPrefixCaptions(
                this.services,
                this.fileModelScope(),
                stage
              );
            }
          )
        );

      loader.setControllerFactory(param -> {
        return controllers.call((Class<? extends LViewType>) param);
      });

      final Pane pane = loader.load();
      LCSS.setCSS(pane);

      final var width = 650.0;
      final var height = 432.0;
      stage.initModality(APPLICATION_MODAL);
      stage.setTitle(this.strings.format("globals"));
      stage.setWidth(width);
      stage.setMaxWidth(width);
      stage.setMinWidth(width);
      stage.setMinHeight(height);
      stage.setHeight(height);
      stage.setMaxHeight(height);
      stage.setScene(new Scene(pane));
      stage.showAndWait();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @FXML
  private void onImageClicked()
  {
    if (!this.imageDisplayWindow.isShowing()) {
      this.imageDisplayWindow.show();
    }
  }

  @FXML
  private void onCaptionsAssignedCopy()
  {
    //    final var captionsCopied =
    //      List.copyOf(
    //        this.captionsAssignedView.getSelectionModel()
    //          .getSelectedItems()
    //      );
    //
    //    LOG.debug("Copying captions: {}", captionsCopied);
    //    this.controller.captionsAssignedCopy(captionsCopied);
  }

  @FXML
  private void onCaptionsAssignedPaste()
  {
    //    this.controller.captionsAssignedPaste();
  }

  @FXML
  private void onCaptionsCompareSelected()
  {
    final var selected =
      this.imagesAll.getSelectionModel()
        .getSelectedItems();

    this.fileModelNow()
      .imagesCompare(
        selected.get(0).id(),
        selected.get(1).id()
      );

    this.comparisons.open(this.services, this.fileModelScope());
  }
}
