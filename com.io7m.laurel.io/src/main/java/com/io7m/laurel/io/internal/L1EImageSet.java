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


package com.io7m.laurel.io.internal;

import com.io7m.blackthorne.core.BTElementHandlerConstructorType;
import com.io7m.blackthorne.core.BTElementHandlerType;
import com.io7m.blackthorne.core.BTElementParsingContextType;
import com.io7m.blackthorne.core.BTQualifiedName;
import com.io7m.laurel.model.LImage;
import com.io7m.laurel.model.LImageCaption;
import com.io7m.laurel.model.LImageSetType;
import com.io7m.laurel.model.LImageSets;

import java.util.List;
import java.util.Map;

import static com.io7m.laurel.io.internal.LNames.qName;

/**
 * An element handler.
 */

public final class L1EImageSet
  implements BTElementHandlerType<Object, LImageSetType>
{
  private final LImageSetType imageSet;

  /**
   * An element handler.
   *
   * @param context The context
   */

  public L1EImageSet(
    final BTElementParsingContextType context)
  {
    this.imageSet = LImageSets.empty();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(qName("Images"), L1EImages::new),
      Map.entry(qName("Captions"), L1ECaptions::new)
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
  {
    switch (result) {
      case final List xs when !xs.isEmpty() -> {
        switch (xs.get(0)) {
          case final LImage ignored0 -> {
            for (final var x : xs) {
              this.imageSet.putImage((LImage) x);
            }
          }
          case final LImageCaption ignored1 -> {
            for (final var x : xs) {
              this.imageSet.putCaption((LImageCaption) x);
            }
          }
          default -> {
            throw new IllegalStateException(
              "Unexpected element: %s".formatted(result)
            );
          }
        }
      }
      case final List xs -> {
        // Nothing
      }
      default -> {
        throw new IllegalStateException(
          "Unexpected element: %s".formatted(result)
        );
      }
    }
  }

  @Override
  public LImageSetType onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.imageSet;
  }
}
