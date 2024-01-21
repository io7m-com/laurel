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

import com.io7m.jaffirm.core.Preconditions;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * An image set.
 *
 * @param globalPrefixCaptions The global prefix captions
 * @param captions             The captions
 * @param images               The images
 */

public record LImageSet(
  List<String> globalPrefixCaptions,
  SortedMap<LImageCaptionID, LImageCaption> captions,
  SortedMap<LImageID, LImage> images)
{
  /**
   * An image set.
   *
   * @param captions The captions
   * @param images   The images
   */

  public LImageSet
  {
    Objects.requireNonNull(globalPrefixCaptions, "globalPrefixCaptions");
    Objects.requireNonNull(captions, "captions");
    Objects.requireNonNull(images, "images");

    for (final var caption : globalPrefixCaptions) {
      new LImageCaption(new LImageCaptionID(UUID.randomUUID()), caption);
    }

    final var texts = new HashSet<String>();
    for (final var entry : captions.entrySet()) {
      Preconditions.checkPreconditionV(
        Objects.equals(entry.getKey(), entry.getValue().id()),
        "Caption ID must match caption."
      );
      texts.add(entry.getValue().text());
    }

    Preconditions.checkPreconditionV(
      texts.size() == captions.size(),
      "Caption texts must be unique"
    );

    for (final var entry : images.entrySet()) {
      Preconditions.checkPreconditionV(
        Objects.equals(entry.getKey(), entry.getValue().imageID()),
        "Image ID must match caption."
      );

      for (final var captionId : entry.getValue().captions()) {
        Preconditions.checkPreconditionV(
          captions.containsKey(captionId),
          "Captions referred to by images must exist"
        );
      }
    }
  }

  /**
   * Find captions for the given image ID.
   *
   * @param id The image ID
   *
   * @return The captions, if any
   */

  public SortedSet<LImageCaption> captionsForImage(
    final LImageID id)
  {
    return this.images.get(id)
      .captions()
      .stream()
      .map(this.captions::get)
      .collect(Collectors.toCollection(TreeSet::new));
  }
}
