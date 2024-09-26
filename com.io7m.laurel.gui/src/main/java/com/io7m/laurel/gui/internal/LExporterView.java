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
import com.io7m.jwheatsheaf.oxygen.JWOxygenIconSet;
import com.io7m.laurel.filemodel.LFileModelType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * The exporter view.
 */

public final class LExporterView extends LAbstractViewWithModel
{
  private final Stage stage;
  private final LPreferencesType preferences;
  private Optional<?> result;
  private final LExporterDialogs dialogs;
  private final LFileChoosers fileChoosers;

  @FXML private Button cancel;
  @FXML private Button export;
  @FXML private TextField directoryField;
  @FXML private CheckBox includeImages;

  /**
   * The exporter view.
   *
   * @param inDialogs  The dialogs
   * @param fileModel  The file model
   * @param inServices The service directory
   * @param inStage    The stage
   */

  public LExporterView(
    final LExporterDialogs inDialogs,
    final RPServiceDirectoryType inServices,
    final LFileModelScope fileModel,
    final Stage inStage)
  {
    super(fileModel);

    this.dialogs =
      Objects.requireNonNull(inDialogs, "dialogs");
    this.fileChoosers =
      inServices.requireService(LFileChoosers.class);
    this.preferences =
      inServices.requireService(LPreferencesType.class);
    this.stage =
      Objects.requireNonNull(inStage, "stage");
    this.result =
      Optional.empty();
  }

  /**
   * @return The resulting request, if any
   */

  public Optional<?> result()
  {
    return this.result;
  }

  @Override
  protected void onInitialize()
  {
    this.export.setDisable(true);

    this.dialogs.directoryProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.updateDirectoryField(newValue);
      });

    this.includeImages.selectedProperty()
      .bindBidirectional(this.dialogs.recentIncludeImagesProperty());

    this.updateDirectoryField(
      this.dialogs.directoryProperty().get()
    );

    this.cancel.requestFocus();
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

  }

  private void updateDirectoryField(
    final Path newValue)
  {
    if (newValue == null) {
      this.directoryField.setText("");
      this.export.setDisable(true);
    } else {
      this.preferences.addRecentFile(newValue);
      this.directoryField.setText(newValue.toString());
      this.export.setDisable(false);
    }
  }

  @FXML
  private void onSelect()
    throws Exception
  {
    final var fileChooser =
      this.fileChoosers.create(
        JWFileChooserConfiguration.builder()
          .setModality(Modality.APPLICATION_MODAL)
          .setAction(JWFileChooserAction.OPEN_EXISTING_SINGLE)
          .setRecentFiles(this.preferences.recentFiles())
          .setCssStylesheet(LCSS.defaultCSS().toURL())
          .setFileImageSet(new JWOxygenIconSet())
          .build()
      );

    final var file = fileChooser.showAndWait();
    if (file.isEmpty()) {
      return;
    }

    this.dialogs.directoryProperty()
      .set(file.getFirst());
  }

  @FXML
  private void onCancel()
  {
    this.result = Optional.empty();
    this.stage.close();
  }

  @FXML
  private void onExport()
  {
    this.stage.close();
  }
}
