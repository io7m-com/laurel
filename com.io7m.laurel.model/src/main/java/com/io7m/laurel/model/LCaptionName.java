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
import java.util.regex.Pattern;

/**
 * A caption.
 *
 * @param text The caption text
 */

public record LCaptionName(String text)
  implements Comparable<LCaptionName>
{
  /**
   * The pattern that defines a valid caption.
   */

  public static final Pattern VALID_CAPTION =
    Pattern.compile("[a-z0-9A-Z_-][a-z0-9A-Z_ \\-']*");

  /**
   * A caption.
   *
   * @param text The caption text
   */

  public LCaptionName
  {
    Objects.requireNonNull(text, "text");
    text = text.trim();

    if (!VALID_CAPTION.matcher(text).matches()) {
      throw new IllegalArgumentException(
        "Caption must match %s".formatted(VALID_CAPTION)
      );
    }
  }

  @Override
  public String toString()
  {
    return this.text;
  }

  @Override
  public int compareTo(
    final LCaptionName other)
  {
    return Comparator.comparing(LCaptionName::text)
      .compare(this, other);
  }
}
