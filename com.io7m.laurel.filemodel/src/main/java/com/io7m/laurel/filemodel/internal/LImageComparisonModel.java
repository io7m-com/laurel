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


package com.io7m.laurel.filemodel.internal;

import com.io7m.jattribute.core.AttributeReadableType;
import com.io7m.jattribute.core.AttributeType;
import com.io7m.jattribute.core.Attributes;
import com.io7m.laurel.filemodel.LImageComparison;
import com.io7m.laurel.model.LCaption;
import com.io7m.laurel.model.LCaptionID;
import com.io7m.laurel.model.LImageID;
import com.io7m.laurel.model.LImageWithID;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A model for performing image comparisons.
 */

public final class LImageComparisonModel
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LImageComparisonModel.class);

  private final AttributeType<List<LCaption>> imageCaptionsA;
  private final AttributeType<List<LCaption>> imageCaptionsB;
  private final AttributeType<Optional<Map.Entry<LImageID, LImageID>>> imageComparison;
  private final AttributeType<Optional<LImageComparison>> imageComparisonImages;
  private final AttributeType<List<LCaption>> imageCaptionsComparisonA;
  private final AttributeType<List<LCaption>> imageCaptionsComparisonB;
  private final AttributeReadableType<List<LImageWithID>> imagesAll;

  LImageComparisonModel(
    final Attributes attributes,
    final AttributeReadableType<List<LImageWithID>> imageAll)
  {
    this.imagesAll =
      Objects.requireNonNull(imageAll, "imageAll");
    this.imageComparison =
      attributes.withValue(Optional.empty());
    this.imageComparisonImages =
      attributes.withValue(Optional.empty());
    this.imageCaptionsA =
      attributes.withValue(List.of());
    this.imageCaptionsB =
      attributes.withValue(List.of());
    this.imageCaptionsComparisonA =
      attributes.withValue(List.of());
    this.imageCaptionsComparisonB =
      attributes.withValue(List.of());
  }

  private void imageComparisonReloadImages(
    final LImageID imageAId,
    final LImageID imageBId)
  {
    LOG.debug("Loading images");

    final var imageA =
      this.imagesAll.get()
        .stream()
        .filter(i -> Objects.equals(i.id(), imageAId))
        .findFirst();

    final var imageB =
      this.imagesAll.get()
        .stream()
        .filter(i -> Objects.equals(i.id(), imageBId))
        .findFirst();

    if (imageA.isPresent() && imageB.isPresent()) {
      this.imageComparisonImages.set(
        Optional.of(new LImageComparison(imageA.get(), imageB.get()))
      );
    } else {
      this.imageComparisonImages.set(Optional.empty());
    }
  }

  private void imageComparisonComputeComparison()
  {
    LOG.debug("Computing caption differences");

    final var captionsA =
      new HashMap<LCaptionID, LCaption>();
    final var captionsB =
      new HashMap<LCaptionID, LCaption>();
    final var captionsAExtra =
      new HashMap<LCaptionID, LCaption>();
    final var captionsBExtra =
      new HashMap<LCaptionID, LCaption>();

    for (final var caption : this.imageCaptionsA.get()) {
      captionsA.put(caption.id(), caption);
    }
    for (final var caption : this.imageCaptionsB.get()) {
      captionsB.put(caption.id(), caption);
    }
    for (final var caption : captionsA.values()) {
      if (!captionsB.containsKey(caption.id())) {
        captionsAExtra.put(caption.id(), caption);
      }
    }
    for (final var caption : captionsB.values()) {
      if (!captionsA.containsKey(caption.id())) {
        captionsBExtra.put(caption.id(), caption);
      }
    }

    LOG.debug("Image A has {} extra captions", captionsAExtra.size());
    LOG.debug("Image B has {} extra captions", captionsBExtra.size());

    this.imageCaptionsComparisonA.set(
      captionsAExtra.values()
        .stream()
        .sorted()
        .toList()
    );
    this.imageCaptionsComparisonB.set(
      captionsBExtra.values()
        .stream()
        .sorted()
        .toList()
    );
  }

  private void imageComparisonReloadCaptions(
    final DSLContext context,
    final Optional<LImageComparison> newValue)
  {
    LOG.debug("Reloading captions");

    if (newValue.isEmpty()) {
      this.imageCaptionsA.set(List.of());
      this.imageCaptionsB.set(List.of());
      return;
    }

    final var comparison =
      newValue.get();

    final var captionsA =
      LCommandModelUpdates.listImageCaptionsAssigned(
        context,
        comparison.imageA().id()
      );
    final var captionsB =
      LCommandModelUpdates.listImageCaptionsAssigned(
        context,
        comparison.imageB().id()
      );

    LOG.debug("Image A has {} captions", captionsA.size());
    LOG.debug("Image B has {} captions", captionsB.size());

    this.imageCaptionsA.set(captionsA);
    this.imageCaptionsB.set(captionsB);
  }

  /**
   * Reload the comparison.
   *
   * @param context The context
   */

  public void reload(
    final DSLContext context)
  {
    final var existing = this.imageComparison.get();
    if (existing.isPresent()) {
      this.set(
        context,
        existing.get().getKey(),
        existing.get().getValue()
      );
      return;
    }

    this.clear();
  }

  private void clear()
  {
    LOG.debug("Clear");

    this.imageComparison.set(Optional.empty());
    this.imageCaptionsA.set(List.of());
    this.imageCaptionsB.set(List.of());
    this.imageCaptionsComparisonA.set(List.of());
    this.imageCaptionsComparisonB.set(List.of());
  }

  /**
   * @return The image comparison
   */

  public AttributeReadableType<Optional<LImageComparison>> imageComparison()
  {
    return this.imageComparisonImages;
  }

  /**
   * @return The captions on A that are not on B
   */

  public AttributeReadableType<List<LCaption>> imageComparisonA()
  {
    return this.imageCaptionsComparisonA;
  }

  /**
   * @return The captions on B that are not on A
   */

  public AttributeReadableType<List<LCaption>> imageComparisonB()
  {
    return this.imageCaptionsComparisonB;
  }

  /**
   * Set the images.
   *
   * @param context The context
   * @param imageA  The image A
   * @param imageB  The image B
   */

  public void set(
    final DSLContext context,
    final LImageID imageA,
    final LImageID imageB)
  {
    LOG.debug("Set {} {}", imageA, imageB);

    this.imageComparison.set(Optional.of(Map.entry(imageA, imageB)));
    this.imageComparisonReloadImages(imageA, imageB);
    this.imageComparisonReloadCaptions(
      context,
      this.imageComparisonImages.get());
    this.imageComparisonComputeComparison();
  }
}
