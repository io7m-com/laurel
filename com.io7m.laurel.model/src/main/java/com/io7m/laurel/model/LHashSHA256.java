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

import java.util.regex.Pattern;

/**
 * A SHA-256 hash value.
 *
 * @param value The value
 */

public record LHashSHA256(
  String value)
  implements LHashType
{
  private static final Pattern VALID_HASH =
    Pattern.compile("[a-f0-9]{64}");

  /**
   * A SHA-256 hash value.
   *
   * @param value The value
   */

  public LHashSHA256
  {
    if (!VALID_HASH.matcher(value).matches()) {
      throw new IllegalArgumentException(
        "Hash values must match %s".formatted(VALID_HASH)
      );
    }
  }

  @Override
  public String name()
  {
    return "SHA-256";
  }
}
