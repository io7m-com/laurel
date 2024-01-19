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


package com.io7m.laurel.tests;

import com.io7m.laurel.io.LExportRequest;
import com.io7m.laurel.io.LExporters;
import com.io7m.laurel.io.LSerializers;
import com.io7m.laurel.model.LImage;
import com.io7m.laurel.model.LImageCaption;
import com.io7m.laurel.model.LImageCaptionID;
import com.io7m.laurel.model.LImageID;
import com.io7m.laurel.model.LImageSetCommandException;
import com.io7m.laurel.model.LImageSetType;
import com.io7m.laurel.model.LImageSets;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class LDemo
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LDemo.class);

  private static final LSerializers SERIALIZERS =
    new LSerializers();

  private LDemo()
  {

  }

  public static void main(
    final String[] args)
    throws Exception
  {
    final var directory =
      Paths.get(args[0]);

    final var output =
      directory.resolve("Captions.xml");

    final var imageSet =
      LImageSets.empty();

    try (var files = Files.list(directory)) {
      final var allImages =
        files.filter(LDemo::isImage)
          .toList();

      for (final var imageFile : allImages) {
        processImageFile(imageSet, imageFile);
      }
    }

    SERIALIZERS.serializeFile(output, imageSet);
  }

  private static void processImageFile(
    final LImageSetType imageSet,
    final Path imageFile)
    throws Exception
  {
    final var noExtension =
      FilenameUtils.removeExtension(imageFile.toString());
    final var captionFile =
      Paths.get(noExtension + ".caption");

    LOG.debug("Image: {}", imageFile);
    LOG.debug("Caption File: {}", captionFile);

    final var imageId = new LImageID(UUID.randomUUID());
    imageSet.imageUpdate(
      new LImage(
        imageId,
        imageFile.getFileName().toString(),
        List.of()
      )
    ).execute();

    try (var lineStream = Files.lines(captionFile)) {
      final var lines =
        lineStream
          .map(x -> x.replace(",", ""))
          .map(String::trim)
          .toList();

      for (final var line : lines) {
        if (line.isEmpty()) {
          continue;
        }

        LOG.debug("Caption: {}", line);
        final var existing = imageSet.captionForText(line);
        if (existing.isPresent()) {
          imageSet.captionAssign(imageId, existing.get()).execute();
        } else {
          final var caption =
            new LImageCaptionID(
              UUID.nameUUIDFromBytes(line.getBytes(StandardCharsets.UTF_8))
            );
          imageSet.captionUpdate(new LImageCaption(caption, line)).execute();
          imageSet.captionAssign(imageId, caption).execute();
        }
      }
    }
  }

  private static boolean isImage(
    final Path p)
  {
    return switch (FilenameUtils.getExtension(p.toString()).toUpperCase(Locale.ROOT)) {
      case "PNG" -> true;
      case "JPG" -> true;
      case "JPEG" -> true;
      default -> false;
    };
  }
}
