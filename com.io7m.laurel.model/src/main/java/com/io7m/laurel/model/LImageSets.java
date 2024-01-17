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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Functions to create image sets.
 */

public final class LImageSets
{
  private LImageSets()
  {

  }

  /**
   * @return An empty image set
   */

  public static LImageSetType empty()
  {
    return new LImageSet();
  }

  private static final class LImageSet implements LImageSetType
  {
    private final SortedMap<LImageCaptionID, LImageCaption> captions;
    private final SortedMap<LImageID, LImage> images;

    /**
     * A mutable image set.
     */

    LImageSet()
    {
      this.captions = new TreeMap<>();
      this.images = new TreeMap<>();
    }

    @Override
    public String toString()
    {
      final StringBuilder sb = new StringBuilder("LImageSet{");
      sb.append("captions=").append(this.captions);
      sb.append(", images=").append(this.images);
      sb.append('}');
      return sb.toString();
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
      final LImageSet imageSet = (LImageSet) o;
      return Objects.equals(this.captions, imageSet.captions)
             && Objects.equals(this.images, imageSet.images);
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(this.captions, this.images);
    }

    @Override
    public SortedMap<LImageCaptionID, LImageCaption> captions()
    {
      return Collections.unmodifiableSortedMap(this.captions);
    }

    @Override
    public SortedMap<LImageID, LImage> images()
    {
      return Collections.unmodifiableSortedMap(this.images);
    }

    @Override
    public LImageSetCommandType putCaption(
      final LImageCaption caption)
    {
      Objects.requireNonNull(caption, "caption");
      return new LOpPutCaption(this, caption);
    }

    @Override
    public LImageSetCommandType removeCaption(
      final LImageCaptionID caption)
    {
      Objects.requireNonNull(caption, "caption");
      return new LOpRemoveCaption(this, caption);
    }

    @Override
    public LImageSetCommandType putImage(
      final LImage image)
    {
      Objects.requireNonNull(image, "image");
      return new LOpPutImage(this, image);
    }
  }

  private static final class LOpPutCaption
    implements LImageSetCommandType
  {
    private final LImageCaption caption;
    private final LImageSet imageSet;
    private LImageCaption existing;

    LOpPutCaption(
      final LImageSet inImageSet,
      final LImageCaption inCaption)
    {
      this.imageSet =
        Objects.requireNonNull(inImageSet, "imageSet");
      this.caption =
        Objects.requireNonNull(inCaption, "caption");
    }

    @Override
    public void execute()
    {
      this.existing = this.imageSet.captions.get(this.caption.id());
      this.imageSet.captions.put(this.caption.id(), this.caption);
    }

    @Override
    public void undo()
    {
      this.imageSet.captions.remove(this.caption.id());
      final var e = this.existing;
      if (e != null) {
        this.imageSet.captions.put(e.id(), e);
      }
    }
  }

  private static final class LOpRemoveCaption
    implements LImageSetCommandType
  {
    private final LImageCaptionID caption;
    private final LImageSet imageSet;
    private LImageCaption existingCaption;
    private List<LImage> existingImages;

    LOpRemoveCaption(
      final LImageSet inImageSet,
      final LImageCaptionID inCaption)
    {
      this.imageSet =
        Objects.requireNonNull(inImageSet, "imageSet");
      this.caption =
        Objects.requireNonNull(inCaption, "caption");
      this.existingImages =
        List.of();
    }

    @Override
    public void execute()
    {
      this.existingImages =
        this.imageSet.images.values()
          .stream()
          .filter(i -> i.captions().contains(this.caption))
          .collect(Collectors.toList());

      this.existingCaption =
        this.imageSet.captions.remove(this.caption);

      for (final var image : this.existingImages) {
        final var newImage = new LImage(
          image.imageID(),
          image.fileName(),
          image.captions()
            .stream()
            .filter(i -> !Objects.equals(i, this.caption))
            .collect(Collectors.toList())
        );
        this.imageSet.images.put(newImage.imageID(), newImage);
      }
    }

    @Override
    public void undo()
    {
      final var e = this.existingCaption;
      if (e != null) {
        this.imageSet.captions.put(e.id(), e);
      }

      for (final var image : this.existingImages) {
        this.imageSet.images.put(image.imageID(), image);
      }
    }
  }

  private static final class LOpPutImage
    implements LImageSetCommandType
  {
    private final LImage image;
    private final LImageSet imageSet;
    private LImage existing;

    LOpPutImage(
      final LImageSet inImageSet,
      final LImage inImage)
    {
      this.imageSet =
        Objects.requireNonNull(inImageSet, "imageSet");
      this.image =
        Objects.requireNonNull(inImage, "caption");
    }

    @Override
    public void execute()
    {
      for (final var caption : this.image.captions()) {
        if (!this.imageSet.captions.containsKey(caption)) {
          throw new IllegalArgumentException(
            "No such caption: %s".formatted(caption)
          );
        }
      }

      this.existing = this.imageSet.images.get(this.image.imageID());
      this.imageSet.images.put(this.image.imageID(), this.image);
    }

    @Override
    public void undo()
    {
      this.imageSet.images.remove(this.image.imageID());
      final var e = this.existing;
      if (e != null) {
        this.imageSet.images.put(e.imageID(), e);
      }
    }
  }
}
