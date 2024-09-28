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
import com.io7m.laurel.model.LImageWithID;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Optional;

/**
 * A single image view.
 */

public final class LImageView extends LAbstractViewWithModel
{
  private final Stage stage;
  private @FXML ImageView imageView;
  private @FXML Button dismiss;
  private @FXML ProgressBar imageProgress;
  private @FXML Parent errorImageLoad;
  private @FXML StackPane imageContainer;

  /**
   * The image view screen.
   *
   * @param inFileModel The file model
   * @param inStage     The stage
   */

  public LImageView(
    final LFileModelScope inFileModel,
    final Stage inStage)
  {
    super(inFileModel);

    this.stage =
      Objects.requireNonNull(inStage, "inStage");
  }

  @Override
  protected void onInitialize()
  {
    this.imageView.fitWidthProperty()
      .bind(this.imageContainer.widthProperty());
    this.imageView.fitHeightProperty()
      .bind(this.imageContainer.heightProperty());
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
      fileModel.imageSelected()
        .subscribe((oldValue, newValue) -> {
          Platform.runLater(() -> {
            this.onImageSelected(newValue);
          });
        })
    );
  }

  private void onImageSelected(
    final Optional<LImageWithID> image)
  {
    final var fileModel = this.fileModelNow();
    if (image.isEmpty()) {
      this.imageView.setImage(null);
      this.imageProgress.setVisible(false);
      this.errorImageLoad.setVisible(false);
      return;
    }

    fileModel.imageStream(image.get().id())
      .thenAccept(inputStreamOpt -> {
        Platform.runLater(() -> {
          LImages.imageLoad(
            inputStreamOpt,
            this.imageProgress,
            this.imageView,
            this.errorImageLoad,
            this.imageView.getFitWidth(),
            this.imageView.getFitHeight()
          );
        });
      });
  }

  @FXML
  private void onDismiss()
  {
    this.stage.close();
  }

  /**
   * The image view screen.
   *
   * @param stage       The stage
   * @param inFileModel The file model
   * @param strings     The strings
   *
   * @return The image view screen
   */

  public static LImageView create(
    final Stage stage,
    final LFileModelScope inFileModel,
    final LStrings strings)
  {
    try {
      final var layout =
        LImageView.class.getResource(
          "/com/io7m/laurel/gui/internal/image.fxml");

      Objects.requireNonNull(layout, "layout");

      final var loader =
        new FXMLLoader(layout, strings.resources());

      final var image = new LImageView(inFileModel, stage);
      loader.setControllerFactory(param -> {
        return image;
      });

      final Pane pane = loader.load();
      LCSS.setCSS(pane);

      stage.setTitle(strings.format("image"));
      stage.setMinWidth(512.0);
      stage.setMinHeight(512.0 + 64.0);
      stage.setWidth(512.0);
      stage.setHeight(512.0 + 64.0);
      stage.setScene(new Scene(pane));
      return image;
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
