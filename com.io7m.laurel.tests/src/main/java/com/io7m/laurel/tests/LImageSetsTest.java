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

import com.io7m.laurel.model.LImage;
import com.io7m.laurel.model.LImageCaption;
import com.io7m.laurel.model.LImageCaptionID;
import com.io7m.laurel.model.LImageID;
import com.io7m.laurel.model.LImageSetCommandException;
import com.io7m.laurel.model.LImageSets;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class LImageSetsTest
{
  private static final LImageCaption CAP_0 =
    new LImageCaption(
      new LImageCaptionID(UUID.randomUUID()), "Caption 0");

  private static final LImageCaption CAP_1 =
    new LImageCaption(
      new LImageCaptionID(UUID.randomUUID()), "Caption 1");

  private static final LImageCaption CAP_2 =
    new LImageCaption(
      new LImageCaptionID(UUID.randomUUID()), "Caption 2");

  @Test
  public void testCaptionAdd()
    throws Exception
  {
    final var i = LImageSets.empty();

    final var cap1 =
      new LImageCaption(
        CAP_0.id(), "Caption 1");

    final var cmd0 = i.captionUpdate(CAP_0);
    cmd0.execute();
    assertEquals(0L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(CAP_0, i.captions().get(CAP_0.id()));
    cmd0.undo();
    assertEquals(0L, i.captionAssignmentCount(CAP_0.id()));
    assertNull(i.captions().get(CAP_0.id()));
    cmd0.execute();
    assertEquals(0L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(CAP_0, i.captions().get(CAP_0.id()));

    final var cmd1 = i.captionUpdate(cap1);
    cmd1.execute();
    assertEquals(0L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(cap1, i.captions().get(cap1.id()));
    cmd1.undo();
    assertEquals(0L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(CAP_0, i.captions().get(CAP_0.id()));
  }

  @Test
  public void testCaptionAddConflict()
    throws Exception
  {
    final var i = LImageSets.empty();

    final var cap1 =
      new LImageCaption(CAP_1.id(), "Caption 0");

    final var cmd0 = i.captionUpdate(CAP_0);
    cmd0.execute();
    assertEquals(0L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(CAP_0, i.captions().get(CAP_0.id()));
    cmd0.undo();
    assertEquals(0L, i.captionAssignmentCount(CAP_0.id()));
    assertNull(i.captions().get(CAP_0.id()));
    cmd0.execute();
    assertEquals(0L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(CAP_0, i.captions().get(CAP_0.id()));

    final var cmd1 = i.captionUpdate(cap1);
    assertThrows(LImageSetCommandException.class, cmd1::execute);
    assertThrows(LImageSetCommandException.class, cmd1::undo);
  }

  @Test
  public void testCaptionRemove()
    throws Exception
  {
    final var i = LImageSets.empty();

    final var cmd0 = i.captionUpdate(CAP_0);
    cmd0.execute();
    assertEquals(0L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(CAP_0, i.captions().get(CAP_0.id()));
    cmd0.undo();
    assertEquals(0L, i.captionAssignmentCount(CAP_0.id()));
    assertNull(i.captions().get(CAP_0.id()));
    cmd0.execute();
    assertEquals(0L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(CAP_0, i.captions().get(CAP_0.id()));

    final var cmd1 = i.captionRemove(CAP_0.id());
    cmd1.execute();
    assertEquals(0L, i.captionAssignmentCount(CAP_0.id()));
    assertNull(i.captions().get(CAP_0.id()));
    cmd1.undo();
    assertEquals(0L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(CAP_0, i.captions().get(CAP_0.id()));
  }

  @Test
  public void testImageAdd()
    throws Exception
  {
    final var i = LImageSets.empty();

    final var image0 =
      new LImage(
        new LImageID(UUID.randomUUID()),
        "File.png",
        List.of(
          CAP_0.id(),
          CAP_1.id(),
          CAP_2.id()
        )
      );

    assertThrows(LImageSetCommandException.class, () -> {
      i.imageUpdate(image0).execute();
    });

    i.captionUpdate(CAP_0).execute();
    i.captionUpdate(CAP_1).execute();
    i.captionUpdate(CAP_2).execute();

    final var cmd0 = i.imageUpdate(image0);
    cmd0.execute();
    assertEquals(1L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_1.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_2.id()));
    assertEquals(image0, i.images().get(image0.imageID()));
    cmd0.undo();
    assertNull(i.images().get(image0.imageID()));
    assertEquals(0L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(0L, i.captionAssignmentCount(CAP_1.id()));
    assertEquals(0L, i.captionAssignmentCount(CAP_2.id()));
  }

  @Test
  public void testImageAddRemoveCaption()
    throws Exception
  {
    final var i = LImageSets.empty();

    final var image0 =
      new LImage(
        new LImageID(UUID.randomUUID()),
        "File.png",
        List.of(
          CAP_0.id(),
          CAP_1.id(),
          CAP_2.id()
        )
      );

    final var image0WithoutCaption =
      new LImage(
        image0.imageID(),
        image0.fileName(),
        List.of(
          CAP_1.id(),
          CAP_2.id()
        )
      );

    i.captionUpdate(CAP_0).execute();
    i.captionUpdate(CAP_1).execute();
    i.captionUpdate(CAP_2).execute();
    i.imageUpdate(image0).execute();

    final var cmd0 = i.captionRemove(CAP_0.id());
    cmd0.execute();
    assertEquals(0L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_1.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_2.id()));
    assertNull(i.captions().get(CAP_0.id()));
    assertEquals(image0WithoutCaption, i.images().get(image0.imageID()));
    cmd0.undo();
    assertEquals(1L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_1.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_2.id()));
    assertEquals(CAP_0, i.captions().get(CAP_0.id()));
    assertEquals(image0, i.images().get(image0.imageID()));
  }

  @Test
  public void testImageUpdate()
    throws Exception
  {
    final var i = LImageSets.empty();

    final var image0 =
      new LImage(
        new LImageID(UUID.randomUUID()),
        "File.png",
        List.of(
          CAP_0.id(),
          CAP_1.id(),
          CAP_2.id()
        )
      );

    final var image0WithoutCaption =
      new LImage(
        image0.imageID(),
        image0.fileName(),
        List.of(
          CAP_1.id(),
          CAP_2.id()
        )
      );

    i.captionUpdate(CAP_0).execute();
    i.captionUpdate(CAP_1).execute();
    i.captionUpdate(CAP_2).execute();
    i.imageUpdate(image0).execute();

    final var cmd0 = i.imageUpdate(image0WithoutCaption);
    cmd0.execute();
    assertEquals(0L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_1.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_2.id()));
    assertEquals(image0WithoutCaption, i.images().get(image0.imageID()));
    cmd0.undo();
    assertEquals(1L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_1.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_2.id()));
    assertEquals(image0, i.images().get(image0.imageID()));
  }

  @Test
  public void testCaptionPriorityDecrease0()
    throws Exception
  {
    final var i = LImageSets.empty();

    final var image0 =
      new LImage(
        new LImageID(UUID.randomUUID()),
        "File.png",
        List.of(
          CAP_0.id(),
          CAP_1.id(),
          CAP_2.id()
        )
      );

    final var image0Reordered =
      new LImage(
        image0.imageID(),
        image0.fileName(),
        List.of(
          CAP_1.id(),
          CAP_0.id(),
          CAP_2.id()
        )
      );

    i.captionUpdate(CAP_0).execute();
    i.captionUpdate(CAP_1).execute();
    i.captionUpdate(CAP_2).execute();
    i.imageUpdate(image0).execute();

    final var cmd0 = i.captionPriorityDecrease(image0.imageID(), CAP_0.id());
    cmd0.execute();
    assertEquals(1L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_1.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_2.id()));
    assertEquals(image0Reordered, i.images().get(image0.imageID()));
    cmd0.undo();
    assertEquals(1L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_1.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_2.id()));
    assertEquals(image0, i.images().get(image0.imageID()));
  }

  @Test
  public void testCaptionPriorityDecrease1()
    throws Exception
  {
    final var i = LImageSets.empty();

    final var image0 =
      new LImage(
        new LImageID(UUID.randomUUID()),
        "File.png",
        List.of(
          CAP_0.id(),
          CAP_1.id(),
          CAP_2.id()
        )
      );

    final var image0Reordered =
      new LImage(
        image0.imageID(),
        image0.fileName(),
        List.of(
          CAP_0.id(),
          CAP_1.id(),
          CAP_2.id()
        )
      );

    i.captionUpdate(CAP_0).execute();
    i.captionUpdate(CAP_1).execute();
    i.captionUpdate(CAP_2).execute();
    i.imageUpdate(image0).execute();

    final var cmd0 = i.captionPriorityDecrease(image0.imageID(), CAP_2.id());
    cmd0.execute();
    assertEquals(1L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_1.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_2.id()));
    assertEquals(image0Reordered, i.images().get(image0.imageID()));
    cmd0.undo();
    assertEquals(1L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_1.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_2.id()));
    assertEquals(image0, i.images().get(image0.imageID()));
  }

  @Test
  public void testCaptionPriorityIncrease0()
    throws Exception
  {
    final var i = LImageSets.empty();

    final var image0 =
      new LImage(
        new LImageID(UUID.randomUUID()),
        "File.png",
        List.of(
          CAP_0.id(),
          CAP_1.id(),
          CAP_2.id()
        )
      );

    final var image0Reordered =
      new LImage(
        image0.imageID(),
        image0.fileName(),
        List.of(
          CAP_1.id(),
          CAP_0.id(),
          CAP_2.id()
        )
      );

    i.captionUpdate(CAP_0).execute();
    i.captionUpdate(CAP_1).execute();
    i.captionUpdate(CAP_2).execute();
    i.imageUpdate(image0).execute();

    final var cmd0 = i.captionPriorityIncrease(image0.imageID(), CAP_1.id());
    cmd0.execute();
    assertEquals(1L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_1.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_2.id()));
    assertEquals(image0Reordered, i.images().get(image0.imageID()));
    cmd0.undo();
    assertEquals(1L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_1.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_2.id()));
    assertEquals(image0, i.images().get(image0.imageID()));
  }

  @Test
  public void testCaptionPriorityIncrease1()
    throws Exception
  {
    final var i = LImageSets.empty();

    final var image0 =
      new LImage(
        new LImageID(UUID.randomUUID()),
        "File.png",
        List.of(
          CAP_0.id(),
          CAP_1.id(),
          CAP_2.id()
        )
      );

    final var image0Reordered =
      new LImage(
        image0.imageID(),
        image0.fileName(),
        List.of(
          CAP_0.id(),
          CAP_1.id(),
          CAP_2.id()
        )
      );

    i.captionUpdate(CAP_0).execute();
    i.captionUpdate(CAP_1).execute();
    i.captionUpdate(CAP_2).execute();
    i.imageUpdate(image0).execute();

    final var cmd0 = i.captionPriorityIncrease(image0.imageID(), CAP_0.id());
    cmd0.execute();
    assertEquals(1L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_1.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_2.id()));
    assertEquals(image0Reordered, i.images().get(image0.imageID()));
    cmd0.undo();
    assertEquals(1L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_1.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_2.id()));
    assertEquals(image0, i.images().get(image0.imageID()));
  }

  @Test
  public void testCaptionUnassign()
    throws Exception
  {
    final var i = LImageSets.empty();

    final var image0 =
      new LImage(
        new LImageID(UUID.randomUUID()),
        "File.png",
        List.of(
          CAP_0.id(),
          CAP_1.id(),
          CAP_2.id()
        )
      );

    final var image0After =
      new LImage(
        image0.imageID(),
        image0.fileName(),
        List.of(
          CAP_0.id(),
          CAP_2.id()
        )
      );

    i.captionUpdate(CAP_0).execute();
    i.captionUpdate(CAP_1).execute();
    i.captionUpdate(CAP_2).execute();
    i.imageUpdate(image0).execute();

    final var cmd0 = i.captionUnassign(image0.imageID(), CAP_1.id());
    cmd0.execute();
    assertEquals(1L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(0L, i.captionAssignmentCount(CAP_1.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_2.id()));
    assertEquals(image0After, i.images().get(image0.imageID()));
    cmd0.undo();
    assertEquals(1L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_1.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_2.id()));
    assertEquals(image0, i.images().get(image0.imageID()));
  }

  @Test
  public void testCaptionAssign()
    throws Exception
  {
    final var i = LImageSets.empty();

    final var image0 =
      new LImage(
        new LImageID(UUID.randomUUID()),
        "File.png",
        List.of(
          CAP_0.id(),
          CAP_2.id()
        )
      );

    final var image0After =
      new LImage(
        image0.imageID(),
        image0.fileName(),
        List.of(
          CAP_1.id(),
          CAP_0.id(),
          CAP_2.id()
        )
      );

    i.captionUpdate(CAP_0).execute();
    i.captionUpdate(CAP_1).execute();
    i.captionUpdate(CAP_2).execute();
    i.imageUpdate(image0).execute();

    final var cmd0 = i.captionAssign(image0.imageID(), CAP_1.id());
    cmd0.execute();
    assertEquals(1L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_1.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_2.id()));
    assertEquals(image0After, i.images().get(image0.imageID()));
    cmd0.undo();
    assertEquals(1L, i.captionAssignmentCount(CAP_0.id()));
    assertEquals(0L, i.captionAssignmentCount(CAP_1.id()));
    assertEquals(1L, i.captionAssignmentCount(CAP_2.id()));
    assertEquals(image0, i.images().get(image0.imageID()));
  }
}
