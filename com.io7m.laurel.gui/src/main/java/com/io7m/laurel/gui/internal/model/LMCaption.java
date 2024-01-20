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


package com.io7m.laurel.gui.internal.model;

import com.io7m.laurel.model.LImageCaption;
import com.io7m.laurel.model.LImageCaptionID;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

/**
 * A caption.
 */

public final class LMCaption
  implements Comparable<LMCaption>, LMImageOrCaptionType
{
  private final LImageCaptionID id;
  private final StringProperty text;
  private final SimpleLongProperty count;

  /**
   * A caption.
   *
   * @param inId The caption ID
   */

  public LMCaption(
    final LImageCaptionID inId)
  {
    this.id =
      Objects.requireNonNull(inId, "id");
    this.text =
      new SimpleStringProperty();
    this.count =
      new SimpleLongProperty();
  }

  /**
   * @return The text
   */

  public ReadOnlyProperty<String> text()
  {
    return this.text;
  }

  /**
   * @return The count
   */

  public SimpleLongProperty count()
  {
    return this.count;
  }

  /**
   * @return The ID
   */

  public LImageCaptionID id()
  {
    return this.id;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || !this.getClass().equals(o.getClass())) {
      return false;
    }
    final LMCaption lmCaption = (LMCaption) o;
    return Objects.equals(this.id, lmCaption.id);
  }

  @Override
  public String toString()
  {
    return "[LMCaption %s %s]".formatted(this.id, this.text.get());
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(this.id);
  }

  @Override
  public int compareTo(
    final LMCaption other)
  {
    return Comparator.comparing((LMCaption o) -> o.text.get())
      .thenComparing(LMCaption::id)
      .compare(this, other);
  }

  /**
   * Set the text.
   *
   * @param newText The new text
   */

  public void setText(
    final String newText)
  {
    this.text.set(
      new LImageCaption(
        new LImageCaptionID(UUID.randomUUID()),
        newText
      ).text()
    );
  }
}
