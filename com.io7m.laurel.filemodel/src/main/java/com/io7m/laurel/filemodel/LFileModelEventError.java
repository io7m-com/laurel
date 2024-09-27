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


package com.io7m.laurel.filemodel;

import com.io7m.seltzer.api.SStructuredErrorType;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * A model event that represents an error.
 *
 * @param message           The message
 * @param progress          The progress
 * @param errorCode         The error code
 * @param attributes        The attributes
 * @param remediatingAction The remediating action
 * @param exception         The exception
 */

public record LFileModelEventError(
  String message,
  OptionalDouble progress,
  String errorCode,
  Map<String, String> attributes,
  Optional<String> remediatingAction,
  Optional<Throwable> exception)
  implements LFileModelEventType, SStructuredErrorType<String>
{
  /**
   * A model event that represents an error.
   *
   * @param message           The message
   * @param progress          The progress
   * @param errorCode         The error code
   * @param attributes        The attributes
   * @param remediatingAction The remediating action
   * @param exception         The exception
   */

  public LFileModelEventError
  {
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(progress, "progress");
    Objects.requireNonNull(errorCode, "errorCode");
    attributes = Map.copyOf(attributes);
    Objects.requireNonNull(remediatingAction, "remediatingAction");
    Objects.requireNonNull(exception, "exception");
  }
}
