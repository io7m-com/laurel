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
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({ZeladorExtension.class})
public final class LFileModelExportTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LFileModelTest.class);

  private static final long TIMEOUT = 10L;

  private static final OpenOption[] OPEN_OPTIONS = {
    StandardOpenOption.WRITE,
    StandardOpenOption.CREATE,
    StandardOpenOption.TRUNCATE_EXISTING
  };

  private Path directory;
  private Path outputFile;
  private CloseableResourcesType resources;
  private ConcurrentLinkedQueue<LFileModelEventType> events;

  @BeforeEach
  public void setup(
    final @TempDir Path directory,
    final CloseableResourcesType resources)
  {
    this.directory = directory;
    this.outputFile = directory.resolve("out.db");
    this.resources = resources;
    this.events = new ConcurrentLinkedQueue<>();
  }

  @AfterEach
  public void tearDown()
  {

  }

  @Test
  public void testExportDatasetGood()
    throws Exception
  {
    final var outputPath =
      this.directory.resolve("export");
    final var inputPath =
      this.unpack("dataset_good.zip", "x");

    try (var importer = LFileModels.createImport(inputPath, this.outputFile)) {
      importer.events().subscribe(new LPerpetualSubscriber<>(this::addEvent));
      importer.execute().get(1L, TimeUnit.MINUTES);
    }

    try (var model = LFileModels.open(this.outputFile, false)) {
      Thread.sleep(1_000L);

      model.export(
          new LExportRequest(outputPath, true))
        .get(1L, TimeUnit.MINUTES);

      final var files = Files.list(outputPath).toList();
      assertTrue(Files.isRegularFile(outputPath.resolve(
        "00000000000000000001.png")));
      assertTrue(Files.isRegularFile(outputPath.resolve(
        "00000000000000000002.png")));
      assertTrue(Files.isRegularFile(outputPath.resolve(
        "00000000000000000003.png")));
      assertTrue(Files.isRegularFile(outputPath.resolve(
        "00000000000000000004.png")));
      assertTrue(Files.isRegularFile(outputPath.resolve(
        "00000000000000000005.png")));

      final var cap1 = outputPath.resolve("00000000000000000001.caption");
      final var cap2 = outputPath.resolve("00000000000000000002.caption");
      final var cap3 = outputPath.resolve("00000000000000000003.caption");
      final var cap4 = outputPath.resolve("00000000000000000004.caption");
      final var cap5 = outputPath.resolve("00000000000000000005.caption");
      assertTrue(Files.isRegularFile(cap1));
      assertTrue(Files.isRegularFile(cap2));
      assertTrue(Files.isRegularFile(cap3));
      assertTrue(Files.isRegularFile(cap4));
      assertTrue(Files.isRegularFile(cap5));
      assertEquals(10, files.size());

      final var m = new HashMap<String, Object>();
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
        LCaptionFiles.parse(m, cap1),
        () -> "File %s must have the expected content".formatted(cap1)
      );
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
        LCaptionFiles.parse(m, cap2),
        () -> "File %s must have the expected content".formatted(cap2)
      );
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
        LCaptionFiles.parse(m, cap3),
        () -> "File %s must have the expected content".formatted(cap3)
      );
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
        LCaptionFiles.parse(m, cap4),
        () -> "File %s must have the expected content".formatted(cap4)
      );
      assertEquals(
        List.of(
          new LCaptionName("animal"),
          new LCaptionName("cat"),
          new LCaptionName("grass"),
          new LCaptionName("outdoors"),
          new LCaptionName("oversized animal")
        ),
        LCaptionFiles.parse(m, cap5),
        () -> "File %s must have the expected content".formatted(cap5)
      );
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
