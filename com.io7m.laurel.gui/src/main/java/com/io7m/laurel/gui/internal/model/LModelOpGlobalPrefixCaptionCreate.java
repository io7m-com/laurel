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

import java.util.Objects;

/**
 * Create a caption.
 */

public final class LModelOpGlobalPrefixCaptionCreate extends LModelOpAbstract
{
  private final LModel model;
  private final String text;
  private final int index;

  /**
   * Create a caption.
   *
   * @param inModel     The model
   * @param inIndex The index
   * @param inText      The text
   */

  public LModelOpGlobalPrefixCaptionCreate(
    final LModel inModel,
    final int inIndex,
    final String inText)
  {
    this.model =
      Objects.requireNonNull(inModel, "model");
    this.index =
      inIndex;
    this.text =
      Objects.requireNonNull(inText, "text");
  }

  @Override
  protected void onExecute()
    throws LModelOpException
  {
    this.model.globalPrefixCaptionAdd(this.index, this.text);
  }

  @Override
  protected void onUndo()
    throws LModelOpException
  {
    this.model.globalPrefixCaptionRemove(this.index);
  }

  @Override
  public String description()
  {
    return "Create a global prefix caption";
  }
}
