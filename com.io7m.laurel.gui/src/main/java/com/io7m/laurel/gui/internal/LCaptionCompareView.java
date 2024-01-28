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

import com.io7m.laurel.gui.internal.model.LMCaption;
import com.io7m.laurel.gui.internal.model.LMImage;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Subscription;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * The caption comparison view.
 */

public final class LCaptionCompareView implements LScreenViewType
{
  private final LMImage imageL;
  private final LMImage imageR;
  private final ObservableList<LMCaption> imageCaptionsL;
  private final ObservableList<LMCaption> imageCaptionsR;
  private final SortedList<LMCaption> imageCaptionsLSorted;
  private final SortedList<LMCaption> imageCaptionsRSorted;
  private final Stage stage;
  private final Subscription imageLSubscription;
  private final Subscription imageRSubscription;
  private final LControllerType controller;
  private final LStrings strings;

  @FXML private Button captionLeft;
  @FXML private Button captionRight;
  @FXML private ImageView compareImageL;
  @FXML private ImageView compareImageLError;
  @FXML private ProgressBar compareImageLProgress;
  @FXML private ImageView compareImageR;
  @FXML private ImageView compareImageRError;
  @FXML private ProgressBar compareImageRProgress;
  @FXML private Label compareImageLName;
  @FXML private Label compareImageRName;
  @FXML private TableView<LMCaption> compareImageLCaptions;
  @FXML private TableView<LMCaption> compareImageRCaptions;

  /**
   * The caption comparison view.
   *
   * @param inServices The service directory
   * @param inStage The stage
   * @param inImageL The left image
   * @param inImageR The right image
   */

  public LCaptionCompareView(
    final RPServiceDirectoryType inServices,
    final Stage inStage,
    final LMImage inImageL,
    final LMImage inImageR)
  {
    Objects.requireNonNull(inServices, "inServices");

    this.controller =
      inServices.requireService(LControllerType.class);
    this.strings =
      inServices.requireService(LStrings.class);
    this.stage =
      Objects.requireNonNull(inStage, "inStage");
    this.imageL =
      Objects.requireNonNull(inImageL, "imageL");
    this.imageR =
      Objects.requireNonNull(inImageR, "imageR");

    this.imageCaptionsL =
      FXCollections.observableArrayList();
    this.imageCaptionsR =
      FXCollections.observableArrayList();

    this.imageCaptionsLSorted =
      new SortedList<>(this.imageCaptionsL);
    this.imageCaptionsRSorted =
      new SortedList<>(this.imageCaptionsR);

    this.imageLSubscription =
      this.imageL.captions().subscribe(this::updateCaptions);
    this.imageRSubscription =
      this.imageR.captions().subscribe(this::updateCaptions);

    this.updateCaptions();
  }

  private void updateCaptions()
  {
    this.imageCaptionsL.setAll(
      captionsInANotB(this.imageL, this.imageR)
    );
    this.imageCaptionsR.setAll(
      captionsInANotB(this.imageR, this.imageL)
    );
  }

  private static List<LMCaption> captionsInANotB(
    final LMImage imageA,
    final LMImage imageB)
  {
    return imageA.captions()
      .stream()
      .filter(c -> !imageB.captions().contains(c))
      .sorted()
      .collect(Collectors.toList());
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.captionLeft.setDisable(true);
    this.captionRight.setDisable(true);

    final var fileL =
      this.imageL.fileName().get();
    final var fileR =
      this.imageR.fileName().get();

    this.compareImageLName.setText(fileL.getFileName().toString());
    this.compareImageRName.setText(fileR.getFileName().toString());

    LImages.imageLoad(
      Optional.of(fileL),
      this.compareImageLProgress,
      this.compareImageL,
      this.compareImageLError,
      32.0,
      32.0
    );

    LImages.imageLoad(
      Optional.of(fileR),
      this.compareImageRProgress,
      this.compareImageR,
      this.compareImageRError,
      32.0,
      32.0
    );

    configureCaptionTable(
      this.compareImageLCaptions,
      this.imageCaptionsLSorted,
      this.strings.format(
        LStringConstants.CAPTIONS_COMPARE_NO_EXTRA_CAPTIONS_PRESENT)
    );
    configureCaptionTable(
      this.compareImageRCaptions,
      this.imageCaptionsRSorted,
      this.strings.format(
        LStringConstants.CAPTIONS_COMPARE_NO_EXTRA_CAPTIONS_PRESENT)
    );

    this.compareImageLCaptions.getSelectionModel()
      .getSelectedItems()
      .addListener((ListChangeListener<? super LMCaption>) observable -> {
        this.onCaptionSelectionChangedL();
      });

    this.compareImageRCaptions.getSelectionModel()
      .getSelectedItems()
      .addListener((ListChangeListener<? super LMCaption>) observable -> {
        this.onCaptionSelectionChangedR();
      });
  }

  private void onCaptionSelectionChangedR()
  {
    final var selected =
      this.compareImageRCaptions.getSelectionModel()
        .getSelectedItems();

    /*
     * If captions in the right table are selected, the button to move them
     * left is enabled.
     */

    if (selected.isEmpty()) {
      this.captionLeft.setDisable(true);
    } else {
      this.captionLeft.setDisable(false);
    }
  }

  private void onCaptionSelectionChangedL()
  {
    final var selected =
      this.compareImageLCaptions.getSelectionModel()
        .getSelectedItems();

    /*
     * If captions in the left table are selected, the button to move them
     * right is enabled.
     */

    if (selected.isEmpty()) {
      this.captionRight.setDisable(true);
    } else {
      this.captionRight.setDisable(false);
    }
  }

  private static void configureCaptionTable(
    final TableView<LMCaption> tableView,
    final SortedList<LMCaption> captions,
    final String placeholder)
  {
    tableView.setPlaceholder(new Label(placeholder));
    tableView.setEditable(false);
    tableView.setItems(captions);
    tableView.getSelectionModel()
      .setSelectionMode(SelectionMode.MULTIPLE);

    captions.comparatorProperty()
      .bind(tableView.comparatorProperty());

    final var columns =
      tableView.getColumns();

    final var colText =
      (TableColumn<LMCaption, String>) columns.get(0);
    final var colCount =
      (TableColumn<LMCaption, Long>) columns.get(1);

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

    tableView.getSortOrder().add(colText);
    tableView.sort();
  }

  @FXML
  private void onCaptionLeftPressed()
  {
    this.controller.imageCaptionAssign(
      List.of(this.imageL.id()),
      List.copyOf(
        this.compareImageRCaptions.getSelectionModel()
          .getSelectedItems()
          .stream()
          .map(LMCaption::id)
          .collect(Collectors.toList())
      )
    );
  }

  @FXML
  private void onCaptionRightPressed()
  {
    this.controller.imageCaptionAssign(
      List.of(this.imageR.id()),
      List.copyOf(
        this.compareImageLCaptions.getSelectionModel()
          .getSelectedItems()
          .stream()
          .map(LMCaption::id)
          .collect(Collectors.toList())
      )
    );
  }
}
