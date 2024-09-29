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

import java.util.Comparator;
import java.util.Objects;

/**
 * A caption along with its ID.
 *
 * @param id    The ID
 * @param name  The caption
 * @param count The number of images to which the caption is assigned
 */

public record LCaption(
  LCaptionID id,
  LCaptionName name,
  long count)
  implements Comparable<LCaption>
{
  /**
   * A caption along with its ID.
   *
   * @param id    The ID
   * @param name  The caption
   * @param count The number of images to which the caption is assigned
   */

  public LCaption
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "tag");
  }

  @Override
  public int compareTo(
    final LCaption other)
  {
    return Comparator.comparing(LCaption::name)
      .thenComparing(LCaption::id)
      .compare(this, other);
  }
}
