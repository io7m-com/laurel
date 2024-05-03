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

import com.io7m.laurel.gui.internal.LController;
import com.io7m.laurel.gui.internal.LPerpetualSubscriber;
import com.io7m.laurel.gui.internal.model.LModelFileStatusType;
import com.io7m.seltzer.api.SStructuredErrorType;
import javafx.application.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LControllerTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LControllerTest.class);

  private static final long TIMEOUT = 10L;

  private LController controller;
  private ArrayList<SStructuredErrorType<?>> errors;

  @BeforeEach
  public void setup()
  {
    this.controller =
      new LController();
    this.errors =
      new ArrayList<>();
    this.controller.errors()
      .subscribe(new LPerpetualSubscriber<>(e -> {
        LOG.debug("{}", e);
        this.errors.add(e);
      }));

    try {
      Platform.startup(() -> {

      });
    } catch (final Exception e) {
      // Don't care
    }
  }

  @AfterEach
  public void tearDown()
  {
    this.controller.close();
  }

  @Test
  public void testInitial()
  {
    assertTrue(this.controller.isSaved());
    assertFalse(this.controller.isBusy());
  }

  @Test
  public void testOpen(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      copyTo(directory, "laurel-example-0.xml");

    assertInstanceOf(
      LModelFileStatusType.None.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.open(file).get(TIMEOUT, TimeUnit.SECONDS);

    assertInstanceOf(
      LModelFileStatusType.Saved.class,
      this.controller.fileStatus().getValue()
    );
  }

  @Test
  public void testOpenFails0(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      copyTo(directory, "laurel-error1.xml");

    assertInstanceOf(
      LModelFileStatusType.None.class,
      this.controller.fileStatus().getValue()
    );

    assertThrows(Exception.class, () -> {
      this.controller.open(file).get(TIMEOUT, TimeUnit.SECONDS);
    });

    assertInstanceOf(
      LModelFileStatusType.None.class,
      this.controller.fileStatus().getValue()
    );
  }

  @Test
  public void testOpenFails1(
    final @TempDir Path directory)
    throws Exception
  {
    assertInstanceOf(
      LModelFileStatusType.None.class,
      this.controller.fileStatus().getValue()
    );

    assertThrows(Exception.class, () -> {
      this.controller.open(directory.resolve("nonexistent"))
        .get(TIMEOUT, TimeUnit.SECONDS);
    });

    assertInstanceOf(
      LModelFileStatusType.None.class,
      this.controller.fileStatus().getValue()
    );
  }

  @Test
  public void testOpenSaveWorkflow(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      copyTo(directory, "laurel-example-0.xml");

    assertInstanceOf(
      LModelFileStatusType.None.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.open(file).get(TIMEOUT, TimeUnit.SECONDS);

    assertInstanceOf(
      LModelFileStatusType.Saved.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.captionNew("hello");

    assertInstanceOf(
      LModelFileStatusType.Unsaved.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.save().get(TIMEOUT, TimeUnit.SECONDS);

    assertInstanceOf(
      LModelFileStatusType.Saved.class,
      this.controller.fileStatus().getValue()
    );
  }

  @Test
  public void testNewSaveWorkflow(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      copyTo(directory, "laurel-example-0.xml");

    assertInstanceOf(
      LModelFileStatusType.None.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.newSet(file);

    assertInstanceOf(
      LModelFileStatusType.Unsaved.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.captionNew("hello");

    assertInstanceOf(
      LModelFileStatusType.Unsaved.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.save().get(TIMEOUT, TimeUnit.SECONDS);

    assertInstanceOf(
      LModelFileStatusType.Saved.class,
      this.controller.fileStatus().getValue()
    );
  }

  @Test
  public void testNewUndoWorkflow(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      copyTo(directory, "laurel-example-0.xml");

    assertInstanceOf(
      LModelFileStatusType.None.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.newSet(file);

    assertInstanceOf(
      LModelFileStatusType.Unsaved.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.captionNew("hello");

    assertInstanceOf(
      LModelFileStatusType.Unsaved.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.undo();

    assertInstanceOf(
      LModelFileStatusType.Unsaved.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.redo();

    assertInstanceOf(
      LModelFileStatusType.Unsaved.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.save().get(TIMEOUT, TimeUnit.SECONDS);

    assertInstanceOf(
      LModelFileStatusType.Saved.class,
      this.controller.fileStatus().getValue()
    );
  }

  @Test
  public void testNewImageAddWorkflow(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      directory.resolve("out.xml");
    final var imageFile =
      copyTo(directory, "001.png");

    assertInstanceOf(
      LModelFileStatusType.None.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.newSet(file);

    assertInstanceOf(
      LModelFileStatusType.Unsaved.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.imagesAdd(List.of(imageFile)).get(TIMEOUT, TimeUnit.SECONDS);

    assertInstanceOf(
      LModelFileStatusType.Unsaved.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.undo();

    assertInstanceOf(
      LModelFileStatusType.Unsaved.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.redo();

    assertInstanceOf(
      LModelFileStatusType.Unsaved.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.save().get(TIMEOUT, TimeUnit.SECONDS);

    assertInstanceOf(
      LModelFileStatusType.Saved.class,
      this.controller.fileStatus().getValue()
    );
  }

  @Test
  public void testNewSaveFails(
    final @TempDir Path directory)
    throws Exception
  {
    assertInstanceOf(
      LModelFileStatusType.None.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.newSet(directory);

    assertInstanceOf(
      LModelFileStatusType.Unsaved.class,
      this.controller.fileStatus().getValue()
    );

    this.controller.captionNew("hello");

    assertInstanceOf(
      LModelFileStatusType.Unsaved.class,
      this.controller.fileStatus().getValue()
    );

    assertThrows(Exception.class, () -> {
      this.controller.save().get(TIMEOUT, TimeUnit.SECONDS);
    });

    assertInstanceOf(
      LModelFileStatusType.Unsaved.class,
      this.controller.fileStatus().getValue()
    );
  }

  private static Path copyTo(
    final Path directory,
    final String name)
    throws Exception
  {
    final var inputURL =
      LControllerTest.class.getResource("/com/io7m/laurel/tests/" + name);

    if (inputURL == null) {
      throw new NoSuchFileException(inputURL.toString());
    }

    final var output = directory.resolve(name);
    try (var stream = inputURL.openStream()) {
      Files.copy(stream, output);
    }
    return output;
  }
}
