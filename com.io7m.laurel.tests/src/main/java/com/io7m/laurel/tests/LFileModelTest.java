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

import com.io7m.laurel.filemodel.LFileModelType;
import com.io7m.laurel.filemodel.LFileModels;
import com.io7m.laurel.gui.internal.LPerpetualSubscriber;
import com.io7m.laurel.model.LCategory;
import com.io7m.laurel.model.LException;
import com.io7m.laurel.model.LTag;
import com.io7m.zelador.test_extension.CloseableResourcesType;
import com.io7m.zelador.test_extension.ZeladorExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ExtendWith({ZeladorExtension.class})
public final class LFileModelTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LFileModelTest.class);

  private static final long TIMEOUT = 10L;

  private static final OpenOption[] OPEN_OPTIONS = {
    StandardOpenOption.WRITE,
    StandardOpenOption.CREATE,
    StandardOpenOption.TRUNCATE_EXISTING
  };

  private Path file;
  private LFileModelType model;
  private Path imageFile;
  private Path textFile;

  @BeforeEach
  public void setup(
    final @TempDir Path directory,
    final CloseableResourcesType resources)
    throws Exception
  {
    this.file =
      directory.resolve("file.lau");
    this.imageFile =
      directory.resolve("image.png");
    this.textFile =
      directory.resolve("file.txt");

    Files.writeString(this.textFile, "Not an image.");

    this.model =
      LFileModels.open(this.file, false);

    resources.addPerTestResource(
      this.model.undoText()
        .subscribe((oldValue, newValue) -> LOG.debug("Undo: {}", newValue))
    );

    resources.addPerTestResource(
      this.model.redoText()
        .subscribe((oldValue, newValue) -> LOG.debug("Redo: {}", newValue))
    );

    this.model.events()
      .subscribe(new LPerpetualSubscriber<>(event -> {
        LOG.debug("Event: {}", event);
      }));

    try (var stream = LFileModelTest.class.getResourceAsStream(
      "/com/io7m/laurel/tests/001.png")) {
      Files.write(this.imageFile, stream.readAllBytes(), OPEN_OPTIONS);
    }
  }

  @Test
  public void testImageSelectNonexistent()
    throws Exception
  {
    assertEquals(Optional.empty(), this.model.imageSelected().get());
    this.model.imageSelect(Optional.of("nonexistent")).get(TIMEOUT, SECONDS);
    assertEquals(Optional.empty(), this.model.imageSelected().get());
  }

  @Test
  public void testImageSelect()
    throws Exception
  {
    this.model.imageAdd(
      "image-a",
      this.imageFile,
      Optional.of(this.imageFile.toUri())
    ).get(TIMEOUT, SECONDS);

    assertEquals(List.of(), this.model.tagsAssigned().get());
    assertEquals(Optional.empty(), this.model.imageSelected().get());
    this.model.imageSelect(Optional.of("image-a")).get(TIMEOUT, SECONDS);

    final var image = this.model.imageSelected().get().get();
    assertEquals("image-a", image.name());
    assertEquals(List.of(), this.model.tagsAssigned().get());

    this.model.imageSelect(Optional.empty()).get(TIMEOUT, SECONDS);
    assertEquals(Optional.empty(), this.model.imageSelected().get());
    assertEquals(List.of(), this.model.tagsAssigned().get());

    this.compact();
  }

  @Test
  public void testImageAdd()
    throws Exception
  {
    assertEquals(Optional.empty(), this.model.undoText().get());
    assertEquals(0, this.model.imageList().get().size());

    this.model.imageAdd(
      "image-a",
      this.imageFile,
      Optional.of(this.imageFile.toUri())
    ).get(TIMEOUT, SECONDS);

    assertEquals(
      Optional.of("Add image(s)"),
      this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());
    assertEquals(1, this.model.imageList().get().size());

    this.model.imageAdd(
      "image-b",
      this.imageFile,
      Optional.of(this.imageFile.toUri())
    ).get(TIMEOUT, SECONDS);

    assertEquals(
      Optional.of("Add image(s)"),
      this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());
    assertEquals(2, this.model.imageList().get().size());

    this.model.imageAdd(
      "image-c",
      this.imageFile,
      Optional.of(this.imageFile.toUri())
    ).get(TIMEOUT, SECONDS);

    assertEquals(
      Optional.of("Add image(s)"),
      this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());
    assertEquals(3, this.model.imageList().get().size());

    /*
     * Now undo the operations.
     */

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(
      Optional.of("Add image(s)"),
      this.model.undoText().get());
    assertEquals(
      Optional.of("Add image(s)"),
      this.model.redoText().get());
    assertEquals(2, this.model.imageList().get().size());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(
      Optional.of("Add image(s)"),
      this.model.undoText().get());
    assertEquals(
      Optional.of("Add image(s)"),
      this.model.redoText().get());
    assertEquals(1, this.model.imageList().get().size());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(Optional.empty(), this.model.undoText().get());
    assertEquals(
      Optional.of("Add image(s)"),
      this.model.redoText().get());
    assertEquals(0, this.model.imageList().get().size());

    /*
     * Now redo the operations.
     */

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(
      Optional.of("Add image(s)"),
      this.model.undoText().get());
    assertEquals(
      Optional.of("Add image(s)"),
      this.model.redoText().get());
    assertEquals(1, this.model.imageList().get().size());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(
      Optional.of("Add image(s)"),
      this.model.undoText().get());
    assertEquals(
      Optional.of("Add image(s)"),
      this.model.redoText().get());
    assertEquals(2, this.model.imageList().get().size());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(
      Optional.of("Add image(s)"),
      this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());
    assertEquals(3, this.model.imageList().get().size());

    this.compact();
  }

  @Test
  public void testImageAddImageNonexistent()
    throws Exception
  {
    assertEquals(Optional.empty(), this.model.undoText().get());

    final var ex =
      Assertions.assertThrows(ExecutionException.class, () -> {
        this.model.imageAdd(
          "image-a",
          this.file.getParent().resolve("nonexistent.txt"),
          Optional.of(this.imageFile.toUri())
        ).get(TIMEOUT, SECONDS);
      });

    final var ee = assertInstanceOf(LException.class, ex.getCause());
    assertEquals("error-io", ee.errorCode());

    assertEquals(Optional.empty(), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());
  }

  @Test
  public void testImageAddImageCorrupt()
    throws Exception
  {
    assertEquals(Optional.empty(), this.model.undoText().get());

    final var ex =
      Assertions.assertThrows(ExecutionException.class, () -> {
        this.model.imageAdd(
          "image-a",
          this.textFile,
          Optional.of(this.textFile.toUri())
        ).get(TIMEOUT, SECONDS);
      });

    final var ee = assertInstanceOf(LException.class, ex.getCause());
    assertEquals("error-image-format", ee.errorCode());

    assertEquals(Optional.empty(), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());
  }

  @Test
  public void testTagAdd()
    throws Exception
  {
    final var ta = new LTag("A");
    final var tb = new LTag("B");
    final var tc = new LTag("C");

    assertEquals(Optional.empty(), this.model.undoText().get());

    this.model.tagAdd(ta).get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta), this.model.tagList().get());
    assertEquals(Optional.of("Add tag(s)"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    this.model.tagAdd(tb).get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb), this.model.tagList().get());
    assertEquals(Optional.of("Add tag(s)"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    this.model.tagAdd(tc).get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb, tc), this.model.tagList().get());
    assertEquals(Optional.of("Add tag(s)"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    this.model.tagAdd(ta).get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb, tc), this.model.tagList().get());
    assertEquals(Optional.of("Add tag(s)"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    /*
     * Now undo the operations.
     */

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb), this.model.tagList().get());
    assertEquals(Optional.of("Add tag(s)"), this.model.undoText().get());
    assertEquals(Optional.of("Add tag(s)"), this.model.redoText().get());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta), this.model.tagList().get());
    assertEquals(Optional.of("Add tag(s)"), this.model.undoText().get());
    assertEquals(Optional.of("Add tag(s)"), this.model.redoText().get());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.model.tagList().get());
    assertEquals(Optional.empty(), this.model.undoText().get());
    assertEquals(Optional.of("Add tag(s)"), this.model.redoText().get());

    /*
     * Now redo the operations.
     */

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta), this.model.tagList().get());
    assertEquals(Optional.of("Add tag(s)"), this.model.undoText().get());
    assertEquals(Optional.of("Add tag(s)"), this.model.redoText().get());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb), this.model.tagList().get());
    assertEquals(Optional.of("Add tag(s)"), this.model.undoText().get());
    assertEquals(Optional.of("Add tag(s)"), this.model.redoText().get());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb, tc), this.model.tagList().get());
    assertEquals(Optional.of("Add tag(s)"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    this.compact();
  }

  private void compact()
    throws Exception
  {
    this.model.compact().get(TIMEOUT, SECONDS);
    assertEquals(Optional.empty(), this.model.redoText().get());
    assertEquals(Optional.empty(), this.model.undoText().get());
  }

  @Test
  public void testCategoryAdd()
    throws Exception
  {
    final var ta = new LCategory("A");
    final var tb = new LCategory("B");
    final var tc = new LCategory("C");

    assertEquals(Optional.empty(), this.model.undoText().get());

    this.model.categoryAdd(ta).get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta), this.model.categoryList().get());
    assertEquals(Optional.of("Add categories"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    this.model.categoryAdd(tb).get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb), this.model.categoryList().get());
    assertEquals(Optional.of("Add categories"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    this.model.categoryAdd(tc).get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb, tc), this.model.categoryList().get());
    assertEquals(Optional.of("Add categories"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    this.model.categoryAdd(ta).get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb, tc), this.model.categoryList().get());
    assertEquals(Optional.of("Add categories"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    /*
     * Now undo the operations.
     */

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb), this.model.categoryList().get());
    assertEquals(Optional.of("Add categories"), this.model.undoText().get());
    assertEquals(Optional.of("Add categories"), this.model.redoText().get());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta), this.model.categoryList().get());
    assertEquals(Optional.of("Add categories"), this.model.undoText().get());
    assertEquals(Optional.of("Add categories"), this.model.redoText().get());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.model.categoryList().get());
    assertEquals(Optional.empty(), this.model.undoText().get());
    assertEquals(Optional.of("Add categories"), this.model.redoText().get());

    /*
     * Now redo the operations.
     */

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta), this.model.categoryList().get());
    assertEquals(Optional.of("Add categories"), this.model.undoText().get());
    assertEquals(Optional.of("Add categories"), this.model.redoText().get());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb), this.model.categoryList().get());
    assertEquals(Optional.of("Add categories"), this.model.undoText().get());
    assertEquals(Optional.of("Add categories"), this.model.redoText().get());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb, tc), this.model.categoryList().get());
    assertEquals(Optional.of("Add categories"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    this.compact();
  }
}
