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

import java.util.Objects;

/**
 * Set the text for a caption.
 */

public final class LModelOpCaptionSetText extends LModelOpAbstract
{
  private final LModel model;
  private final LImageCaptionID captionID;
  private final String text;
  private String textPrevious;
  private LMCaption caption;

  /**
   * Set the text for a caption.
   *
   * @param inModel     The model
   * @param inCaptionID The caption
   * @param inText      The new text
   */

  public LModelOpCaptionSetText(
    final LModel inModel,
    final LImageCaptionID inCaptionID,
    final String inText)
  {
    this.model =
      Objects.requireNonNull(inModel, "model");
    this.captionID =
      Objects.requireNonNull(inCaptionID, "captionID");
    this.text =
      Objects.requireNonNull(inText, "text");
  }

  @Override
  protected void onExecute()
    throws LModelOpException
  {
    final var captions = this.model.captions();
    if (!captions.containsKey(this.captionID)) {
      return;
    }

    final var captionTexts =
      this.model.captionTexts();
    final var captionForText =
      captionTexts.get(this.text);

    if (captionForText != null) {
      if (!captionForText.equals(this.captionID)) {
        throw new LModelOpException(
          "Caption %s already has text '%s'"
            .formatted(captionForText, this.text)
        );
      }
    }

    this.caption = this.model.captions().get(this.captionID);
    this.textPrevious = this.caption.text().getValue();
    this.caption.setText(this.text);

    this.model.captionTexts().remove(this.textPrevious);
    this.model.captionTexts().put(this.text, this.captionID);
  }

  @Override
  protected void onUndo()
  {
    this.caption.setText(this.textPrevious);
    this.model.captionTexts().remove(this.text);
    this.model.captionTexts().put(this.textPrevious, this.captionID);
  }

  @Override
  public String description()
  {
    return "Change caption text";
  }
}
