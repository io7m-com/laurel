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

import com.io7m.laurel.gui.internal.model.LMImage;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * The About screen.
 */

public final class LImageView implements LScreenViewType
{
  private final Stage stage;
  private final LControllerType controller;

  private @FXML Rectangle border;
  private @FXML ImageView imageView;
  private @FXML Button dismiss;
  private @FXML ProgressBar imageProgress;

  /**
   * The image view screen.
   *
   * @param services The services
   * @param inStage  The stage
   */

  public LImageView(
    final RPServiceDirectoryType services,
    final Stage inStage)
  {
    this.controller =
      services.requireService(LControllerType.class);
    this.stage =
      Objects.requireNonNull(inStage, "inStage");
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.border.widthProperty()
      .bind(this.stage.widthProperty().subtract(24.0));
    this.border.heightProperty()
      .bind(this.stage.heightProperty().subtract(32 + 32 + 16 + 8));

    this.border.widthProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.imageView.setFitWidth(newValue.doubleValue() - 2.0);
      });
    this.border.heightProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.imageView.setFitHeight(newValue.doubleValue() - 2.0);
      });

    this.controller.imageSelected()
      .subscribe((image1, image2) -> {
        this.onImageSelected(image2);
      });
  }

  private void onImageSelected(
    final LMImage image)
  {
    if (image == null) {
      this.controller.imageSelect(Optional.empty());
      this.imageView.setImage(null);
      return;
    }

    final var imageFileOpt =
      Optional.ofNullable(image.fileName().get());

    this.imageProgress.setVisible(true);
    if (imageFileOpt.isPresent()) {
      final var imageValue =
        new Image(
          imageFileOpt.get().toUri().toString(),
          this.imageView.getFitWidth(),
          this.imageView.getFitHeight(),
          false,
          true,
          true
        );

      this.imageProgress.setVisible(true);

      imageValue.exceptionProperty()
        .subscribe(e -> {
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
    } else {
      this.imageView.setImage(null);
      this.imageProgress.setVisible(false);
    }
  }

  @FXML
  private void onDismiss()
  {
    this.stage.close();
  }

  /**
   * The image view screen.
   *
   * @param stage    The stage
   * @param services The services
   * @param strings  The strings
   *
   * @return The about screen
   */

  public static LImageView create(
    final Stage stage,
    final RPServiceDirectoryType services,
    final LStrings strings)
  {
    try {
      final var layout =
        LExporterDialogs.class.getResource(
          "/com/io7m/laurel/gui/internal/image.fxml");

      Objects.requireNonNull(layout, "layout");

      final var loader =
        new FXMLLoader(layout, strings.resources());

      final var image = new LImageView(services, stage);
      loader.setControllerFactory(param -> {
        return image;
      });

      final Pane pane = loader.load();
      LCSS.setCSS(pane);

      stage.setTitle(strings.format("image"));
      stage.setWidth(512);
      stage.setMinWidth(256);
      stage.setMinHeight(256);
      stage.setHeight(512);
      stage.setScene(new Scene(pane));
      return image;
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
