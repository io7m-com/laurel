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

import com.io7m.laurel.model.LImageID;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;

/**
 * An image.
 */

public final class LMImage
  implements Comparable<LMImage>, LMImageOrCaptionType
{
  private final LImageID id;
  private final SimpleObjectProperty<Path> file;
  private final ObservableSet<LMCaption> captions;

  /**
   * An image.
   *
   * @param inId The ID
   */

  public LMImage(
    final LImageID inId)
  {
    this.id =
      Objects.requireNonNull(inId, "id");
    this.file =
      new SimpleObjectProperty<>();
    this.captions =
      FXCollections.observableSet();
  }

  @Override
  public String toString()
  {
    return "[LMImage %s %s]".formatted(this.id, this.file.get());
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
    final LMImage lmImage = (LMImage) o;
    return Objects.equals(this.id, lmImage.id);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(this.id);
  }

  /**
   * @return The captions
   */

  public ObservableSet<LMCaption> captions()
  {
    return this.captions;
  }

  /**
   * @return The file name
   */

  public SimpleObjectProperty<Path> fileName()
  {
    return this.file;
  }

  /**
   * @return The ID
   */

  public LImageID id()
  {
    return this.id;
  }

  @Override
  public int compareTo(
    final LMImage other)
  {
    return Comparator.comparing((LMImage o) -> o.file.get())
      .thenComparing(LMImage::id)
      .compare(this, other);
  }
}
