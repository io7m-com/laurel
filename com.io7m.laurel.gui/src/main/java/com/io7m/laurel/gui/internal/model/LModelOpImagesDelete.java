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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Delete images.
 */

public final class LModelOpImagesDelete extends LModelOpAbstract
{
  private final LModel model;
  private final List<LMImage> images;
  private ArrayList<LModelOpCaptionsAssign> assignments;
  private List<LMImageCreate> imageCreates;

  /**
   * Delete images.
   *
   * @param inModel  The model
   * @param inImages The images
   */

  public LModelOpImagesDelete(
    final LModel inModel,
    final List<LMImage> inImages)
  {
    this.model =
      Objects.requireNonNull(inModel, "model");
    this.images =
      Objects.requireNonNull(inImages, "inImages");
  }

  @Override
  protected void onExecute()
    throws LModelOpException
  {
    this.assignments =
      new ArrayList<>();
    this.imageCreates =
      this.images.stream()
        .map(x -> new LMImageCreate(x.id(), x.fileName().get()))
        .collect(Collectors.toList());

    for (final var image : this.images) {
      final var imageCaptions =
        image.captions()
          .stream()
          .map(LMCaption::id)
          .collect(Collectors.toList());

      this.assignments.add(
        new LModelOpCaptionsAssign(
          this.model, List.of(image.id()), imageCaptions
        )
      );

      new LModelOpCaptionsUnassign(
        this.model, List.of(image.id()), imageCaptions
      ).execute();

      this.model.images().remove(image.id());
      this.model.imageCaptionGraph().removeVertex(image);
    }
  }

  @Override
  protected void onUndo()
    throws LModelOpException
  {
    new LModelOpImagesCreate(
      this.model,
      this.imageCreates
    ).execute();

    for (final var assignment : this.assignments) {
      assignment.execute();
    }
  }

  @Override
  public String description()
  {
    return "Delete images";
  }
}
