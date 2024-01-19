/*
 * Copyright © 2021 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

/**
 * A flow subscriber that consumes messages until it is explicitly closed.
 *
 * @param <T> The type of messages
 */

public final class LPerpetualSubscriber<T>
  implements Flow.Subscriber<T>, AutoCloseable
{
  private final Consumer<T> processor;
  private Flow.Subscription subscription;

  /**
   * A flow subscriber that consumes messages until it is explicitly closed.
   *
   * @param inProcessor A consumer function
   */

  public LPerpetualSubscriber(
    final Consumer<T> inProcessor)
  {
    this.processor =
      Objects.requireNonNull(inProcessor, "inProcessor");
  }

  @Override
  public void onSubscribe(
    final Flow.Subscription newSubscription)
  {
    this.subscription =
      Objects.requireNonNull(newSubscription, "newSubscription");

    this.subscription.request(Long.MAX_VALUE);
  }

  @Override
  public void onNext(
    final T item)
  {
    this.processor.accept(item);
  }

  @Override
  public void onError(
    final Throwable throwable)
  {

  }

  @Override
  public void onComplete()
  {

  }

  @Override
  public void close()
  {
    this.subscription.cancel();
  }
}
