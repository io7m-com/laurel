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

import com.io7m.laurel.model.LImageCaptionID;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Delete captions.
 */

public final class LModelOpCaptionDelete extends LModelOpAbstract
{
  private final LModel model;
  private final List<LImageCaptionID> captionIDs;
  private final HashSet<LMCaption> captions;
  private final HashSet<LMImage> images;

  /**
   * Delete captions.
   *
   * @param inModel      The model
   * @param inCaptionIDs The captions
   */

  public LModelOpCaptionDelete(
    final LModel inModel,
    final List<LImageCaptionID> inCaptionIDs)
  {
    this.model =
      Objects.requireNonNull(inModel, "model");
    this.captionIDs =
      Objects.requireNonNull(inCaptionIDs, "captionIDs");

    this.captions =
      new HashSet<>();
    this.images =
      new HashSet<>();
  }

  @Override
  protected void onExecute()
    throws LModelOpException
  {
    this.captions.clear();
    this.images.clear();

    for (final var captionID : this.captionIDs) {
      final var caption = this.model.captions().get(captionID);
      if (caption == null) {
        throw new LModelOpException(
          "Caption %s does not exist".formatted(captionID)
        );
      }
      this.captions.add(caption);
    }

    final var graph = this.model.imageCaptionGraph();
    for (final var caption : this.captions) {
      for (final var edge : graph.outgoingEdgesOf(caption)) {
        this.images.add(edge.image());
      }
    }

    for (final var image : this.images) {
      image.captions().removeAll(this.captions);
    }

    final var captionTexts = this.model.captionTexts();
    for (final var caption : this.captions) {
      captionTexts.remove(caption.text().getValue());
      graph.removeVertex(caption);
      this.model.captions().remove(caption.id());
    }
  }

  @Override
  protected void onUndo()
    throws LModelOpException
  {
    for (final var caption : this.captions) {
      final var create =
        new LModelOpCaptionCreate(
          this.model,
          caption.id(),
          caption.text().getValue()
        );
      create.execute();
    }

    new LModelOpCaptionsAssign(
      this.model,
      this.images.stream()
        .map(LMImage::id)
        .collect(Collectors.toList()),
      this.captions.stream()
        .map(LMCaption::id)
        .collect(Collectors.toList())
    ).execute();
  }

  @Override
  public String description()
  {
    return "Create caption";
  }
}
