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

import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Functions to configure image views.
 */

public final class LImages
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LImages.class);

  private LImages()
  {

  }

  /**
   * Configure an image view for the given file.
   *
   * @param fileOpt       The file
   * @param imageProgress The progress indicator
   * @param imageView     The image view
   * @param imageError    The error indicator shown on failures
   * @param width         The expected width
   * @param height        The expected height
   */

  public static void imageLoad(
    final Optional<Path> fileOpt,
    final ProgressBar imageProgress,
    final ImageView imageView,
    final Node imageError,
    final double width,
    final double height)
  {
    imageProgress.setVisible(true);
    if (fileOpt.isPresent()) {
      final var imageValue =
        new Image(
          fileOpt.get().toUri().toString(),
          width,
          height,
          true,
          true,
          true
        );

      imageProgress.setVisible(true);
      imageValue.exceptionProperty()
        .subscribe(e -> {
          if (e != null) {
            LOG.error("Image load: ", e);
            imageError.setVisible(true);
          }
          imageProgress.setVisible(true);
        });

      imageValue.progressProperty()
        .addListener((observable, oldValue, newValue) -> {
          if (newValue.doubleValue() >= 1.0) {
            imageProgress.setVisible(false);
          }
        });

      imageProgress.progressProperty()
        .bind(imageValue.progressProperty());

      imageView.setImage(imageValue);
      imageError.setVisible(false);
    } else {
      imageView.setImage(null);
      imageProgress.setVisible(false);
      imageError.setVisible(false);
    }
  }
}
