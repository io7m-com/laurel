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

import com.io7m.laurel.filemodel.LFileModelEventType;
import com.io7m.laurel.filemodel.LFileModelType;
import com.io7m.laurel.filemodel.LFileModels;
import com.io7m.laurel.gui.internal.LPerpetualSubscriber;
import com.io7m.laurel.model.LException;
import com.io7m.laurel.model.LImageWithID;
import com.io7m.zelador.test_extension.CloseableResourcesType;
import com.io7m.zelador.test_extension.ZeladorExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({ZeladorExtension.class})
public final class LFileModelImportTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LFileModelTest.class);

  private Path directory;
  private Path outputFile;
  private ConcurrentLinkedQueue<LFileModelEventType> events;

  @BeforeEach
  public void setup(
    final @TempDir Path directory)
  {
    this.directory = directory;
    this.outputFile = directory.resolve("out.db");
    this.events = new ConcurrentLinkedQueue<>();
  }

  @Test
  public void testImportDatasetGood(
    final CloseableResourcesType resources)
    throws Exception
  {
    final var tags =
      openTags("dataset_good.txt");
    final var inputPath =
      this.unpack("dataset_good.zip", "x");

    try (var importer = LFileModels.createImport(inputPath, this.outputFile)) {
      importer.events().subscribe(new LPerpetualSubscriber<>(this::addEvent));
      importer.execute().get(1L, TimeUnit.MINUTES);
    }

    try (var model =
           resources.addPerTestResource(LFileModels.open(
             this.outputFile,
             false))) {
      model.loading().get(1L, TimeUnit.MINUTES);

      final var captions =
        model.captionList()
          .get()
          .stream()
          .map(x -> x.name().text())
          .collect(Collectors.toSet());

      final var images =
        model.imageList()
          .get()
          .stream()
          .map(LImageWithID::image)
          .collect(Collectors.toSet());

      assertEquals(
        Set.of(
          "1boy",
          "male focus",
          "outdoors",
          "horse",
          "pants",
          "tree",
          "solo",
          "shirt",
          "hat",
          "photo background",
          "white shirt",
          "jewelry"
        ),
        tagsFor(model, "X_00001_.png")
      );
      assertEquals(
        Set.of(
          "ground vehicle",
          "no humans",
          "grass",
          "traditional media",
          "bicycle",
          "motorcycle",
          "motor vehicle",
          "outdoors",
          "sky",
          "tree",
          "cloud",
          "day"
        ),
        tagsFor(model, "X_00002_.png")
      );
      assertEquals(
        Set.of(
          "no humans",
          "sky",
          "traditional media",
          "food",
          "fruit",
          "outdoors",
          "day"
        ),
        tagsFor(model, "X_00003_.png")
      );
      assertEquals(
        Set.of(
          "monochrome",
          "greyscale",
          "no humans",
          "car",
          "ground vehicle",
          "scenery",
          "motor vehicle",
          "building",
          "window",
          "real world location"
        ),
        tagsFor(model, "X_00005_.png")
      );
      assertEquals(
        Set.of(
          "animal",
          "grass",
          "outdoors",
          "oversized animal",
          "cat"
        ),
        tagsFor(model, "X_00006_.png")
      );

      assertEquals(tags, captions);
      assertEquals(5, images.size());
    }
  }

  private static Set<String> tagsFor(
    final LFileModelType model,
    final String image)
    throws Exception
  {
    final var images =
      model.imageList().get();

    final var imageID =
      Optional.of(
        images.stream()
          .filter(x -> x.image().name().endsWith(image))
          .findFirst()
          .orElseThrow()
          .id()
      );

    model.imageSelect(imageID)
      .get(1L, TimeUnit.SECONDS);

    return model.imageCaptionsAssigned()
      .get()
      .stream()
      .map(x -> x.name().text())
      .collect(Collectors.toSet());
  }

  @Test
  public void testImportDatasetCorruptCaption(
    final CloseableResourcesType resources)
    throws Exception
  {
    final var inputPath =
      this.unpack("dataset_corrupt_caption.zip", "x");

    try (var importer =
           resources.addPerTestResource(LFileModels.createImport(
             inputPath,
             this.outputFile))) {
      importer.events().subscribe(new LPerpetualSubscriber<>(this::addEvent));

      final var ex = assertThrows(LException.class, () -> {
        try {
          importer.execute().get(1L, TimeUnit.MINUTES);
        } catch (final Throwable e) {
          throw e.getCause();
        }
      });
      assertEquals("error-import", ex.errorCode());

      assertTrue(
        this.events.stream()
          .anyMatch(e -> e.message().contains("Caption must match"))
      );
    }
  }

  private void addEvent(
    final LFileModelEventType e)
  {
    LOG.debug("Event: {}", e);
    this.events.add(e);
  }

  private static Set<String> openTags(
    final String name)
    throws Exception
  {
    final var filePath =
      "/com/io7m/laurel/tests/%s".formatted(name);

    try (var stream =
           LFileModelImportTest.class.getResourceAsStream(filePath)) {

      final var reader =
        new InputStreamReader(stream, StandardCharsets.UTF_8);
      try (var bufferedReader = new BufferedReader(reader)) {
        return bufferedReader.lines().collect(Collectors.toSet());
      }
    }
  }

  private Path unpack(
    final String zipName,
    final String outputName)
    throws IOException
  {
    final var outputDirectory =
      this.directory.resolve(outputName);

    Files.createDirectories(outputDirectory);

    final var zipPath =
      "/com/io7m/laurel/tests/%s".formatted(zipName);

    try (var zipStream =
           LFileModelImportTest.class.getResourceAsStream(zipPath)) {

      try (var zipInputStream = new ZipInputStream(zipStream)) {
        while (true) {
          final var entry = zipInputStream.getNextEntry();
          if (entry == null) {
            break;
          }

          final var outputFile =
            outputDirectory.resolve(entry.getName());

          LOG.debug("Copy {} -> {}", entry.getName(), outputFile);
          Files.copy(zipInputStream, outputFile);
        }
      }
    }

    return outputDirectory;
  }
}
