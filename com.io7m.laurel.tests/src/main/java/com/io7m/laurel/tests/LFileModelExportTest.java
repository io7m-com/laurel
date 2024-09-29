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

import com.io7m.laurel.filemodel.LExportRequest;
import com.io7m.laurel.filemodel.LFileModelEventType;
import com.io7m.laurel.filemodel.LFileModelStatusIdle;
import com.io7m.laurel.filemodel.LFileModelStatusLoading;
import com.io7m.laurel.filemodel.LFileModels;
import com.io7m.laurel.filemodel.internal.LCaptionFiles;
import com.io7m.laurel.gui.internal.LPerpetualSubscriber;
import com.io7m.laurel.model.LCaptionName;
import com.io7m.zelador.test_extension.CloseableResourcesType;
import com.io7m.zelador.test_extension.ZeladorExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({ZeladorExtension.class})
public final class LFileModelExportTest
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
  public void testExportDatasetGood(
    final CloseableResourcesType resources)
    throws Exception
  {
    final var outputPath =
      this.directory.resolve("export");
    final var inputPath =
      this.unpack("dataset_good.zip", "x");

    try (var importer =
           resources.addPerTestResource(LFileModels.createImport(
             inputPath,
             this.outputFile))) {
      importer.events().subscribe(new LPerpetualSubscriber<>(this::addEvent));
      importer.execute().get(1L, TimeUnit.MINUTES);
    }

    try (var model =
           resources.addPerTestResource(LFileModels.open(
             this.outputFile,
             false))) {
      model.loading().get(1L, TimeUnit.MINUTES);

      model.export(
          new LExportRequest(outputPath, true))
        .get(1L, TimeUnit.MINUTES);

      for (final var image : model.imageList().get()) {
        final var imageId =
          image.id().value();
        final var imageFile =
          outputPath.resolve("0000000000000000000%d.png".formatted(imageId));
        final var captionFile =
          outputPath.resolve("0000000000000000000%d.caption".formatted(imageId));

        assertTrue(Files.isRegularFile(imageFile));
        assertTrue(Files.isRegularFile(captionFile));

        final var m = new HashMap<String, Object>();

        switch ((int) imageId) {
          case 1 -> {
            assertEquals(
              List.of(
                new LCaptionName("1boy"),
                new LCaptionName("hat"),
                new LCaptionName("horse"),
                new LCaptionName("jewelry"),
                new LCaptionName("male focus"),
                new LCaptionName("outdoors"),
                new LCaptionName("pants"),
                new LCaptionName("photo background"),
                new LCaptionName("shirt"),
                new LCaptionName("solo"),
                new LCaptionName("tree"),
                new LCaptionName("white shirt")
              ),
              LCaptionFiles.parse(m, captionFile),
              () -> "File %s must have the expected content"
                .formatted(captionFile)
            );
          }
          case 2 -> {
            assertEquals(
              List.of(
                new LCaptionName("bicycle"),
                new LCaptionName("cloud"),
                new LCaptionName("day"),
                new LCaptionName("grass"),
                new LCaptionName("ground vehicle"),
                new LCaptionName("motor vehicle"),
                new LCaptionName("motorcycle"),
                new LCaptionName("no humans"),
                new LCaptionName("outdoors"),
                new LCaptionName("sky"),
                new LCaptionName("traditional media"),
                new LCaptionName("tree")
              ),
              LCaptionFiles.parse(m, captionFile),
              () -> "File %s must have the expected content"
                .formatted(captionFile)
            );
          }
          case 3 -> {
            assertEquals(
              List.of(
                new LCaptionName("day"),
                new LCaptionName("food"),
                new LCaptionName("fruit"),
                new LCaptionName("no humans"),
                new LCaptionName("outdoors"),
                new LCaptionName("sky"),
                new LCaptionName("traditional media")
              ),
              LCaptionFiles.parse(m, captionFile),
              () -> "File %s must have the expected content"
                .formatted(captionFile)
            );
          }
          case 4 -> {
            assertEquals(
              List.of(
                new LCaptionName("building"),
                new LCaptionName("car"),
                new LCaptionName("greyscale"),
                new LCaptionName("ground vehicle"),
                new LCaptionName("monochrome"),
                new LCaptionName("motor vehicle"),
                new LCaptionName("no humans"),
                new LCaptionName("real world location"),
                new LCaptionName("scenery"),
                new LCaptionName("window")
              ),
              LCaptionFiles.parse(m, captionFile),
              () -> "File %s must have the expected content"
                .formatted(captionFile)
            );
          }
          case 5 -> {
            assertEquals(
              List.of(
                new LCaptionName("animal"),
                new LCaptionName("cat"),
                new LCaptionName("grass"),
                new LCaptionName("outdoors"),
                new LCaptionName("oversized animal")
              ),
              LCaptionFiles.parse(m, captionFile),
              () -> "File %s must have the expected content"
                .formatted(captionFile)
            );
          }
        }
      }
    }
  }

  private void addEvent(
    final LFileModelEventType e)
  {
    LOG.debug("Event: {}", e);
    this.events.add(e);
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
           LFileModelExportTest.class.getResourceAsStream(zipPath)) {

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
