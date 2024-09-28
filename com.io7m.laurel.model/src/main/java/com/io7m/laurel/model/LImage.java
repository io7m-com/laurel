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

import com.io7m.mime2045.core.MimeType;

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * An image.
 *
 * @param name   The name
 * @param file   The file
 * @param source The source
 * @param type   The MIME type
 * @param hash   The hash
 */

public record LImage(
  String name,
  Optional<Path> file,
  Optional<URI> source,
  MimeType type,
  LHashType hash)
{
  /**
   * An image.
   *
   * @param name   The name
   * @param file   The file
   * @param source The source
   * @param type   The MIME type
   * @param hash   The hash
   */

  public LImage
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(hash, "hash");
    Objects.requireNonNull(type, "type");
  }
}
