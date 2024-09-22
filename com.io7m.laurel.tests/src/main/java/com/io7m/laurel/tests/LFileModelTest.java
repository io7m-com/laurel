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
import com.io7m.laurel.filemodel.LImageCaptionsAssignment;
import com.io7m.laurel.filemodel.LCategoryCaptionsAssignment;
import com.io7m.laurel.gui.internal.LPerpetualSubscriber;
import com.io7m.laurel.model.LCaptionName;
import com.io7m.laurel.model.LCategory;
import com.io7m.laurel.model.LCategoryID;
import com.io7m.laurel.model.LCategoryName;
import com.io7m.laurel.model.LException;
import com.io7m.laurel.model.LImageID;
import com.io7m.laurel.model.LCaption;
import com.io7m.laurel.model.LMetadataValue;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
    this.model.imageSelect(Optional.of(new LImageID(2300L))).get(
      TIMEOUT,
      SECONDS);
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

    final var imageId =
      this.model.imageList().get().get(0).id();

    assertEquals(List.of(), this.imageCaptionsAssignedNow());
    assertEquals(Optional.empty(), this.model.imageSelected().get());
    this.model.imageSelect(Optional.of(imageId)).get(TIMEOUT, SECONDS);

    final var image = this.model.imageSelected().get().get();
    assertEquals("image-a", image.image().name());
    assertEquals(List.of(), this.imageCaptionsAssignedNow());

    this.model.imageSelect(Optional.empty()).get(TIMEOUT, SECONDS);
    assertEquals(Optional.empty(), this.model.imageSelected().get());
    assertEquals(List.of(), this.imageCaptionsAssignedNow());

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
  public void testCaptionAdd()
    throws Exception
  {
    final var ta = new LCaptionName("A");
    final var tb = new LCaptionName("B");
    final var tc = new LCaptionName("C");

    assertEquals(Optional.empty(), this.model.undoText().get());

    this.model.captionAdd(ta).get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta), this.captionListNow());
    assertEquals(Optional.of("Add caption(s)"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    this.model.captionAdd(tb).get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb), this.captionListNow());
    assertEquals(Optional.of("Add caption(s)"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    this.model.captionAdd(tc).get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb, tc), this.captionListNow());
    assertEquals(Optional.of("Add caption(s)"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    this.model.captionAdd(ta).get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb, tc), this.captionListNow());
    assertEquals(Optional.of("Add caption(s)"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    /*
     * Now undo the operations.
     */

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb), this.captionListNow());
    assertEquals(Optional.of("Add caption(s)"), this.model.undoText().get());
    assertEquals(Optional.of("Add caption(s)"), this.model.redoText().get());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta), this.captionListNow());
    assertEquals(Optional.of("Add caption(s)"), this.model.undoText().get());
    assertEquals(Optional.of("Add caption(s)"), this.model.redoText().get());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.captionListNow());
    assertEquals(Optional.empty(), this.model.undoText().get());
    assertEquals(Optional.of("Add caption(s)"), this.model.redoText().get());

    /*
     * Now redo the operations.
     */

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta), this.captionListNow());
    assertEquals(Optional.of("Add caption(s)"), this.model.undoText().get());
    assertEquals(Optional.of("Add caption(s)"), this.model.redoText().get());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb), this.captionListNow());
    assertEquals(Optional.of("Add caption(s)"), this.model.undoText().get());
    assertEquals(Optional.of("Add caption(s)"), this.model.redoText().get());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb, tc), this.captionListNow());
    assertEquals(Optional.of("Add caption(s)"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    this.compact();
  }

  private List<LCaptionName> captionListNow()
  {
    return this.model.captionList()
      .get()
      .stream()
      .map(LCaption::name)
      .toList();
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
    final var tan = new LCategoryName("A");
    final var tbn = new LCategoryName("B");
    final var tcn = new LCategoryName("C");

    assertEquals(Optional.empty(), this.model.undoText().get());


    this.model.categoryAdd(tan).get(TIMEOUT, SECONDS);
    final var ta = this.findCategoryID(tan.text());

    assertEquals(List.of(ta), this.categoryListIDsNow());
    assertEquals(Optional.of("Add categories"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    this.model.categoryAdd(tbn).get(TIMEOUT, SECONDS);
    final var tb = this.findCategoryID(tbn.text());

    assertEquals(List.of(ta, tb), this.categoryListIDsNow());
    assertEquals(Optional.of("Add categories"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    this.model.categoryAdd(tcn).get(TIMEOUT, SECONDS);
    final var tc = this.findCategoryID(tcn.text());

    assertEquals(List.of(ta, tb, tc), this.categoryListIDsNow());
    assertEquals(Optional.of("Add categories"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    this.model.categoryAdd(tan).get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb, tc), this.categoryListIDsNow());
    assertEquals(Optional.of("Add categories"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    /*
     * Now undo the operations.
     */

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb), this.categoryListIDsNow());
    assertEquals(Optional.of("Add categories"), this.model.undoText().get());
    assertEquals(Optional.of("Add categories"), this.model.redoText().get());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta), this.categoryListIDsNow());
    assertEquals(Optional.of("Add categories"), this.model.undoText().get());
    assertEquals(Optional.of("Add categories"), this.model.redoText().get());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.categoryListIDsNow());
    assertEquals(Optional.empty(), this.model.undoText().get());
    assertEquals(Optional.of("Add categories"), this.model.redoText().get());

    /*
     * Now redo the operations.
     */

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta), this.categoryListIDsNow());
    assertEquals(Optional.of("Add categories"), this.model.undoText().get());
    assertEquals(Optional.of("Add categories"), this.model.redoText().get());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb), this.categoryListIDsNow());
    assertEquals(Optional.of("Add categories"), this.model.undoText().get());
    assertEquals(Optional.of("Add categories"), this.model.redoText().get());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta, tb, tc), this.categoryListIDsNow());
    assertEquals(Optional.of("Add categories"), this.model.undoText().get());
    assertEquals(Optional.empty(), this.model.redoText().get());

    this.compact();
  }

  private List<LCategoryID> categoryListIDsNow()
  {
    return this.model.categoryList()
      .get()
      .stream()
      .map(LCategory::id)
      .toList();
  }

  @Test
  public void testCategorySetRequired()
    throws Exception
  {
    final var tan = new LCategoryName("A");
    final var tbn = new LCategoryName("B");
    final var tcn = new LCategoryName("C");

    assertEquals(List.of(), this.model.categoriesRequired().get());
    assertEquals(Optional.empty(), this.model.undoText().get());

    this.model.categoryAdd(tan).get(TIMEOUT, SECONDS);
    this.model.categoryAdd(tbn).get(TIMEOUT, SECONDS);
    this.model.categoryAdd(tcn).get(TIMEOUT, SECONDS);

    final var ta = this.findCategory(tan);
    final var tb = this.findCategory(tbn);
    final var tc = this.findCategory(tcn);

    this.model.categorySetRequired(Set.of(ta.id(), tc.id())).get(TIMEOUT, SECONDS);
    assertEquals(
      List.of(ta.id(), tc.id()),
      this.categoriesRequiredIDNow()
    );

    // Redundant, won't be added to the undo stack.
    this.model.categorySetRequired(Set.of(ta.id(), tc.id())).get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta.id(), tc.id()), this.categoriesRequiredIDNow());

    // Redundant, won't be added to the undo stack.
    this.model.categorySetNotRequired(Set.of(tb.id())).get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta.id(), tc.id()), this.categoriesRequiredIDNow());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.categoriesRequiredIDNow());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta.id(), tc.id()), this.categoriesRequiredIDNow());

    this.model.categorySetNotRequired(Set.of(ta.id())).get(TIMEOUT, SECONDS);
    assertEquals(List.of(tc.id()), this.categoriesRequiredIDNow());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ta.id(), tc.id()), this.categoriesRequiredIDNow());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(tc.id()), this.categoriesRequiredIDNow());
  }

  private List<LCategoryID> categoriesRequiredIDNow()
  {
    return this.model.categoriesRequired()
      .get()
      .stream()
      .map(LCategory::id)
      .toList();
  }

  @Test
  public void testCategoryTagAssign()
    throws Exception
  {
    final var can = new LCategoryName("A");
    final var cbn = new LCategoryName("B");

    final var txn = new LCaptionName("TX");
    final var tyn = new LCaptionName("TY");
    final var tzn = new LCaptionName("TZ");

    assertEquals(List.of(), this.categoriesRequiredIDNow());
    assertEquals(Optional.empty(), this.model.undoText().get());

    this.model.categoryAdd(can).get(TIMEOUT, SECONDS);
    this.model.categoryAdd(cbn).get(TIMEOUT, SECONDS);

    final var ca = this.findCategoryID(can.text());
    final var cb = this.findCategoryID(cbn.text());

    this.model.captionAdd(txn).get(TIMEOUT, SECONDS);
    this.model.captionAdd(tyn).get(TIMEOUT, SECONDS);
    this.model.captionAdd(tzn).get(TIMEOUT, SECONDS);

    final var tx = this.findCaption(txn.text());
    final var ty = this.findCaption(tyn.text());
    final var tz = this.findCaption(tzn.text());

    this.model.categoryCaptionsAssign(List.of(
      new LCategoryCaptionsAssignment(ca, List.of(tx.id(), ty.id()))
    )).get(TIMEOUT, SECONDS);

    assertEquals(List.of(tx.name(), ty.name()), this.categoryCaptionsNow(ca));
    assertEquals(List.of(), this.categoryCaptionsNow(cb));

    this.model.categoryCaptionsAssign(List.of(
      new LCategoryCaptionsAssignment(cb, List.of(ty.id(), tz.id()))
    )).get(TIMEOUT, SECONDS);

    // Redundant, does nothing
    this.model.categoryCaptionsAssign(List.of()).get(TIMEOUT, SECONDS);

    assertEquals(List.of(tx.name(), ty.name()), this.categoryCaptionsNow(ca));
    assertEquals(List.of(ty.name(), tz.name()), this.categoryCaptionsNow(cb));

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.name(), ty.name()), this.categoryCaptionsNow(ca));
    assertEquals(List.of(), this.categoryCaptionsNow(cb));

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.categoryCaptionsNow(ca));
    assertEquals(List.of(), this.categoryCaptionsNow(cb));

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.name(), ty.name()), this.categoryCaptionsNow(ca));
    assertEquals(List.of(), this.categoryCaptionsNow(cb));

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.name(), ty.name()), this.categoryCaptionsNow(ca));
    assertEquals(List.of(ty.name(), tz.name()), this.categoryCaptionsNow(cb));

    this.model.categoryCaptionsUnassign(
      List.of(new LCategoryCaptionsAssignment(ca, List.of(tx.id())))
    ).get(TIMEOUT, SECONDS);

    assertEquals(List.of(ty.name()), this.categoryCaptionsNow(ca));
    assertEquals(List.of(ty.name(), tz.name()), this.categoryCaptionsNow(cb));

    this.model.categoryCaptionsUnassign(
      List.of(new LCategoryCaptionsAssignment(cb, List.of(tz.id())))
    ).get(TIMEOUT, SECONDS);

    assertEquals(List.of(ty.name()), this.categoryCaptionsNow(ca));
    assertEquals(List.of(ty.name()), this.categoryCaptionsNow(cb));

    // Redundant, does nothing
    this.model.categoryCaptionsUnassign(List.of()).get(TIMEOUT, SECONDS);

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ty.name()), this.categoryCaptionsNow(ca));
    assertEquals(List.of(ty.name(), tz.name()), this.categoryCaptionsNow(cb));

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.name(), ty.name()), this.categoryCaptionsNow(ca));
    assertEquals(List.of(ty.name(), tz.name()), this.categoryCaptionsNow(cb));

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ty.name()), this.categoryCaptionsNow(ca));
    assertEquals(List.of(ty.name(), tz.name()), this.categoryCaptionsNow(cb));

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ty.name()), this.categoryCaptionsNow(ca));
    assertEquals(List.of(ty.name()), this.categoryCaptionsNow(cb));
  }

  @Test
  public void testImageCaptionsAssignUnassign()
    throws Exception
  {
    this.model.imageAdd(
      "image-a",
      this.imageFile,
      Optional.of(this.imageFile.toUri())
    ).get(TIMEOUT, SECONDS);

    this.model.imageAdd(
      "image-b",
      this.imageFile,
      Optional.of(this.imageFile.toUri())
    ).get(TIMEOUT, SECONDS);

    final var i0 = this.model.imageList().get().get(0).id();
    final var i1 = this.model.imageList().get().get(1).id();

    this.model.imageSelect(Optional.of(i1)).get(TIMEOUT, SECONDS);

    this.model.captionAdd(new LCaptionName("TX")).get(TIMEOUT, SECONDS);
    this.model.captionAdd(new LCaptionName("TY")).get(TIMEOUT, SECONDS);
    this.model.captionAdd(new LCaptionName("TZ")).get(TIMEOUT, SECONDS);

    final var tx = this.findCaption("TX");

    assertEquals(List.of(), this.imageCaptionsAssignedNow());

    final var assign = new LImageCaptionsAssignment(i1, Set.of(tx.id()));

    this.model.imageCaptionsAssign(List.of(assign)).get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.name()), this.imageCaptionsAssignedNow());

    this.model.imageCaptionsUnassign(List.of(assign)).get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.imageCaptionsAssignedNow());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.name()), this.imageCaptionsAssignedNow());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.imageCaptionsAssignedNow());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.name()), this.imageCaptionsAssignedNow());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.imageCaptionsAssignedNow());
  }

  @Test
  public void testImageDelete()
    throws Exception
  {
    this.model.imageAdd(
      "image-a",
      this.imageFile,
      Optional.of(this.imageFile.toUri())
    ).get(TIMEOUT, SECONDS);

    this.model.imageAdd(
      "image-b",
      this.imageFile,
      Optional.of(this.imageFile.toUri())
    ).get(TIMEOUT, SECONDS);

    final var i0 =
      this.model.imageList().get().get(0).id();
    final var i1 =
      this.model.imageList().get().get(1).id();

    this.model.captionAdd(new LCaptionName("TX")).get(TIMEOUT, SECONDS);
    this.model.captionAdd(new LCaptionName("TY")).get(TIMEOUT, SECONDS);
    this.model.captionAdd(new LCaptionName("TZ")).get(TIMEOUT, SECONDS);

    final var tx = this.findCaption("TX");
    final var ty = this.findCaption("TY");
    final var tz = this.findCaption("TZ");

    assertEquals(List.of(), this.imageCaptionsAssignedNow());

    final var assign0 =
      new LImageCaptionsAssignment(
        i0,
        Set.of(tx.id(), ty.id(), tz.id())
      );
    final var assign1 =
      new LImageCaptionsAssignment(
        i0,
        Set.of(ty.id())
      );

    this.model.imageSelect(Optional.of(i0)).get(TIMEOUT, SECONDS);
    this.model.imageCaptionsAssign(List.of(assign0, assign1)).get(TIMEOUT, SECONDS);
    assertEquals(
      List.of(tx.name(), ty.name(), tz.name()),
      this.imageCaptionsAssignedNow()
    );

    this.model.imagesDelete(List.of(i0, i1)).get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.imageCaptionsAssignedNow());

    this.model.undo().get(TIMEOUT, SECONDS);
    this.model.imageSelect(Optional.of(i0)).get(TIMEOUT, SECONDS);
    assertEquals(
      List.of(tx.name(), ty.name(), tz.name()),
      this.imageCaptionsAssignedNow()
    );

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.imageCaptionsAssignedNow());
  }

  private List<LCaptionName> imageCaptionsAssignedNow()
  {
    return this.model.imageCaptionsAssigned()
      .get()
      .stream()
      .map(LCaption::name)
      .toList();
  }

  @Test
  public void testImageCaptionsAssignUnassignNotSelected()
    throws Exception
  {
    this.model.imageAdd(
      "image-a",
      this.imageFile,
      Optional.of(this.imageFile.toUri())
    ).get(TIMEOUT, SECONDS);

    this.model.imageAdd(
      "image-b",
      this.imageFile,
      Optional.of(this.imageFile.toUri())
    ).get(TIMEOUT, SECONDS);

    final var i0 = this.model.imageList().get().get(0).id();
    final var i1 = this.model.imageList().get().get(1).id();

    this.model.imageSelect(Optional.of(i1)).get(TIMEOUT, SECONDS);

    this.model.captionAdd(new LCaptionName("TX")).get(TIMEOUT, SECONDS);
    this.model.captionAdd(new LCaptionName("TY")).get(TIMEOUT, SECONDS);
    this.model.captionAdd(new LCaptionName("TZ")).get(TIMEOUT, SECONDS);

    final var tx = this.findCaption("TX");
    final var ty = this.findCaption("TY");

    final var assign1 = new LImageCaptionsAssignment(i1, Set.of(tx.id()));
    final var assign0 = new LImageCaptionsAssignment(i0, Set.of(ty.id()));

    this.model.imageCaptionsAssign(List.of(assign1)).get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.name()), this.imageCaptionsAssignedNow());

    this.model.imageCaptionsAssign(List.of(assign0)).get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.name()), this.imageCaptionsAssignedNow());

    this.model.imageCaptionsUnassign(List.of(assign0)).get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.name()), this.imageCaptionsAssignedNow());
  }

  @Test
  public void testMetadata()
    throws Exception
  {
    assertEquals(List.of(), this.model.metadataList().get());

    final var meta0 = new LMetadataValue("a", "x");
    final var meta1 = new LMetadataValue("b", "y");
    final var meta2 = new LMetadataValue("c", "z");
    final var meta3 = new LMetadataValue("c", "w");

    this.model.metadataPut(List.of(meta0, meta1, meta2)).get(TIMEOUT, SECONDS);
    assertEquals(List.of(meta0, meta1, meta2), this.model.metadataList().get());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.model.metadataList().get());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(meta0, meta1, meta2), this.model.metadataList().get());

    this.model.metadataRemove(List.of(meta0, meta1, meta2)).get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.model.metadataList().get());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(meta0, meta1, meta2), this.model.metadataList().get());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.model.metadataList().get());

    this.model.metadataPut(List.of(meta2)).get(TIMEOUT, SECONDS);
    this.model.metadataPut(List.of(meta3)).get(TIMEOUT, SECONDS);
    assertEquals(List.of(meta3), this.model.metadataList().get());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(meta2), this.model.metadataList().get());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(meta3), this.model.metadataList().get());
  }

  private LCategoryID findCategoryID(
    final String name)
  {
    return this.model.categoryList()
      .get()
      .stream()
      .filter(x -> Objects.equals(x.name().text(), name))
      .map(LCategory::id)
      .findFirst()
      .orElseThrow();
  }

  private LCategory findCategory(
    final LCategoryName name)
  {
    return this.model.categoryList()
      .get()
      .stream()
      .filter(x -> Objects.equals(x.name(), name))
      .findFirst()
      .orElseThrow();
  }

  private LCaption findCaption(
    final String name)
  {
    return this.model.captionList()
      .get()
      .stream()
      .filter(x -> Objects.equals(x.name().text(), name))
      .findFirst()
      .orElseThrow();
  }

  private List<LCaptionName> categoryCaptionsNow(
    final LCategoryID ca)
  {
    final var baseList =
      this.model.categoryCaptions()
        .get()
        .get(ca);

    if (baseList == null) {
      return List.of();
    }

    return baseList
      .stream()
      .map(LCaption::name)
      .toList();
  }
}
