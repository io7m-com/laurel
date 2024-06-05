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
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * The merge view screen.
 */

public final class LMergeView
  implements LScreenViewType
{
  private final Stage stage;
  private final LControllerType controller;
  private final ObservableList<Path> files;
  private final LFileChoosers choosers;
  private final LPreferencesType preferences;

  @FXML private ListView<Path> filesView;
  @FXML private TextArea textArea;
  @FXML private Button removeSelected;
  @FXML private Button mergeSelected;

  /**
   * The merge view screen.
   *
   * @param services The services
   * @param inStage  The stage
   */

  public LMergeView(
    final RPServiceDirectoryType services,
    final Stage inStage)
  {
    this.controller =
      services.requireService(LControllerType.class);
    this.choosers =
      services.requireService(LFileChoosers.class);
    this.preferences =
      services.requireService(LPreferencesType.class);
    this.stage =
      Objects.requireNonNull(inStage, "inStage");
    this.files =
      FXCollections.observableArrayList();
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.filesView.setItems(this.files);
    this.filesView.getSelectionModel()
      .selectedItemProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.onSelectionChanged();
      });

    this.files.addListener((ListChangeListener<? super Path>) observable -> {
      this.onListChanged();
    });
  }

  private void onListChanged()
  {
    if (this.files.isEmpty()) {
      this.mergeSelected.setDisable(true);
    } else {
      this.mergeSelected.setDisable(false);
    }
  }

  private void onSelectionChanged()
  {
    final var selectionModel =
      this.filesView.getSelectionModel();

    if (selectionModel.getSelectedItems().isEmpty()) {
      this.removeSelected.setDisable(true);
    } else {
      this.removeSelected.setDisable(false);
    }
  }

  @FXML
  private void onAddSelected()
    throws Exception
  {
    final var fileChooser =
      this.choosers.fileChoosers()
        .create(
          JWFileChooserConfiguration.builder()
            .setModality(Modality.APPLICATION_MODAL)
            .setAction(JWFileChooserAction.OPEN_EXISTING_MULTIPLE)
            .setRecentFiles(this.preferences.recentFiles())
            .setCssStylesheet(LCSS.defaultCSS().toURL())
            .setFileImageSet(new JWOxygenIconSet())
            .build()
        );

    this.files.addAll(fileChooser.showAndWait());
  }

  @FXML
  private void onRemoveSelected()
  {
    final var selectionModel =
      this.filesView.getSelectionModel();

    this.files.removeAll(selectionModel.getSelectedItems());
  }

  @FXML
  private void onMergeSelected()
  {
    this.controller.merge(List.copyOf(this.files));
  }

  @FXML
  private void onCancelSelected()
  {

  }
}
