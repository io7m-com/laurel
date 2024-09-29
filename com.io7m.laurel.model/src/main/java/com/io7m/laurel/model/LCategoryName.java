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
 * A category.
 *
 * @param text The caption text
 */

public record LCategoryName(String text)
  implements Comparable<LCategoryName>
{
  /**
   * The pattern that defines a valid category.
   */

  public static final Pattern VALID_CATEGORY =
    Pattern.compile("[a-z0-9A-Z_-][a-z0-9A-Z_ \\-']*");

  /**
   * A category.
   *
   * @param text The caption text
   */

  public LCategoryName
  {
    Objects.requireNonNull(text, "text");
    text = text.trim();

    if (!VALID_CATEGORY.matcher(text).matches()) {
      throw new IllegalArgumentException(
        "Category must match %s".formatted(VALID_CATEGORY)
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
    final LCategoryName other)
  {
    return Comparator.comparing(LCategoryName::text)
      .compare(this, other);
  }
}
