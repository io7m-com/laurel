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
import com.io7m.laurel.filemodel.LImageCaptionsAssignment;
import com.io7m.laurel.filemodel.LImageComparison;
import com.io7m.laurel.model.LCaption;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.io7m.laurel.gui.internal.LStringConstants.CAPTIONS_COMPARE_NO_EXTRA_CAPTIONS_PRESENT;

/**
 * The view for image comparisons.
 */

public final class LCaptionComparisonView
  extends LAbstractViewWithModel
{
  private final LStrings strings;
  @FXML private Button captionLeft;
  @FXML private Button captionRight;
  @FXML private ImageView compareImageL;
  @FXML private ImageView compareImageLError;
  @FXML private ImageView compareImageR;
  @FXML private ImageView compareImageRError;
  @FXML private Label compareImageLName;
  @FXML private Label compareImageRName;
  @FXML private ProgressBar compareImageLProgress;
  @FXML private ProgressBar compareImageRProgress;
  @FXML private TableColumn<LCaption, Long> imageLCount;
  @FXML private TableColumn<LCaption, Long> imageRCount;
  @FXML private TableColumn<LCaption, String> imageLText;
  @FXML private TableColumn<LCaption, String> imageRText;
  @FXML private TableView<LCaption> compareImageLCaptions;
  @FXML private TableView<LCaption> compareImageRCaptions;

  LCaptionComparisonView(
    final Stage stage,
    final RPServiceDirectoryType services,
    final LFileModelScope inFileModel)
  {
    super(inFileModel);

    this.strings =
      services.requireService(LStrings.class);
  }

  @Override
  protected void onInitialize()
  {
    this.captionLeft.setDisable(true);
    this.captionRight.setDisable(true);

    final var placeholder0 =
      new Label(
        this.strings.format(CAPTIONS_COMPARE_NO_EXTRA_CAPTIONS_PRESENT));
    placeholder0.setTextAlignment(TextAlignment.CENTER);

    this.compareImageLCaptions.setPlaceholder(placeholder0);
    this.compareImageLCaptions.getSelectionModel()
      .setSelectionMode(SelectionMode.MULTIPLE);

    final var placeholder1 =
      new Label(this.strings.format(CAPTIONS_COMPARE_NO_EXTRA_CAPTIONS_PRESENT));
    placeholder1.setTextAlignment(TextAlignment.CENTER);

    this.compareImageRCaptions.setPlaceholder(placeholder1);
    this.compareImageRCaptions.getSelectionModel()
      .setSelectionMode(SelectionMode.MULTIPLE);

    this.imageLText.setSortable(true);
    this.imageLText.setReorderable(false);
    this.imageLText.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(
        param.getValue()
          .name()
          .text()
      );
    });

    this.imageLCount.setPrefWidth(16.0);
    this.imageLCount.setSortable(true);
    this.imageLCount.setReorderable(false);
    this.imageLCount.setCellValueFactory(param -> {
      return new ReadOnlyObjectWrapper<>(
        Long.valueOf(param.getValue().count())
      );
    });

    this.imageRText.setSortable(true);
    this.imageRText.setReorderable(false);
    this.imageRText.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(
        param.getValue()
          .name()
          .text()
      );
    });

    this.imageRCount.setPrefWidth(16.0);
    this.imageRCount.setSortable(true);
    this.imageRCount.setReorderable(false);
    this.imageRCount.setCellValueFactory(param -> {
      return new ReadOnlyObjectWrapper<>(
        Long.valueOf(param.getValue().count())
      );
    });

    this.compareImageLCaptions.getSelectionModel()
      .getSelectedItems()
      .addListener((ListChangeListener<? super LCaption>) c -> this.onSelectionChanged());
    this.compareImageRCaptions.getSelectionModel()
      .getSelectedItems()
      .addListener((ListChangeListener<? super LCaption>) c -> this.onSelectionChanged());
  }

  private void onSelectionChanged()
  {
    this.captionLeft.setDisable(
      this.compareImageRCaptions.getSelectionModel()
        .getSelectedItems()
        .isEmpty()
    );
    this.captionRight.setDisable(
      this.compareImageLCaptions.getSelectionModel()
        .getSelectedItems()
        .isEmpty()
    );
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
      model.imageComparison()
        .subscribe((oldValue, newValue) -> {
          Platform.runLater(() -> this.onImageComparisonUpdated(newValue));
        })
    );
    subscriptions.add(
      model.imageComparisonA()
        .subscribe((_0, items) -> {
          Platform.runLater(() -> {
            this.compareImageLCaptions.setItems(
              FXCollections.observableList(items)
            );
          });
        })
    );
    subscriptions.add(
      model.imageComparisonB()
        .subscribe((_0, items) -> {
          Platform.runLater(() -> {
            this.compareImageRCaptions.setItems(
              FXCollections.observableList(items)
            );
          });
        })
    );
  }

  private void onImageComparisonUpdated(
    final Optional<LImageComparison> newValue)
  {
    if (newValue.isEmpty()) {
      this.compareImageL.setImage(null);
      this.compareImageR.setImage(null);
      return;
    }

    final var comparison = newValue.get();
    final var imageA = comparison.imageA();
    this.compareImageLName.setText(imageA.image().name());
    final var imageB = comparison.imageB();
    this.compareImageRName.setText(imageB.image().name());

    this.fileModelNow().imageStream(imageA.id())
      .thenAccept(inputStreamOpt -> {
        Platform.runLater(() -> {
          LImages.imageLoad(
            inputStreamOpt,
            this.compareImageLProgress,
            this.compareImageL,
            this.compareImageLError,
            32.0,
            32.0
          );
        });
      });
    this.fileModelNow().imageStream(imageB.id())
      .thenAccept(inputStreamOpt -> {
        Platform.runLater(() -> {
          LImages.imageLoad(
            inputStreamOpt,
            this.compareImageRProgress,
            this.compareImageR,
            this.compareImageRError,
            32.0,
            32.0
          );
        });
      });
  }

  @FXML
  private void onCaptionLeftPressed()
  {
    final var model =
      this.fileModelNow();

    final var comparison =
      model.imageComparison()
        .get()
        .orElseThrow();

    model.imageCaptionsAssign(
      List.of(
        new LImageCaptionsAssignment(
          comparison.imageA().id(),
          this.compareImageRCaptions.getSelectionModel()
            .getSelectedItems()
            .stream()
            .map(LCaption::id)
            .collect(Collectors.toSet()))
      )
    );
  }

  @FXML
  private void onCaptionRightPressed()
  {
    final var model =
      this.fileModelNow();

    final var comparison =
      model.imageComparison()
        .get()
        .orElseThrow();

    model.imageCaptionsAssign(
      List.of(
        new LImageCaptionsAssignment(
          comparison.imageB().id(),
          this.compareImageLCaptions.getSelectionModel()
            .getSelectedItems()
            .stream()
            .map(LCaption::id)
            .collect(Collectors.toSet()))
      )
    );
  }
}
