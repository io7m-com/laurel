/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
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


package com.io7m.laurel.io;

import com.io7m.laurel.io.internal.LSerializer;
import com.io7m.laurel.writer.api.LSerializerFactoryType;
import com.io7m.laurel.writer.api.LSerializerType;

import java.io.OutputStream;
import java.net.URI;

/**
 * The default provider of manifest serializers.
 */

public final class LSerializers
  implements LSerializerFactoryType
{
  /**
   * The default provider of manifest serializers.
   */

  public LSerializers()
  {

  }

  @Override
  public LSerializerType createSerializerWithContext(
    final Void context,
    final URI target,
    final OutputStream stream)
  {
    return new LSerializer(stream);
  }
}
