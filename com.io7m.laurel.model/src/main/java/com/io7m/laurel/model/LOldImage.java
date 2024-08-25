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


package com.io7m.laurel.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An image.
 *
 * @param imageID  The image ID
 * @param fileName The file name
 * @param captions The captions
 */

public record LOldImage(
  LImageID imageID,
  String fileName,
  SortedSet<LImageCaptionID> captions)
  implements Comparable<LOldImage>
{
  /**
   * An image.
   *
   * @param imageID  The image ID
   * @param fileName The file name
   * @param captions The captions
   */

  public LOldImage
  {
    Objects.requireNonNull(imageID, "imageID");
    Objects.requireNonNull(fileName, "fileName");
    captions = Collections.unmodifiableSortedSet(new TreeSet<>(captions));
  }

  @Override
  public int compareTo(
    final LOldImage other)
  {
    return Comparator.comparing(LOldImage::fileName)
      .thenComparing(LOldImage::imageID)
      .compare(this, other);
  }
}
