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

import com.io7m.seltzer.api.SStructuredErrorExceptionType;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The type of exceptions raised by the package.
 */

public class LException
  extends Exception
  implements SStructuredErrorExceptionType<String>
{
  private final String errorCode;
  private final Map<String, String> attributes;
  private final Optional<String> remediatingAction;

  /**
   * Construct an exception.
   *
   * @param cause               The cause
   * @param inErrorCode         The error code
   * @param inAttributes        The attributes
   * @param inRemediatingAction The remediating action
   */

  public LException(
    final Throwable cause,
    final String inErrorCode,
    final Map<String, String> inAttributes,
    final Optional<String> inRemediatingAction)
  {
    super(
      Objects.requireNonNullElse(
        cause.getMessage(),
        cause.getClass().getSimpleName()
      ),
      Objects.requireNonNull(cause, "cause")
    );

    this.errorCode =
      Objects.requireNonNull(inErrorCode, "errorCode");
    this.attributes =
      Map.copyOf(inAttributes);
    this.remediatingAction =
      Objects.requireNonNull(inRemediatingAction, "remediatingAction");
  }

  /**
   * Construct an exception.
   *
   * @param message             The message
   * @param inErrorCode         The error code
   * @param inAttributes        The attributes
   * @param inRemediatingAction The remediating action
   */

  public LException(
    final String message,
    final String inErrorCode,
    final Map<String, String> inAttributes,
    final Optional<String> inRemediatingAction)
  {
    super(Objects.requireNonNull(message, "message"));

    this.errorCode =
      Objects.requireNonNull(inErrorCode, "errorCode");
    this.attributes =
      Map.copyOf(inAttributes);
    this.remediatingAction =
      Objects.requireNonNull(inRemediatingAction, "remediatingAction");
  }

  /**
   * Construct an exception.
   *
   * @param message             The message
   * @param cause               The cause
   * @param inErrorCode         The error code
   * @param inAttributes        The attributes
   * @param inRemediatingAction The remediating action
   */

  public LException(
    final String message,
    final Throwable cause,
    final String inErrorCode,
    final Map<String, String> inAttributes,
    final Optional<String> inRemediatingAction)
  {
    super(
      Objects.requireNonNull(message, "message"),
      Objects.requireNonNull(cause, "cause")
    );

    this.errorCode =
      Objects.requireNonNull(inErrorCode, "errorCode");
    this.attributes =
      Map.copyOf(inAttributes);
    this.remediatingAction =
      Objects.requireNonNull(inRemediatingAction, "remediatingAction");
  }

  /**
   * Construct an exception.
   *
   * @param message     The message
   * @param cause       The cause
   * @param inErrorCode The error code
   */

  public LException(
    final String message,
    final Throwable cause,
    final String inErrorCode)
  {
    this(message, cause, inErrorCode, Map.of(), Optional.empty());
  }

  /**
   * Construct an exception.
   *
   * @param message      The message
   * @param cause        The cause
   * @param inErrorCode  The error code
   * @param inAttributes The attributes
   */

  public LException(
    final String message,
    final Throwable cause,
    final String inErrorCode,
    final Map<String, String> inAttributes)
  {
    this(message, cause, inErrorCode, inAttributes, Optional.empty());
  }

  /**
   * Construct an exception.
   *
   * @param message             The message
   * @param cause               The cause
   * @param inErrorCode         The error code
   * @param inRemediatingAction The remediating action
   */

  public LException(
    final String message,
    final Throwable cause,
    final String inErrorCode,
    final Optional<String> inRemediatingAction)
  {
    this(message, cause, inErrorCode, Map.of(), inRemediatingAction);
  }

  /**
   * Construct an exception.
   *
   * @param cause               The cause
   * @param inErrorCode         The error code
   * @param inRemediatingAction The remediating action
   */

  public LException(
    final Throwable cause,
    final String inErrorCode,
    final Optional<String> inRemediatingAction)
  {
    this(cause, inErrorCode, Map.of(), inRemediatingAction);
  }

  /**
   * Construct an exception.
   *
   * @param message             The message
   * @param inErrorCode         The error code
   * @param inRemediatingAction The remediating action
   */

  public LException(
    final String message,
    final String inErrorCode,
    final Optional<String> inRemediatingAction)
  {
    this(message, inErrorCode, Map.of(), inRemediatingAction);
  }

  /**
   * Construct an exception.
   *
   * @param message      The message
   * @param inErrorCode  The error code
   * @param inAttributes The attributes
   */

  public LException(
    final String message,
    final String inErrorCode,
    final Map<String, String> inAttributes)
  {
    this(message, inErrorCode, inAttributes, Optional.empty());
  }

  @Override
  public final String errorCode()
  {
    return this.errorCode;
  }

  @Override
  public final Map<String, String> attributes()
  {
    return this.attributes;
  }

  @Override
  public final Optional<String> remediatingAction()
  {
    return this.remediatingAction;
  }

  @Override
  public final Optional<Throwable> exception()
  {
    return Optional.of(this);
  }
}

