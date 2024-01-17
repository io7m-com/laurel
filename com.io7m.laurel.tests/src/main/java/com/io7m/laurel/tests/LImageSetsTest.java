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
import com.io7m.laurel.model.LImageSets;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class LImageSetsTest
{
  @Test
  public void testCaptionAdd()
  {
    final var i = LImageSets.empty();

    final var cap0 =
      new LImageCaption(
        new LImageCaptionID(UUID.randomUUID()), "Caption 0");
    final var cap1 =
      new LImageCaption(
        cap0.id(), "Caption 1");

    final var cmd0 = i.putCaption(cap0);
    cmd0.execute();
    assertEquals(cap0, i.captions().get(cap0.id()));
    cmd0.undo();
    assertNull(i.captions().get(cap0.id()));
    cmd0.execute();
    assertEquals(cap0, i.captions().get(cap0.id()));

    final var cmd1 = i.putCaption(cap1);
    cmd1.execute();
    assertEquals(cap1, i.captions().get(cap1.id()));
    cmd1.undo();
    assertEquals(cap0, i.captions().get(cap0.id()));
  }

  @Test
  public void testCaptionRemove()
  {
    final var i = LImageSets.empty();

    final var cap0 =
      new LImageCaption(
        new LImageCaptionID(UUID.randomUUID()), "Caption 0");

    final var cmd0 = i.putCaption(cap0);
    cmd0.execute();
    assertEquals(cap0, i.captions().get(cap0.id()));
    cmd0.undo();
    assertNull(i.captions().get(cap0.id()));
    cmd0.execute();
    assertEquals(cap0, i.captions().get(cap0.id()));

    final var cmd1 = i.removeCaption(cap0.id());
    cmd1.execute();
    assertNull(i.captions().get(cap0.id()));
    cmd1.undo();
    assertEquals(cap0, i.captions().get(cap0.id()));
  }

  @Test
  public void testImageAdd()
  {
    final var i = LImageSets.empty();

    final var cap0 =
      new LImageCaption(
        new LImageCaptionID(UUID.randomUUID()), "Caption 0");
    final var cap1 =
      new LImageCaption(
        new LImageCaptionID(UUID.randomUUID()), "Caption 0");
    final var cap2 =
      new LImageCaption(
        new LImageCaptionID(UUID.randomUUID()), "Caption 0");

    final var image0 =
      new LImage(
        new LImageID(UUID.randomUUID()),
        "File.png",
        List.of(
          cap0.id(),
          cap1.id(),
          cap2.id()
        )
      );

    assertThrows(IllegalArgumentException.class, () -> {
      i.putImage(image0).execute();
    });

    i.putCaption(cap0).execute();
    i.putCaption(cap1).execute();
    i.putCaption(cap2).execute();

    final var cmd0 = i.putImage(image0);
    cmd0.execute();
    assertEquals(image0, i.images().get(image0.imageID()));
    cmd0.undo();
    assertNull(i.images().get(image0.imageID()));
  }

  @Test
  public void testImageAddRemoveCaption()
  {
    final var i = LImageSets.empty();

    final var cap0 =
      new LImageCaption(
        new LImageCaptionID(UUID.randomUUID()), "Caption 0");
    final var cap1 =
      new LImageCaption(
        new LImageCaptionID(UUID.randomUUID()), "Caption 0");
    final var cap2 =
      new LImageCaption(
        new LImageCaptionID(UUID.randomUUID()), "Caption 0");

    final var image0 =
      new LImage(
        new LImageID(UUID.randomUUID()),
        "File.png",
        List.of(
          cap0.id(),
          cap1.id(),
          cap2.id()
        )
      );

    final var image0WithoutCaption =
      new LImage(
        image0.imageID(),
        image0.fileName(),
        List.of(
          cap1.id(),
          cap2.id()
        )
      );

    i.putCaption(cap0).execute();
    i.putCaption(cap1).execute();
    i.putCaption(cap2).execute();
    i.putImage(image0).execute();

    final var cmd0 = i.removeCaption(cap0.id());
    cmd0.execute();
    assertNull(i.captions().get(cap0.id()));
    assertEquals(image0WithoutCaption, i.images().get(image0.imageID()));
    cmd0.undo();
    assertEquals(cap0, i.captions().get(cap0.id()));
    assertEquals(image0, i.images().get(image0.imageID()));
  }

  @Test
  public void testImageUpdate()
  {
    final var i = LImageSets.empty();

    final var cap0 =
      new LImageCaption(
        new LImageCaptionID(UUID.randomUUID()), "Caption 0");
    final var cap1 =
      new LImageCaption(
        new LImageCaptionID(UUID.randomUUID()), "Caption 0");
    final var cap2 =
      new LImageCaption(
        new LImageCaptionID(UUID.randomUUID()), "Caption 0");

    final var image0 =
      new LImage(
        new LImageID(UUID.randomUUID()),
        "File.png",
        List.of(
          cap0.id(),
          cap1.id(),
          cap2.id()
        )
      );

    final var image0WithoutCaption =
      new LImage(
        image0.imageID(),
        image0.fileName(),
        List.of(
          cap1.id(),
          cap2.id()
        )
      );

    i.putCaption(cap0).execute();
    i.putCaption(cap1).execute();
    i.putCaption(cap2).execute();
    i.putImage(image0).execute();

    final var cmd0 = i.putImage(image0WithoutCaption);
    cmd0.execute();
    assertEquals(image0WithoutCaption, i.images().get(image0.imageID()));
    cmd0.undo();
    assertEquals(image0, i.images().get(image0.imageID()));
  }
}
