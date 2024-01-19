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


package com.io7m.laurel.io;

import com.io7m.laurel.model.LImageCaption;
import com.io7m.laurel.model.LImageSetReadableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Functions to export image sets.
 */

public final class LExporters
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LExporters.class);

  private LExporters()
  {

  }

  /**
   * Export the given image set.
   *
   * @param request         The request
   * @param sourceDirectory The directory against which image names are relative
   * @param imageSet        The image set
   *
   * @throws IOException On I/O errors
   */

  public static void export(
    final LExportRequest request,
    final Path sourceDirectory,
    final LImageSetReadableType imageSet)
    throws IOException
  {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(sourceDirectory, "sourceDirectory");
    Objects.requireNonNull(imageSet, "imageSet");

    if (request.includeImages()) {
      exportImages(request, sourceDirectory, imageSet);
    }
    exportCaptions(request, imageSet);
  }

  private static void exportCaptions(
    final LExportRequest request,
    final LImageSetReadableType imageSet)
    throws IOException
  {
    for (final var image : imageSet.images().values()) {
      final var outputFile =
        request.outputDirectory().resolve(image.fileName() + ".caption");
      final var outputFileTmp =
        request.outputDirectory().resolve(image.fileName() + ".caption.tmp");

      LOG.info("Write {} -> {}", outputFileTmp, outputFile);

      final var captions =
        imageSet.captionsForImage(image.imageID());

      final var text =
        captions.stream()
          .map(LImageCaption::text)
          .collect(Collectors.joining(",\n"))
        + '\n';

      Files.writeString(
        outputFileTmp,
        text,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE
      );
      Files.move(
        outputFileTmp,
        outputFile,
        StandardCopyOption.ATOMIC_MOVE,
        StandardCopyOption.REPLACE_EXISTING
      );
    }
  }

  private static void exportImages(
    final LExportRequest request,
    final Path sourceDirectory,
    final LImageSetReadableType imageSet)
    throws IOException
  {
    for (final var image : imageSet.images().values()) {
      final var inputFile =
        sourceDirectory.resolve(image.fileName());
      final var outputFile =
        request.outputDirectory().resolve(image.fileName());
      final var outputFileTmp =
        request.outputDirectory().resolve(image.fileName() + ".tmp");

      LOG.info("Write {} -> {}", outputFileTmp, outputFile);

      Files.copy(
        inputFile,
        outputFileTmp,
        StandardCopyOption.REPLACE_EXISTING
      );
      Files.move(
        outputFileTmp,
        outputFile,
        StandardCopyOption.ATOMIC_MOVE,
        StandardCopyOption.REPLACE_EXISTING
      );
    }
  }
}
