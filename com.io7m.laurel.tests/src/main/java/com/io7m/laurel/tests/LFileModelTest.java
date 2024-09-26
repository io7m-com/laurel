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

import com.io7m.laurel.filemodel.LCategoryCaptionsAssignment;
import com.io7m.laurel.filemodel.LFileModelType;
import com.io7m.laurel.filemodel.LFileModels;
import com.io7m.laurel.filemodel.LImageCaptionsAssignment;
import com.io7m.laurel.gui.internal.LPerpetualSubscriber;
import com.io7m.laurel.model.LCaption;
import com.io7m.laurel.model.LCaptionID;
import com.io7m.laurel.model.LCaptionName;
import com.io7m.laurel.model.LCategory;
import com.io7m.laurel.model.LCategoryID;
import com.io7m.laurel.model.LCategoryName;
import com.io7m.laurel.model.LException;
import com.io7m.laurel.model.LGlobalCaption;
import com.io7m.laurel.model.LImageID;
import com.io7m.laurel.model.LImageWithID;
import com.io7m.laurel.model.LMetadataValue;
import com.io7m.zelador.test_extension.CloseableResourcesType;
import com.io7m.zelador.test_extension.ZeladorExtension;
import org.junit.jupiter.api.AfterEach;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

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

  @AfterEach
  public void tearDown()
    throws LException
  {
    this.model.close();
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
      assertThrows(ExecutionException.class, () -> {
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
      assertThrows(ExecutionException.class, () -> {
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

  @Test
  public void testCaptionModify()
    throws Exception
  {
    final var tta = new LCaptionName("A");

    this.model.captionAdd(tta).get(TIMEOUT, SECONDS);

    final var ta = this.findCaption("A");

    this.model.captionModify(ta.id(), new LCaptionName("B"))
      .get(TIMEOUT, SECONDS);

    final var tb = this.findCaption("B");

    assertEquals(ta.id(), tb.id());
    this.model.undo().get(TIMEOUT, SECONDS);
    this.model.redo().get(TIMEOUT, SECONDS);
  }

  @Test
  public void testCaptionModifyNonexistent()
    throws Exception
  {
    this.model.captionModify(new LCaptionID(1000L), new LCaptionName("B"))
      .get(TIMEOUT, SECONDS);

    assertEquals(List.of(), this.model.undoStack().get());
  }

  @Test
  public void testCaptionModifyConflict()
    throws Exception
  {
    final var tta = new LCaptionName("A");
    final var ttb = new LCaptionName("B");

    this.model.captionAdd(tta).get(TIMEOUT, SECONDS);
    this.model.captionAdd(ttb).get(TIMEOUT, SECONDS);

    final var ta = this.findCaption("A");
    final var tb = this.findCaption("B");

    final var ex =
      assertThrows(LException.class, () -> {
        try {
          this.model.captionModify(ta.id(), new LCaptionName("B"))
            .get(TIMEOUT, SECONDS);
        } catch (final ExecutionException e) {
          throw e.getCause();
        }
      });

    assertEquals("error-duplicate", ex.errorCode());
  }

  @Test
  public void testCaptionCopyPaste()
    throws Exception
  {
    final var tta = new LCaptionName("A");
    final var ttb = new LCaptionName("B");
    final var ttc = new LCaptionName("C");

    this.model.captionAdd(tta).get(TIMEOUT, SECONDS);
    this.model.captionAdd(ttb).get(TIMEOUT, SECONDS);
    this.model.captionAdd(ttc).get(TIMEOUT, SECONDS);

    final var ta = this.findCaption("A");
    final var tb = this.findCaption("B");
    final var tc = this.findCaption("C");

    this.model.captionsCopy(
      Set.of(ta, tb, tc)
        .stream()
        .map(LCaption::id)
        .collect(Collectors.toSet())
    );

    assertEquals(
      Set.of(ta.id(), tb.id(), tc.id()),
      this.model.captionClipboard().get()
    );

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

    final var images =
      this.model.imageList().get();

    this.model.captionsPaste(
      images.stream()
        .map(LImageWithID::id)
        .collect(Collectors.toSet())
    ).get(TIMEOUT, SECONDS);

    assertEquals(
      Set.of(),
      this.model.captionClipboard().get()
    );

    this.model.imageSelect(Optional.of(images.get(0).id()))
      .get(TIMEOUT, SECONDS);

    assertEquals(
      List.of(tta, ttb, ttc),
      this.imageCaptionsAssignedNow()
    );

    this.model.imageSelect(Optional.of(images.get(1).id()))
      .get(TIMEOUT, SECONDS);

    assertEquals(
      List.of(tta, ttb, ttc),
      this.imageCaptionsAssignedNow()
    );
  }

  @Test
  public void testCaptionFilter()
    throws Exception
  {
    final var tta = new LCaptionName("A");
    final var ttb = new LCaptionName("B");
    final var ttc = new LCaptionName("C");

    this.model.captionAdd(tta).get(TIMEOUT, SECONDS);
    this.model.captionAdd(ttb).get(TIMEOUT, SECONDS);
    this.model.captionAdd(ttc).get(TIMEOUT, SECONDS);

    final var ta = this.findCaption("A");
    final var tb = this.findCaption("B");
    final var tc = this.findCaption("C");

    this.model.imageAdd(
      "image-a",
      this.imageFile,
      Optional.of(this.imageFile.toUri())
    ).get(TIMEOUT, SECONDS);

    final var images =
      this.model.imageList().get();

    this.model.imageSelect(Optional.of(images.get(0).id()))
      .get(TIMEOUT, SECONDS);

    assertEquals(
      List.of(ta, tb, tc),
      this.model.imageCaptionsUnassignedFiltered().get()
    );

    this.model.captionsUnassignedListFilterSet("a");

    assertEquals(
      List.of(ta),
      this.model.imageCaptionsUnassignedFiltered().get()
    );

    this.model.captionsUnassignedListFilterSet("b");

    assertEquals(
      List.of(tb),
      this.model.imageCaptionsUnassignedFiltered().get()
    );

    this.model.captionsUnassignedListFilterSet("c");

    assertEquals(
      List.of(tc),
      this.model.imageCaptionsUnassignedFiltered().get()
    );

    this.model.captionsUnassignedListFilterSet("");

    assertEquals(
      List.of(ta, tb, tc),
      this.model.imageCaptionsUnassignedFiltered().get()
    );
  }

  @Test
  public void testImageFilter()
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

    this.model.imageAdd(
      "image-c",
      this.imageFile,
      Optional.of(this.imageFile.toUri())
    ).get(TIMEOUT, SECONDS);

    final var images =
      this.model.imageList().get();

    assertEquals(
      images,
      this.model.imageListFiltered().get()
    );

    this.model.imageListFilterSet("-a");

    assertEquals(
      List.of(images.get(0)),
      this.model.imageListFiltered().get()
    );

    this.model.imageListFilterSet("-b");

    assertEquals(
      List.of(images.get(1)),
      this.model.imageListFiltered().get()
    );

    this.model.imageListFilterSet("-c");

    assertEquals(
      List.of(images.get(2)),
      this.model.imageListFiltered().get()
    );

    this.model.imageListFilterSet("");

    assertEquals(
      images,
      this.model.imageListFiltered().get()
    );
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

    this.model.categorySetRequired(Set.of(ta.id(), tc.id())).get(
      TIMEOUT,
      SECONDS);
    assertEquals(
      List.of(ta.id(), tc.id()),
      this.categoriesRequiredIDNow()
    );

    // Redundant, won't be added to the undo stack.
    this.model.categorySetRequired(Set.of(ta.id(), tc.id())).get(
      TIMEOUT,
      SECONDS);
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
  public void testImageCaptionsDeletion()
    throws Exception
  {
    final var can = new LCategoryName("A");
    final var cbn = new LCategoryName("B");

    this.model.categoryAdd(can).get(TIMEOUT, SECONDS);
    this.model.categoryAdd(cbn).get(TIMEOUT, SECONDS);

    final var ca = this.findCategory(can);
    final var cb = this.findCategory(cbn);

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
    final var tz = this.findCaption("TZ");

    final var imageAssigns = List.of(
      new LImageCaptionsAssignment(i0, Set.of(tx.id(), ty.id())),
      new LImageCaptionsAssignment(i1, Set.of(ty.id(), tz.id()))
    );

    this.model.imageCaptionsAssign(imageAssigns).get(TIMEOUT, SECONDS);

    final var categoryAssigns =
      List.of(
        new LCategoryCaptionsAssignment(ca.id(), List.of(tx.id(), ty.id())),
        new LCategoryCaptionsAssignment(cb.id(), List.of(tz.id()))
      );

    this.model.categoryCaptionsAssign(categoryAssigns)
      .get(TIMEOUT, SECONDS);

    this.model.imageSelect(Optional.of(i0)).get(TIMEOUT, SECONDS);
    assertEquals(
      List.of(tx.name(), ty.name()),
      this.imageCaptionsAssignedNow());
    this.model.imageSelect(Optional.of(i1)).get(TIMEOUT, SECONDS);
    assertEquals(
      List.of(ty.name(), tz.name()),
      this.imageCaptionsAssignedNow());
    this.model.categorySelect(Optional.of(ca.id())).get(TIMEOUT, SECONDS);
    assertEquals(
      List.of(tx.name(), ty.name()),
      this.categoryCaptionsNow(ca.id()));
    this.model.categorySelect(Optional.of(cb.id())).get(TIMEOUT, SECONDS);
    assertEquals(List.of(tz.name()), this.categoryCaptionsNow(cb.id()));

    this.model.captionRemove(Set.of(tx.id(), ty.id(), tz.id()))
      .get(TIMEOUT, SECONDS);

    this.model.imageSelect(Optional.of(i0)).get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.imageCaptionsAssignedNow());
    this.model.imageSelect(Optional.of(i1)).get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.imageCaptionsAssignedNow());
    this.model.categorySelect(Optional.of(ca.id())).get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.categoryCaptionsNow(ca.id()));
    this.model.categorySelect(Optional.of(cb.id())).get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.categoryCaptionsNow(cb.id()));

    this.model.undo().get(TIMEOUT, SECONDS);

    this.model.imageSelect(Optional.of(i0)).get(TIMEOUT, SECONDS);
    assertEquals(
      List.of(tx.name(), ty.name()),
      this.imageCaptionsAssignedNow());
    this.model.imageSelect(Optional.of(i1)).get(TIMEOUT, SECONDS);
    assertEquals(
      List.of(ty.name(), tz.name()),
      this.imageCaptionsAssignedNow());
    this.model.categorySelect(Optional.of(ca.id())).get(TIMEOUT, SECONDS);
    assertEquals(
      List.of(tx.name(), ty.name()),
      this.categoryCaptionsNow(ca.id()));
    this.model.categorySelect(Optional.of(cb.id())).get(TIMEOUT, SECONDS);
    assertEquals(List.of(tz.name()), this.categoryCaptionsNow(cb.id()));

    this.model.redo().get(TIMEOUT, SECONDS);

    this.model.imageSelect(Optional.of(i0)).get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.imageCaptionsAssignedNow());
    this.model.imageSelect(Optional.of(i1)).get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.imageCaptionsAssignedNow());
    this.model.categorySelect(Optional.of(ca.id())).get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.categoryCaptionsNow(ca.id()));
    this.model.categorySelect(Optional.of(cb.id())).get(TIMEOUT, SECONDS);
    assertEquals(List.of(), this.categoryCaptionsNow(cb.id()));
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
        i1,
        Set.of(ty.id())
      );

    this.model.imageSelect(Optional.of(i0)).get(TIMEOUT, SECONDS);
    this.model.imageCaptionsAssign(List.of(assign0, assign1)).get(
      TIMEOUT,
      SECONDS);
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

  @Test
  public void testImageComparison()
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
        i1,
        Set.of(ty.id())
      );

    this.model.imageCaptionsAssign(List.of(assign0, assign1))
      .get(TIMEOUT, SECONDS);

    this.model.imagesCompare(i0, i1)
      .get(TIMEOUT, SECONDS);

    assertEquals(
      i0, this.model.imageComparison().get().orElseThrow().imageA().id()
    );
    assertEquals(
      i1, this.model.imageComparison().get().orElseThrow().imageB().id()
    );

    /*
     * Image A has two captions that image B doesn't have.
     */

    assertEquals(
      List.of(
        tx.id(),
        tz.id()
      ),
      this.model.imageComparisonA()
        .get()
        .stream()
        .map(LCaption::id)
        .toList()
    );

    assertEquals(
      List.of(),
      this.model.imageComparisonB()
        .get()
        .stream()
        .map(LCaption::id)
        .toList()
    );

    /*
     * Now, assign one of the captions to image B...
     */

    this.model.imageCaptionsAssign(
        List.of(new LImageCaptionsAssignment(i1, Set.of(tz.id()))))
      .get(TIMEOUT, SECONDS);

    /*
     * Now image A only has one caption that image B doesn't have.
     */

    assertEquals(
      List.of(
        tx.id()
      ),
      this.model.imageComparisonA()
        .get()
        .stream()
        .map(LCaption::id)
        .toList()
    );

    assertEquals(
      List.of(),
      this.model.imageComparisonB()
        .get()
        .stream()
        .map(LCaption::id)
        .toList()
    );
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

    this.model.metadataRemove(List.of(meta0, meta1, meta2)).get(
      TIMEOUT,
      SECONDS);
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

  @Test
  public void testGlobalCaptions()
    throws Exception
  {
    final var txn = new LCaptionName("TX");
    final var tyn = new LCaptionName("TY");
    final var tzn = new LCaptionName("TZ");

    assertEquals(List.of(), this.globalCaptionsNow());

    this.model.globalCaptionAdd(txn).get(TIMEOUT, SECONDS);
    final var tx = this.findGlobalCaption(txn);
    assertEquals(List.of(tx.id()), this.globalCaptionsNow());

    this.model.globalCaptionAdd(tyn).get(TIMEOUT, SECONDS);
    final var ty = this.findGlobalCaption(tyn);
    assertEquals(List.of(tx.id(), ty.id()), this.globalCaptionsNow());

    this.model.globalCaptionAdd(tzn).get(TIMEOUT, SECONDS);
    final var tz = this.findGlobalCaption(tzn);
    assertEquals(List.of(tx.id(), ty.id(), tz.id()), this.globalCaptionsNow());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.id(), ty.id()), this.globalCaptionsNow());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.id(), ty.id(), tz.id()), this.globalCaptionsNow());

    this.model.globalCaptionRemove(tz.id()).get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.id(), ty.id()), this.globalCaptionsNow());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.id(), ty.id(), tz.id()), this.globalCaptionsNow());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.id(), ty.id()), this.globalCaptionsNow());
  }

  @Test
  public void testGlobalCaptionsOrderLower()
    throws Exception
  {
    final var txn = new LCaptionName("TX");
    final var tyn = new LCaptionName("TY");
    final var tzn = new LCaptionName("TZ");

    assertEquals(List.of(), this.globalCaptionsNow());

    this.model.globalCaptionAdd(txn).get(TIMEOUT, SECONDS);
    this.model.globalCaptionAdd(tyn).get(TIMEOUT, SECONDS);
    this.model.globalCaptionAdd(tzn).get(TIMEOUT, SECONDS);

    final var tx = this.findGlobalCaption(txn);
    final var ty = this.findGlobalCaption(tyn);
    final var tz = this.findGlobalCaption(tzn);

    this.model.globalCaptionOrderLower(tx.id()).get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.id(), ty.id(), tz.id()), this.globalCaptionsNow());

    this.model.globalCaptionOrderLower(ty.id()).get(TIMEOUT, SECONDS);
    assertEquals(List.of(ty.id(), tx.id(), tz.id()), this.globalCaptionsNow());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.id(), ty.id(), tz.id()), this.globalCaptionsNow());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(ty.id(), tx.id(), tz.id()), this.globalCaptionsNow());
  }

  @Test
  public void testGlobalCaptionsOrderUpper()
    throws Exception
  {
    final var txn = new LCaptionName("TX");
    final var tyn = new LCaptionName("TY");
    final var tzn = new LCaptionName("TZ");

    assertEquals(List.of(), this.globalCaptionsNow());

    this.model.globalCaptionAdd(txn).get(TIMEOUT, SECONDS);
    this.model.globalCaptionAdd(tyn).get(TIMEOUT, SECONDS);
    this.model.globalCaptionAdd(tzn).get(TIMEOUT, SECONDS);

    final var tx = this.findGlobalCaption(txn);
    final var ty = this.findGlobalCaption(tyn);
    final var tz = this.findGlobalCaption(tzn);

    this.model.globalCaptionOrderUpper(tz.id()).get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.id(), ty.id(), tz.id()), this.globalCaptionsNow());

    this.model.globalCaptionOrderUpper(ty.id()).get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.id(), tz.id(), ty.id()), this.globalCaptionsNow());

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.id(), ty.id(), tz.id()), this.globalCaptionsNow());

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(List.of(tx.id(), tz.id(), ty.id()), this.globalCaptionsNow());
  }

  @Test
  public void testGlobalCaptionsModify()
    throws Exception
  {
    final var txn = new LCaptionName("TX");

    assertEquals(List.of(), this.globalCaptionsNow());

    this.model.globalCaptionAdd(txn).get(TIMEOUT, SECONDS);

    final var tx = this.findGlobalCaption(txn);

    this.model.globalCaptionModify(tx.id(), new LCaptionName("ZZZ"))
      .get(TIMEOUT, SECONDS);
    assertEquals(
      "ZZZ",
      this.model.globalCaptionList().get().get(0).caption().name().text()
    );

    this.model.undo().get(TIMEOUT, SECONDS);
    assertEquals(
      "TX",
      this.model.globalCaptionList().get().get(0).caption().name().text()
    );

    this.model.redo().get(TIMEOUT, SECONDS);
    assertEquals(
      "ZZZ",
      this.model.globalCaptionList().get().get(0).caption().name().text()
    );
  }

  @Test
  public void testGlobalCaptionsModifyCollision()
    throws Exception
  {
    final var txn = new LCaptionName("TX");
    final var tyn = new LCaptionName("TY");

    assertEquals(List.of(), this.globalCaptionsNow());

    this.model.globalCaptionAdd(txn).get(TIMEOUT, SECONDS);
    this.model.globalCaptionAdd(tyn).get(TIMEOUT, SECONDS);

    final var tx = this.findGlobalCaption(txn);
    final var ty = this.findGlobalCaption(tyn);

    final var ex = assertThrows(LException.class, () -> {
      try {
        this.model.globalCaptionModify(tx.id(), new LCaptionName("TY"))
          .get(TIMEOUT, SECONDS);
      } catch (final ExecutionException e) {
        throw e.getCause();
      }
    });

    assertEquals("error-duplicate", ex.errorCode());
  }

  @Test
  public void testGlobalCaptionsModifyNonexistent()
    throws Exception
  {
    final var txn = new LCaptionName("TX");

    assertEquals(List.of(), this.globalCaptionsNow());

    this.model.globalCaptionAdd(txn).get(TIMEOUT, SECONDS);

    final var tx0 = this.findGlobalCaption(txn);

    this.model.globalCaptionModify(
        new LCaptionID(1000L),
        new LCaptionName("TY"))
      .get(TIMEOUT, SECONDS);

    final var tx1 = this.findGlobalCaption(txn);
    assertEquals(tx0, tx1);
  }

  private List<LCaptionID> globalCaptionsNow()
  {
    return this.model.globalCaptionList()
      .get()
      .stream()
      .map(LGlobalCaption::caption)
      .map(LCaption::id)
      .toList();
  }

  private LCaption findGlobalCaption(
    final LCaptionName name)
  {
    return this.model.globalCaptionList()
      .get()
      .stream()
      .map(LGlobalCaption::caption)
      .filter(x -> Objects.equals(x.name(), name))
      .findFirst()
      .orElseThrow();
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
