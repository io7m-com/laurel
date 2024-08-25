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

import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.laurel.model.LOldImage;
import com.io7m.laurel.model.LImageCaption;
import com.io7m.laurel.model.LImageCaptionID;
import com.io7m.laurel.model.LImageID;
import com.io7m.laurel.model.LImageSet;
import com.io7m.seltzer.api.SStructuredError;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Functions to export image sets.
 */

public final class LImporters
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LImporters.class);

  private LImporters()
  {

  }

  /**
   * Import a captioned image set.
   *
   * @param sourceDirectory The directory to import
   * @param errorConsumer   The error consumer
   *
   * @return A captioned image set
   *
   * @throws Exception On I/O errors
   */

  public static LImageSet importDirectory(
    final Path sourceDirectory,
    final Consumer<SStructuredError<String>> errorConsumer)
    throws Exception
  {
    Objects.requireNonNull(sourceDirectory, "sourceDirectory");
    Objects.requireNonNull(errorConsumer, "errorConsumer");

    final var exceptionTracker =
      new ExceptionTracker<Exception>();

    final var images =
      new TreeMap<LImageID, LOldImage>();
    final var captions =
      new TreeMap<LImageCaptionID, LImageCaption>();
    final var captionTexts =
      new TreeMap<String, LImageCaptionID>();

    int index = 0;
    try (var files = Files.list(sourceDirectory)) {
      final var fileList = files.sorted().toList();
      for (final var file : fileList) {
        if (isImage(file)) {
          final var image =
            processImageFile(
              exceptionTracker,
              index,
              sourceDirectory,
              file,
              captionTexts,
              captions,
              errorConsumer
            );
          images.put(image.imageID(), image);
        }
        ++index;
      }
    } catch (final Exception e) {
      errorConsumer.accept(
        new SStructuredError<>(
          "error",
          e.getMessage(),
          Map.of(),
          Optional.empty(),
          Optional.of(e)
        )
      );
      exceptionTracker.addException(e);
    }

    exceptionTracker.throwIfNecessary();
    return new LImageSet(List.of(), captions, images);
  }

  private static LOldImage processImageFile(
    final ExceptionTracker<Exception> exceptionTracker,
    final int fileIndex,
    final Path sourceDirectory,
    final Path file,
    final TreeMap<String, LImageCaptionID> captionTexts,
    final TreeMap<LImageCaptionID, LImageCaption> captions,
    final Consumer<SStructuredError<String>> errorConsumer)
    throws IOException
  {
    LOG.info("Processing image file: {}", file);

    try {
      final var rawName =
        FilenameUtils.removeExtension(file.getFileName().toString());
      final var captionName =
        "%s.caption".formatted(rawName);
      final var captionFile =
        file.resolveSibling(captionName);

      LOG.info("Processing caption file: {}", captionFile);

      final var captionLines =
        readCaptions(captionFile);

      final var imageCaptions =
        new TreeSet<LImageCaptionID>();

      int captionIndex = 0;
      for (final var line : captionLines) {
        final var existingId = captionTexts.get(line);
        try {
          final LImageCaption caption;
          if (existingId == null) {
            caption = new LImageCaption(
              new LImageCaptionID(
                UUID.nameUUIDFromBytes(line.getBytes(StandardCharsets.UTF_8))
              ),
              line
            );
          } else {
            caption = captions.get(existingId);
          }
          captions.put(caption.id(), caption);
          captionTexts.put(line, caption.id());
          imageCaptions.add(caption.id());
        } catch (final Exception e) {
          errorConsumer.accept(
            new SStructuredError<>(
              "error-caption",
              e.getMessage(),
              Map.ofEntries(
                Map.entry(
                  "Caption[%d,%d]".formatted(
                  Integer.valueOf(fileIndex),
                  Integer.valueOf(captionIndex)),
                  line
                ),
                Map.entry(
                  "File[%d]".formatted(
                    Integer.valueOf(fileIndex)),
                  file.toString()
                )
              ),
              Optional.empty(),
              Optional.of(e)
            )
          );
          exceptionTracker.addException(e);
        }
        ++captionIndex;
      }

      return new LOldImage(
        new LImageID(
          UUID.nameUUIDFromBytes(
            file.toAbsolutePath()
              .toString()
              .getBytes(StandardCharsets.UTF_8)
          )
        ),
        sourceDirectory.relativize(file).toString(),
        imageCaptions
      );

    } catch (final Exception e) {
      errorConsumer.accept(
        new SStructuredError<>(
          "error-file",
          e.getMessage(),
          Map.ofEntries(
            Map.entry("File[%d]".formatted(Integer.valueOf(fileIndex)), file.toString())
          ),
          Optional.empty(),
          Optional.of(e)
        )
      );
      throw e;
    }
  }

  private static List<String> readCaptions(
    final Path file)
    throws IOException
  {
    try (var stream = Files.lines(file, StandardCharsets.UTF_8)) {
      return stream.map(x -> x.replace(",", ""))
        .map(String::trim)
        .filter(x -> !x.isEmpty())
        .sorted()
        .collect(Collectors.toList());
    }
  }

  private static boolean isImage(
    final Path file)
  {
    final var ext =
      FilenameUtils.getExtension(file.toAbsolutePath().toString())
        .toUpperCase(Locale.ROOT);

    return switch (ext) {
      case "PNG" -> true;
      case "JPG" -> true;
      case "JPEG" -> true;
      default -> false;
    };
  }

  private static void exportCaptions(
    final LExportRequest request,
    final LImageSet imageSet)
    throws IOException
  {
    for (final var image : imageSet.images().values()) {
      final var outputFile =
        request.outputDirectory().resolve(image.fileName() + ".caption");
      final var outputFileTmp =
        request.outputDirectory().resolve(image.fileName() + ".caption.tmp");

      LOG.info("Write {} -> {}", outputFileTmp, outputFile);

      final SortedSet<LImageCaption> captions =
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
    final LImageSet imageSet)
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
