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


package com.io7m.laurel.gui.internal;

import com.io7m.jaffirm.core.Postconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A controller factory based on an immutable map of controllers.
 *
 * @param <T> The base type of controllers.
 */

public final class LViewControllerFactoryMapped<T>
  implements LViewControllerFactoryType<T>
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LViewControllerFactoryMapped.class);

  private final Map<Class<? extends T>, Supplier<? extends T>> controllers;

  private LViewControllerFactoryMapped(
    final Map<Class<? extends T>, Supplier<? extends T>> inControllers)
  {
    this.controllers =
      Objects.requireNonNull(inControllers, "controllers");
  }

  /**
   * A controller factory based on an immutable map of controllers.
   *
   * @param <T>          The base type of controllers.
   * @param constructors The constructors
   *
   * @return A controller factory
   */

  @SafeVarargs
  public static <T> LViewControllerFactoryType<T> create(
    final Map.Entry<Class<? extends T>, Supplier<? extends T>>... constructors)
  {
    Objects.requireNonNull(constructors, "constructors");

    return new LViewControllerFactoryMapped<>(
      Map.ofEntries(constructors)
    );
  }

  @Override
  public T call(
    final Class<? extends T> clazz)
  {
    LOG.debug("Resolving controller: {}", clazz);

    final var supplier = this.controllers.get(clazz);
    if (supplier == null) {
      throw new IllegalStateException(
        "No controller available for %s".formatted(clazz)
      );
    }

    final var controller = supplier.get();
    Postconditions.checkPostconditionV(
      clazz.isInstance(controller),
      "Controller %s is an instance of %s",
      controller,
      clazz
    );
    return controller;
  }
}
