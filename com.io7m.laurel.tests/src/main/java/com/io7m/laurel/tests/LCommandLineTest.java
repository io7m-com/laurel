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

import com.io7m.laurel.cmdline.LCMain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LCommandLineTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LCommandLineTest.class);

  private Path directory;

  @BeforeEach
  public void setup(
    final @TempDir Path directory)
  {
    this.directory = directory;
  }

  @Test
  public void testUsage()
  {
    LCMain.mainExitless(new String[]{});
  }

  @Test
  public void testHelp()
  {
    LCMain.mainExitless(new String[]{
      "help",
      "help"
    });
  }

  @Test
  public void testImport()
    throws IOException
  {
    final var inputDir =
      this.unpack("dataset_good.zip", "import");
    final var outputFile =
      this.directory.resolve("output.db")
        .toAbsolutePath();

    final var r = LCMain.mainExitless(new String[]{
      "import",
      "--input-directory",
      inputDir.toAbsolutePath().toString(),
      "--output-file",
      outputFile.toString()
    });
    assertEquals(0, r);

    assertTrue(
      Files.isRegularFile(outputFile),
      () -> "Output file %s must be a regular file".formatted(outputFile)
    );
  }

  @Test
  public void testExport()
    throws IOException
  {
    final var inputDir =
      this.unpack("dataset_good.zip", "import");
    final var outputFile =
      this.directory.resolve("output.db")
        .toAbsolutePath();
    final var outputDirectory =
      this.directory.resolve("export")
        .toAbsolutePath();

    var r = LCMain.mainExitless(new String[]{
      "import",
      "--input-directory",
      inputDir.toAbsolutePath().toString(),
      "--output-file",
      outputFile.toString()
    });
    assertEquals(0, r);

    r = LCMain.mainExitless(new String[]{
      "export",
      "--output-directory",
      outputDirectory.toAbsolutePath().toString(),
      "--input-file",
      outputFile.toString()
    });
    assertEquals(0, r);

    assertTrue(
      Files.isDirectory(outputDirectory),
      () -> "Output directory %s must be a directory".formatted(outputDirectory)
    );
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
