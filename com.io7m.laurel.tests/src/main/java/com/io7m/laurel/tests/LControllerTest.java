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

import com.io7m.jattribute.core.Attributes;
import com.io7m.laurel.gui.internal.LController;
import com.io7m.laurel.gui.internal.LImageSetSaved;
import com.io7m.laurel.gui.internal.LImageSetStateNone;
import com.io7m.laurel.gui.internal.LImageSetUnsaved;
import com.io7m.laurel.gui.internal.LPerpetualSubscriber;
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

  private LController controller;
  private ArrayList<SStructuredErrorType<?>> errors;

  @BeforeEach
  public void setup()
  {
    this.controller =
      new LController(Attributes.create(ex -> {}));
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
      LImageSetStateNone.class,
      this.controller.imageSetState().get()
    );

    this.controller.open(file).get(5L, TimeUnit.SECONDS);

    assertInstanceOf(
      LImageSetSaved.class,
      this.controller.imageSetState().get()
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
      LImageSetStateNone.class,
      this.controller.imageSetState().get()
    );

    assertThrows(Exception.class, () -> {
      this.controller.open(file).get(5L, TimeUnit.SECONDS);
    });

    assertInstanceOf(
      LImageSetStateNone.class,
      this.controller.imageSetState().get()
    );
  }

  @Test
  public void testOpenFails1(
    final @TempDir Path directory)
    throws Exception
  {
    assertInstanceOf(
      LImageSetStateNone.class,
      this.controller.imageSetState().get()
    );

    assertThrows(Exception.class, () -> {
      this.controller.open(directory.resolve("nonexistent"))
        .get(5L, TimeUnit.SECONDS);
    });

    assertInstanceOf(
      LImageSetStateNone.class,
      this.controller.imageSetState().get()
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
      LImageSetStateNone.class,
      this.controller.imageSetState().get()
    );

    this.controller.open(file).get(5L, TimeUnit.SECONDS);

    assertInstanceOf(
      LImageSetSaved.class,
      this.controller.imageSetState().get()
    );

    this.controller.captionNew("hello");

    assertInstanceOf(
      LImageSetUnsaved.class,
      this.controller.imageSetState().get()
    );

    this.controller.save().get(5L, TimeUnit.SECONDS);

    assertInstanceOf(
      LImageSetSaved.class,
      this.controller.imageSetState().get()
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
      LImageSetStateNone.class,
      this.controller.imageSetState().get()
    );

    this.controller.newSet(file).get(5L, TimeUnit.SECONDS);

    assertInstanceOf(
      LImageSetUnsaved.class,
      this.controller.imageSetState().get()
    );

    this.controller.captionNew("hello");

    assertInstanceOf(
      LImageSetUnsaved.class,
      this.controller.imageSetState().get()
    );

    this.controller.save().get(5L, TimeUnit.SECONDS);

    assertInstanceOf(
      LImageSetSaved.class,
      this.controller.imageSetState().get()
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
      LImageSetStateNone.class,
      this.controller.imageSetState().get()
    );

    this.controller.newSet(file).get(5L, TimeUnit.SECONDS);

    assertInstanceOf(
      LImageSetUnsaved.class,
      this.controller.imageSetState().get()
    );

    this.controller.captionNew("hello");

    assertInstanceOf(
      LImageSetUnsaved.class,
      this.controller.imageSetState().get()
    );

    this.controller.undo();

    assertInstanceOf(
      LImageSetUnsaved.class,
      this.controller.imageSetState().get()
    );

    this.controller.redo();

    assertInstanceOf(
      LImageSetUnsaved.class,
      this.controller.imageSetState().get()
    );

    this.controller.save().get(5L, TimeUnit.SECONDS);

    assertInstanceOf(
      LImageSetSaved.class,
      this.controller.imageSetState().get()
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
      LImageSetStateNone.class,
      this.controller.imageSetState().get()
    );

    this.controller.newSet(file).get(5L, TimeUnit.SECONDS);

    assertInstanceOf(
      LImageSetUnsaved.class,
      this.controller.imageSetState().get()
    );

    this.controller.imagesAdd(List.of(imageFile)).get(5L, TimeUnit.SECONDS);

    assertInstanceOf(
      LImageSetUnsaved.class,
      this.controller.imageSetState().get()
    );

    this.controller.undo();

    assertInstanceOf(
      LImageSetUnsaved.class,
      this.controller.imageSetState().get()
    );

    this.controller.redo();

    assertInstanceOf(
      LImageSetUnsaved.class,
      this.controller.imageSetState().get()
    );

    this.controller.save().get(5L, TimeUnit.SECONDS);

    assertInstanceOf(
      LImageSetSaved.class,
      this.controller.imageSetState().get()
    );
  }

  @Test
  public void testNewSaveFails(
    final @TempDir Path directory)
    throws Exception
  {
    assertInstanceOf(
      LImageSetStateNone.class,
      this.controller.imageSetState().get()
    );

    this.controller.newSet(directory).get(5L, TimeUnit.SECONDS);

    assertInstanceOf(
      LImageSetUnsaved.class,
      this.controller.imageSetState().get()
    );

    this.controller.captionNew("hello");

    assertInstanceOf(
      LImageSetUnsaved.class,
      this.controller.imageSetState().get()
    );

    assertThrows(Exception.class, () -> {
      this.controller.save().get(5L, TimeUnit.SECONDS);
    });

    assertInstanceOf(
      LImageSetUnsaved.class,
      this.controller.imageSetState().get()
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
