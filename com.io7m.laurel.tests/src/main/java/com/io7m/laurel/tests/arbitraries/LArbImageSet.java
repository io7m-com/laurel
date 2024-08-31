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


package com.io7m.laurel.tests.arbitraries;

import com.io7m.laurel.model.LImageCaption;
import com.io7m.laurel.model.LImageCaptionID;
import com.io7m.laurel.model.LImageID;
import com.io7m.laurel.model.LImageSet;
import com.io7m.laurel.model.LOldImage;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Combinators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class LArbImageSet extends LArbAbstract<LImageSet>
{
  public LArbImageSet()
  {
    super(LImageSet.class, () -> {
      return Combinators.combine(
        Arbitraries.defaultFor(LImageID.class),
        Arbitraries.defaultFor(LImageCaption.class)
          .set()
          .ofMinSize(1)
          .ofMaxSize(100),
        Arbitraries.defaultFor(LImageID.class)
          .list()
          .ofMinSize(1)
          .ofMaxSize(10)
      ).as((id, captions, images) -> {

        final var imagesConstructed = new ArrayList<LOldImage>();
        for (final var imageId : images) {
          final var imageCaps = new TreeSet<LImageCaptionID>();
          for (final var caption : captions) {
            if (Math.random() < 0.125) {
              imageCaps.add(caption.id());
            }
          }
          imagesConstructed.add(new LOldImage(imageId, imageId + ".png", imageCaps));
        }

        return new LImageSet(
          List.of("a", "b", "c"),
          new TreeMap<>(captionMap(captions)),
          new TreeMap<>(imageMap(imagesConstructed))
        );
      });
    });
  }

  private static Map<LImageCaptionID, LImageCaption> captionMap(
    final Collection<LImageCaption> captions)
  {
    return captions.stream()
      .map(x -> Map.entry(x.id(), x))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static Map<LImageID, LOldImage> imageMap(
    final Collection<LOldImage> images)
  {
    return images.stream()
      .map(x -> Map.entry(x.imageID(), x))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
