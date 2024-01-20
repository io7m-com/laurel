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
import com.io7m.laurel.model.LImageID;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Assign captions.
 */

public final class LModelOpCaptionsAssign extends LModelOpAbstract
{
  private final LModel model;
  private final List<LImageID> imageIDs;
  private final List<LImageCaptionID> captionIDs;
  private List<LMCaption> captions;
  private List<LMImage> images;

  /**
   * Assign captions.
   *
   * @param inModel      The model
   * @param inImageIDs   The image IDs
   * @param inCaptionIDs The caption IDs
   */

  public LModelOpCaptionsAssign(
    final LModel inModel,
    final List<LImageID> inImageIDs,
    final List<LImageCaptionID> inCaptionIDs)
  {
    this.model =
      Objects.requireNonNull(inModel, "model");
    this.imageIDs =
      Objects.requireNonNull(inImageIDs, "inImages");
    this.captionIDs =
      Objects.requireNonNull(inCaptionIDs, "inCaptionIDs");

    this.captions =
      new ArrayList<>();
    this.images =
      new ArrayList<>();
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

    for (final var imageID : this.imageIDs) {
      final var image = this.model.images().get(imageID);
      if (image == null) {
        throw new LModelOpException(
          "Image %s does not exist".formatted(imageID)
        );
      }
      this.images.add(image);
    }

    final var graph = this.model.imageCaptionGraph();
    for (final var image : this.images) {
      graph.addVertex(image);
    }
    for (final var caption : this.captions) {
      graph.addVertex(caption);
    }

    for (final var image : this.images) {
      for (final var caption : this.captions) {
        graph.addEdge(caption, image, new LMImageCaption(image, caption));

        if (!image.captions().contains(caption)) {
          caption.count().set(caption.count().get() + 1L);
        }
      }
      image.captions().addAll(this.captions);
    }
  }

  @Override
  protected void onUndo()
    throws LModelOpException
  {
    new LModelOpCaptionsUnassign(
      this.model,
      this.imageIDs,
      this.captionIDs
    ).execute();
  }

  @Override
  public String description()
  {
    return "Assign caption(s)";
  }
}
