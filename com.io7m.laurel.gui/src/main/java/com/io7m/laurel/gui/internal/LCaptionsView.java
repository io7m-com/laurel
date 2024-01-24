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

import com.io7m.jwheatsheaf.api.JWFileChooserAction;
import com.io7m.jwheatsheaf.api.JWFileChooserConfiguration;
import com.io7m.jwheatsheaf.oxygen.JWOxygenIconSet;
import com.io7m.laurel.gui.internal.model.LMCaption;
import com.io7m.laurel.gui.internal.model.LMImage;
import com.io7m.laurel.gui.internal.model.LModelFileStatusType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static javafx.stage.Modality.APPLICATION_MODAL;

/**
 * The captions view.
 */

public final class LCaptionsView implements LScreenViewType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LCaptionsView.class);

  private final RPServiceDirectoryType services;
  private final LControllerType controller;
  private final LStrings strings;
  private final LFileChoosers choosers;
  private final LCaptionEditors editors;
  private final LPreferencesType preferences;

  @FXML private Parent captions;
  @FXML private TableView<LMCaption> captionsUnassignedView;
  @FXML private TableView<LMCaption> captionsAssignedView;
  @FXML private TableView<LMImage> imagesAll;
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

  private Stage imageDisplayWindow;
  private LImageView imageDisplay;

  /**
   * The captions view.
   *
   * @param inServices The service directory
   */

  public LCaptionsView(
    final RPServiceDirectoryType inServices)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.controller =
      inServices.requireService(LControllerType.class);
    this.strings =
      inServices.requireService(LStrings.class);
    this.choosers =
      inServices.requireService(LFileChoosers.class);
    this.editors =
      inServices.requireService(LCaptionEditors.class);
    this.preferences =
      inServices.requireService(LPreferencesType.class);
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.imageDisplayWindow = new Stage();
    this.imageDisplay =
      LImageView.create(
        this.imageDisplayWindow,
        this.services,
        this.strings
      );

    this.captions.setDisable(true);
    this.captions.setVisible(false);

    this.imageProgress.setVisible(false);
    this.errorImageLoad.setVisible(false);

    this.imageDelete.setDisable(true);
    this.imageCaptionAssign.setDisable(true);
    this.imageCaptionUnassign.setDisable(true);
    this.captionNew.setDisable(false);
    this.captionDelete.setDisable(true);

    this.controller.model()
      .fileStatus()
      .subscribe((oldValue, newValue) -> {
        this.handleImageSetStateChanged(newValue);
      });

    this.initializeImagesTable();
    this.initializeCaptionsAssignedTable();
    this.initializeCaptionsUnassignedTable();
  }

  private void initializeCaptionsUnassignedTable()
  {
    this.captionsUnassignedView.setPlaceholder(
      new Label(this.strings.format(LStringConstants.CAPTION_NONE_UNASSIGNED)));
    this.captionsUnassignedView.setColumnResizePolicy(
      TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
    this.captionsUnassignedView.setItems(
      this.controller.captionsUnassigned());
    this.captionsUnassignedView.setEditable(false);
    this.captionsUnassignedView.getSelectionModel()
      .setSelectionMode(SelectionMode.MULTIPLE);
    this.captionsUnassignedView.getSelectionModel()
      .selectedItemProperty()
      .subscribe(this::onCaptionUnassignedSelected);

    final var columns =
      this.captionsUnassignedView.getColumns();

    final var colText =
      (TableColumn<LMCaption, String>) columns.get(0);
    final var colCount =
      (TableColumn<LMCaption, Long>) columns.get(1);

    this.controller.captionsUnassigned()
      .comparatorProperty()
      .bind(this.captionsUnassignedView.comparatorProperty());

    colText.setSortable(true);
    colText.setReorderable(false);
    colText.setCellValueFactory(param -> {
      return param.getValue().text();
    });

    colCount.setPrefWidth(16.0);
    colCount.setSortable(true);
    colCount.setReorderable(false);
    colCount.setCellValueFactory(param -> {
      return param.getValue().count().asObject();
    });

    this.captionsUnassignedView.getSortOrder().add(colText);
    this.captionsUnassignedView.sort();
  }

  private void initializeCaptionsAssignedTable()
  {
    this.captionsAssignedView.setPlaceholder(
      new Label(""));
    this.captionsAssignedView.setItems(
      this.controller.captionsAssigned());
    this.captionsAssignedView.setEditable(false);
    this.captionsAssignedView.getSelectionModel()
      .setSelectionMode(SelectionMode.MULTIPLE);
    this.captionsAssignedView.getSelectionModel()
      .selectedItemProperty()
      .subscribe(this::onCaptionAssignedSelected);

    final var columns =
      this.captionsAssignedView.getColumns();

    final var colText =
      (TableColumn<LMCaption, String>) columns.get(0);
    final var colCount =
      (TableColumn<LMCaption, Long>) columns.get(1);

    this.controller.captionsAssigned()
      .comparatorProperty()
      .bind(this.captionsAssignedView.comparatorProperty());

    colText.setSortable(true);
    colText.setReorderable(false);
    colText.setCellValueFactory(param -> {
      return param.getValue().text();
    });

    colCount.setPrefWidth(16.0);
    colCount.setSortable(true);
    colCount.setReorderable(false);
    colCount.setCellValueFactory(param -> {
      return param.getValue().count().asObject();
    });

    this.captionsAssignedView.getSortOrder().add(colText);
    this.captionsAssignedView.sort();
  }

  private void initializeImagesTable()
  {
    this.imagesAll.setItems(this.controller.imageList());
    this.imagesAll.setEditable(false);

    final var columns =
      this.imagesAll.getColumns();

    final var colText =
      (TableColumn<LMImage, String>) columns.get(0);

    this.controller.imageList()
      .comparatorProperty()
      .bind(this.imagesAll.comparatorProperty());

    colText.setSortable(true);
    colText.setReorderable(false);
    colText.setCellValueFactory(param -> {
      return param.getValue().fileName().map(x -> x.getFileName().toString());
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
    final LMCaption caption)
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
    final LMCaption caption)
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
    final LMImage image)
  {
    this.updateImageCaptionUnassignButton();
    this.updateImageCaptionAssignButton();

    if (image == null) {
      this.controller.imageSelect(Optional.empty());
      this.imageView.setImage(null);
      this.imageDelete.setDisable(true);
      return;
    }

    this.imageDelete.setDisable(false);

    final var imageFileOpt =
      this.controller.imageSelect(Optional.of(image.id()));

    this.imageProgress.setVisible(true);
    if (imageFileOpt.isPresent()) {
      final var imageValue =
        new Image(
          imageFileOpt.get().toUri().toString(),
          256.0,
          256.0,
          true,
          true,
          true
        );

      this.imageProgress.setVisible(true);

      imageValue.exceptionProperty()
        .subscribe(e -> {
          if (e != null) {
            LOG.error("Image load: ", e);
            this.errorImageLoad.setVisible(true);
          }
          this.imageProgress.setVisible(true);
        });

      imageValue.progressProperty()
        .addListener((observable, oldValue, newValue) -> {
          if (newValue.doubleValue() >= 1.0) {
            this.imageProgress.setVisible(false);
          }
        });

      this.imageProgress.progressProperty()
        .bind(imageValue.progressProperty());

      this.imageView.setImage(imageValue);
      this.errorImageLoad.setVisible(false);
    } else {
      this.imageView.setImage(null);
      this.imageProgress.setVisible(false);
      this.errorImageLoad.setVisible(false);
    }
  }

  private void handleImageSetStateChanged(
    final LModelFileStatusType fileStatus)
  {
    Platform.runLater(() -> {
      final var none = fileStatus instanceof LModelFileStatusType.None;
      this.captions.setDisable(none);
      this.captions.setVisible(!none);
    });
  }

  @FXML
  private void onImageAdd()
    throws Exception
  {
    final var fileChooser =
      this.choosers.fileChoosers()
        .create(
          JWFileChooserConfiguration.builder()
            .setModality(Modality.APPLICATION_MODAL)
            .setAction(JWFileChooserAction.OPEN_EXISTING_MULTIPLE)
            .setCssStylesheet(LCSS.defaultCSS().toURL())
            .setRecentFiles(this.preferences.recentFiles())
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

    this.controller.imagesAdd(files);
  }

  @FXML
  private void onImageDelete()
  {
    final var images =
      List.copyOf(
        this.imagesAll.getSelectionModel()
          .getSelectedItems()
      );

    this.controller.imagesDelete(images);
  }

  @FXML
  private void onImageCaptionAssign()
  {
    final var images =
      this.imagesAll.getSelectionModel()
        .getSelectedItems();
    final var captionsAvailable =
      this.captionsUnassignedView.getSelectionModel()
        .getSelectedItems();

    this.controller.imageCaptionAssign(
      images.stream()
        .map(LMImage::id)
        .collect(Collectors.toList()),
      captionsAvailable.stream()
        .map(LMCaption::id)
        .collect(Collectors.toList())
    );

    this.captionsAssignedView.requestFocus();
  }

  @FXML
  private void onImageCaptionUnassign()
  {
    final var images =
      this.imagesAll.getSelectionModel()
        .getSelectedItems();
    final var captionsAssigned =
      this.captionsAssignedView.getSelectionModel()
        .getSelectedItems();

    this.controller.imageCaptionUnassign(
      images.stream()
        .map(LMImage::id)
        .collect(Collectors.toList()),
      captionsAssigned.stream()
        .map(LMCaption::id)
        .collect(Collectors.toList())
    );

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
      this.controller.captionNew(text);
    }
  }

  @FXML
  private void onCaptionDelete()
  {
    final var captionsAvailable =
      this.captionsUnassignedView.getSelectionModel()
        .getSelectedItems();

    if (!captionsAvailable.isEmpty()) {
      this.controller.captionRemove(captionsAvailable);
    }
  }

  @FXML
  private void onCaptionSearchChanged()
  {
    this.controller.captionsUnassignedSetFilter(
      this.captionAvailableSearch.getText().trim()
    );
  }

  @FXML
  private void onImageSearchChanged()
  {
    this.controller.imagesSetFilter(this.imageSearch.getText().trim());
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

      final var globals = new LGlobalPrefixCaptions(this.services, stage);
      loader.setControllerFactory(param -> {
        return globals;
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
}
