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
import com.io7m.laurel.model.LImage;
import com.io7m.laurel.model.LImageCaption;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * The captions view.
 */

public final class LCaptionsView implements LScreenViewType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LCaptionsView.class);

  private final LControllerType controller;
  private final LStrings strings;
  private final LFileChoosers choosers;
  private final LCaptionEditors editors;
  private final LPreferencesType preferences;

  @FXML private Parent captions;
  @FXML private TableView<LImageCaption> captionsAvailableView;
  @FXML private ListView<LImageCaption> captionsAssignedView;
  @FXML private TableView<LImage> imagesAll;
  @FXML private ImageView imageView;
  @FXML private Parent errorImageLoad;

  @FXML private Button imageAdd;
  @FXML private Button imageDelete;
  @FXML private Button imageCaptionAssign;
  @FXML private Button imageCaptionUnassign;
  @FXML private Button imageCaptionPriorityUp;
  @FXML private Button imageCaptionPriorityDown;
  @FXML private Button captionNew;
  @FXML private Button captionDelete;

  /**
   * The captions view.
   *
   * @param services The service directory
   */

  public LCaptionsView(
    final RPServiceDirectoryType services)
  {
    this.controller =
      services.requireService(LControllerType.class);
    this.strings =
      services.requireService(LStrings.class);
    this.choosers =
      services.requireService(LFileChoosers.class);
    this.editors =
      services.requireService(LCaptionEditors.class);
    this.preferences =
      services.requireService(LPreferencesType.class);
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.captions.setDisable(true);
    this.captions.setVisible(false);
    this.errorImageLoad.setVisible(false);

    this.imageDelete.setDisable(true);
    this.imageCaptionAssign.setDisable(true);
    this.imageCaptionUnassign.setDisable(true);
    this.imageCaptionPriorityUp.setDisable(true);
    this.imageCaptionPriorityDown.setDisable(true);
    this.captionNew.setDisable(false);
    this.captionDelete.setDisable(true);

    this.controller.imageSetState()
      .subscribe((oldValue, newValue) -> this.handleImageSetStateChanged());

    this.imagesAll.setItems(this.controller.imageListReadable());
    this.imagesAll.setEditable(false);

    {
      final var columns =
        this.imagesAll.getColumns();

      final var colText =
        (TableColumn<LImage, String>) columns.get(0);

      this.controller.imageListReadable()
        .comparatorProperty()
        .bind(this.imagesAll.comparatorProperty());

      colText.setSortable(true);
      colText.setReorderable(false);
      colText.setCellValueFactory(param -> {
        return new ReadOnlyStringWrapper(param.getValue().fileName());
      });
    }

    this.imagesAll.getSelectionModel()
      .setSelectionMode(SelectionMode.MULTIPLE);
    this.imagesAll.getSelectionModel()
      .selectedItemProperty()
      .subscribe(this::onImageSelected);

    this.captionsAssignedView.setItems(this.controller.captionListAssigned());
    this.captionsAssignedView.setEditable(false);
    this.captionsAssignedView.setCellFactory(param -> new LImageCaptionCell());
    this.captionsAssignedView.getSelectionModel()
      .setSelectionMode(SelectionMode.MULTIPLE);
    this.captionsAssignedView.getSelectionModel()
      .selectedItemProperty()
      .subscribe(this::onImageCaptionSelected);

    {
      final var columns =
        this.captionsAvailableView.getColumns();

      final var colText =
        (TableColumn<LImageCaption, String>) columns.get(0);
      final var colCount =
        (TableColumn<LImageCaption, Long>) columns.get(1);

      this.controller.captionListAvailable()
        .comparatorProperty()
        .bind(this.captionsAvailableView.comparatorProperty());

      colText.setSortable(true);
      colText.setReorderable(false);
      colText.setCellValueFactory(param -> {
        return new ReadOnlyStringWrapper(param.getValue().text());
      });

      colCount.setSortable(true);
      colCount.setReorderable(false);
      colCount.setCellValueFactory(param -> {
        final var countThen =
          this.controller.captionCount(param.getValue().id());

        final var prop = new SimpleLongProperty(countThen);
        this.controller.captionListAvailable()
          .addListener((ListChangeListener<? super LImageCaption>) c -> {
            final var countNow =
              this.controller.captionCount(param.getValue().id());
            prop.set(countNow);
          });
        return prop.asObject();
      });
    }

    this.captionsAvailableView.setPlaceholder(
      new Label(this.strings.format(LStringConstants.CAPTION_NONE)));
    this.captionsAvailableView.setColumnResizePolicy(
      TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
    this.captionsAvailableView.setItems(this.controller.captionListAvailable());
    this.captionsAvailableView.setEditable(false);
    this.captionsAvailableView.getSelectionModel()
      .setSelectionMode(SelectionMode.MULTIPLE);
    this.captionsAvailableView.getSelectionModel()
      .selectedItemProperty()
      .subscribe(this::onCaptionSelected);
  }

  private void onCaptionSelected(
    final LImageCaption caption)
  {
    this.updateImageCaptionUnassignButton();
    this.updateImageCaptionAssignButton();
    this.updateImageCaptionPriorityButtons();

    if (caption == null) {
      this.captionDelete.setDisable(true);
      return;
    }

    this.captionDelete.setDisable(false);
  }

  private void onImageCaptionSelected(
    final LImageCaption caption)
  {
    this.updateImageCaptionAssignButton();
    this.updateImageCaptionUnassignButton();
    this.updateImageCaptionPriorityButtons();
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
      !this.captionsAvailableView.getSelectionModel()
        .getSelectedItems()
        .isEmpty();

    if (imageSelected && captionSelected) {
      this.imageCaptionAssign.setDisable(false);
    } else {
      this.imageCaptionAssign.setDisable(true);
    }
  }

  private void onImageSelected(
    final LImage image)
  {
    this.updateImageCaptionUnassignButton();
    this.updateImageCaptionAssignButton();
    this.updateImageCaptionPriorityButtons();

    if (image == null) {
      this.controller.imageSelect(Optional.empty());
      this.imageView.setImage(null);
      this.imageDelete.setDisable(true);
      return;
    }

    this.imageDelete.setDisable(false);

    final var imageFileOpt =
      this.controller.imageSelect(Optional.of(image.imageID()));

    if (imageFileOpt.isPresent()) {
      final var imageValue =
        new Image(
          imageFileOpt.get().toUri().toString(),
          256.0,
          256.0,
          false,
          false,
          true
        );

      imageValue.exceptionProperty()
        .subscribe(e -> {
          LOG.error("Image load: ", e);
          this.errorImageLoad.setVisible(true);
        });

      this.imageView.setImage(imageValue);
      this.errorImageLoad.setVisible(false);
    }
  }

  private void updateImageCaptionPriorityButtons()
  {
    final var imageSelected =
      !this.imagesAll.getSelectionModel()
        .getSelectedItems()
        .isEmpty();

    final var captionSelected =
      this.captionsAssignedView.getSelectionModel()
        .getSelectedItems()
        .size() == 1;

    if (imageSelected && captionSelected) {
      this.imageCaptionPriorityDown.setDisable(false);
      this.imageCaptionPriorityUp.setDisable(false);
    } else {
      this.imageCaptionPriorityDown.setDisable(true);
      this.imageCaptionPriorityUp.setDisable(true);
    }
  }

  private static final class LImageCell
    extends ListCell<LImage>
  {
    LImageCell()
    {

    }

    @Override
    protected void updateItem(
      final LImage item,
      final boolean empty)
    {
      super.updateItem(item, empty);

      this.setGraphic(null);
      this.setText(null);
      this.setTooltip(null);

      if (empty || item == null) {
        return;
      }

      this.setTooltip(new Tooltip(item.imageID().toString()));
      this.setText(item.fileName());
    }
  }

  private static final class LImageCaptionCell
    extends ListCell<LImageCaption>
  {
    LImageCaptionCell()
    {

    }

    @Override
    protected void updateItem(
      final LImageCaption item,
      final boolean empty)
    {
      super.updateItem(item, empty);

      this.setGraphic(null);
      this.setText(null);
      this.setTooltip(null);

      if (empty || item == null) {
        return;
      }

      this.setTooltip(new Tooltip(item.id().toString()));
      this.setText(item.text());
    }
  }

  private void handleImageSetStateChanged()
  {
    final var state = this.controller.imageSetState().get();
    Platform.runLater(() -> {
      final var none = state instanceof LImageSetStateNone;
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

  }

  @FXML
  private void onImageCaptionPriorityUp()
  {
    final var image =
      this.imagesAll.getSelectionModel()
        .getSelectedItem();
    final var caption =
      this.captionsAssignedView.getSelectionModel()
        .getSelectedItem();

    this.controller.imageCaptionPriorityIncrease(
      image.imageID(),
      caption.id()
    );
    this.captionsAssignedView.requestFocus();
  }

  @FXML
  private void onImageCaptionPriorityDown()
  {
    final var image =
      this.imagesAll.getSelectionModel()
        .getSelectedItem();
    final var caption =
      this.captionsAssignedView.getSelectionModel()
        .getSelectedItem();

    this.controller.imageCaptionPriorityDecrease(
      image.imageID(),
      caption.id()
    );
    this.captionsAssignedView.requestFocus();
  }

  @FXML
  private void onImageCaptionAssign()
  {
    final var image =
      this.imagesAll.getSelectionModel()
        .getSelectedItem();
    final var captionsAvailable =
      this.captionsAvailableView.getSelectionModel()
        .getSelectedItems();

    this.controller.imageCaptionAssign(
      image.imageID(),
      captionsAvailable.stream()
        .map(LImageCaption::id)
        .collect(Collectors.toList())
    );

    this.captionsAssignedView.requestFocus();
  }

  @FXML
  private void onImageCaptionUnassign()
  {
    final var image =
      this.imagesAll.getSelectionModel()
        .getSelectedItem();
    final var captionsAssigned =
      this.captionsAssignedView.getSelectionModel()
        .getSelectedItems();

    this.controller.imageCaptionUnassign(
      image.imageID(),
      captionsAssigned.stream()
        .map(LImageCaption::id)
        .collect(Collectors.toList())
    );

    this.captionsAvailableView.requestFocus();
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
      this.captionsAvailableView.getSelectionModel()
        .getSelectedItems();

    if (!captionsAvailable.isEmpty()) {
      this.controller.captionRemove(captionsAvailable);
    }
  }
}
