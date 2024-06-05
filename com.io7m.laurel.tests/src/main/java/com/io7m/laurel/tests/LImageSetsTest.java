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

import com.io7m.laurel.io.LImageSets;
import com.io7m.laurel.model.LImage;
import com.io7m.laurel.model.LImageCaption;
import com.io7m.laurel.model.LImageCaptionID;
import com.io7m.laurel.model.LImageSet;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LImageSetsTest
{
  @Test
  public void testEmpty()
  {
    final var x =
      new LImageSet(List.of(), new TreeMap<>(), new TreeMap<>());
    final var y =
      new LImageSet(List.of(), new TreeMap<>(), new TreeMap<>());
    final var z =
      LImageSets.merge(x, y);

    assertEquals(List.of(), z.globalPrefixCaptions());
    assertEquals(new TreeMap<>(), z.images());
    assertEquals(new TreeMap<>(), z.captions());
  }

  /**
   * Two captions with the same IDs but different text become two new
   * captions.
   */

  @Test
  public void testCaptionXY0()
  {
    final var cap0 =
      new LImageCaption(
        new LImageCaptionID(UUID.randomUUID()),
        "text 0"
      );

    final var cap1 =
      new LImageCaption(
        cap0.id(),
        "text 1"
      );

    final var mx = new TreeMap<LImageCaptionID, LImageCaption>();
    mx.put(cap0.id(), cap0);
    final var my = new TreeMap<LImageCaptionID, LImageCaption>();
    my.put(cap1.id(), cap1);

    final var x = new LImageSet(List.of(), mx, new TreeMap<>());
    final var y = new LImageSet(List.of(), my, new TreeMap<>());
    final var z = LImageSets.merge(x, y);

    assertTrue(
      z.captions()
        .values()
        .stream()
        .anyMatch(c -> Objects.equals(c.text(), "text 0"))
    );
    assertTrue(
      z.captions()
        .values()
        .stream()
        .anyMatch(c -> Objects.equals(c.text(), "text 1"))
    );
    assertEquals(2, z.captions().size());

    assertEquals(List.of(), z.globalPrefixCaptions());
    assertEquals(new TreeMap<>(), z.images());
  }

  /**
   * Two captions with the same text but different IDs become two new
   * captions.
   */

  @Test
  public void testCaptionXY1()
  {
    final var cap0 =
      new LImageCaption(
        new LImageCaptionID(UUID.randomUUID()),
        "text 0"
      );

    final var cap1 =
      new LImageCaption(
        new LImageCaptionID(UUID.randomUUID()),
        "text 0"
      );

    assertNotEquals(cap0.id(), cap1.id());

    final var mx = new TreeMap<LImageCaptionID, LImageCaption>();
    mx.put(cap0.id(), cap0);
    final var my = new TreeMap<LImageCaptionID, LImageCaption>();
    my.put(cap1.id(), cap1);

    final var x = new LImageSet(List.of(), mx, new TreeMap<>());
    final var y = new LImageSet(List.of(), my, new TreeMap<>());
    final var z = LImageSets.merge(x, y);

    assertTrue(
      z.captions()
        .values()
        .stream()
        .anyMatch(c -> Objects.equals(c.text(), "text 0"))
    );
    assertEquals(1, z.captions().size());

    assertEquals(List.of(), z.globalPrefixCaptions());
    assertEquals(new TreeMap<>(), z.images());
  }

  /**
   * Merging image sets works.
   *
   * @param x A set
   * @param y A set
   */

  @Property
  public void testMergeXY(
    final @ForAll LImageSet x,
    final @ForAll LImageSet y)
  {
    final var z = LImageSets.merge(x, y);

    assertAll(
      x.globalPrefixCaptions()
        .stream()
        .map(text -> () -> {
          assertTrue(
            z.globalPrefixCaptions().contains(text),
            "Merge contains %s".formatted(text)
          );
        })
    );

    assertAll(
      y.globalPrefixCaptions()
        .stream()
        .map(text -> () -> {
          assertTrue(
            z.globalPrefixCaptions().contains(text),
            "Merge contains %s".formatted(text)
          );
        })
    );

    assertAll(
      x.captions()
        .values()
        .stream()
        .map(LImageCaption::text)
        .map(text -> () -> {
          assertTrue(
            containsCaptionText(z, text),
            "Merge contains %s".formatted(text)
          );
        })
    );

    assertAll(
      y.captions()
        .values()
        .stream()
        .map(LImageCaption::text)
        .map(text -> () -> {
          assertTrue(
            containsCaptionText(z, text),
            "Merge contains %s".formatted(text)
          );
        })
    );

    assertAll(
      x.images()
        .values()
        .stream()
        .map(LImage::fileName)
        .map(text -> () -> {
          assertTrue(
            containsFile(z, text),
            "Merge contains file %s".formatted(text)
          );
        })
    );

    assertAll(
      y.images()
        .values()
        .stream()
        .map(LImage::fileName)
        .map(text -> () -> {
          assertTrue(
            containsFile(z, text),
            "Merge contains file %s".formatted(text)
          );
        })
    );
  }

  private static boolean containsFile(
    final LImageSet set,
    final String text)
  {
    return set.images()
      .values()
      .stream()
      .anyMatch(i -> Objects.equals(i.fileName(), text));
  }

  private static boolean containsCaptionText(
    final LImageSet set,
    final String text)
  {
    return set.captions()
      .values()
      .stream()
      .anyMatch(c -> Objects.equals(c.text(), text));
  }
}
