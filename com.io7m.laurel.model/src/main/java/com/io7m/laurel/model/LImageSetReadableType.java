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

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;

/**
 * The image set.
 */

public interface LImageSetReadableType
{
  /**
   * @return The captions
   */

  SortedMap<LImageCaptionID, LImageCaption> captions();

  /**
   * @return The images
   */

  SortedMap<LImageID, LImage> images();

  /**
   * @param imageId The image
   *
   * @return The captions for the given image
   */

  default List<LImageCaption> captionsForImage(
    final LImageID imageId)
  {
    final var image = this.images().get(imageId);
    if (image != null) {
      return image.captions()
        .stream()
        .map(x -> this.captions().get(x))
        .collect(Collectors.toList());
    }
    return List.of();
  }

  /**
   * @param caption The caption ID
   *
   * @return The number of images to which the caption is assigned
   */

  long captionAssignmentCount(LImageCaptionID caption);

  /**
   * @param text The text
   *
   * @return The ID of the caption with the given text
   */

  Optional<LImageCaptionID> captionForText(String text);

  /**
   * @return A readable stream of events
   */

  Flow.Publisher<LEventType> events();
}
