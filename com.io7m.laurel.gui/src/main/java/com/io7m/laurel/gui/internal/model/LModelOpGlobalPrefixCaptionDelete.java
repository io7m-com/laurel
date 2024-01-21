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

public final class LModelOpGlobalPrefixCaptionDelete extends LModelOpAbstract
{
  private final LModel model;
  private final int index;
  private String text;

  /**
   * Create a caption.
   *
   * @param inModel     The model
   * @param inIndex      The caption index
   */

  public LModelOpGlobalPrefixCaptionDelete(
    final LModel inModel,
    final int inIndex)
  {
    this.model =
      Objects.requireNonNull(inModel, "model");
    this.index =
      inIndex;
  }

  @Override
  protected void onExecute()
    throws LModelOpException
  {
    this.text = this.model.globalPrefixCaptionRemove(this.index);
  }

  @Override
  protected void onUndo()
    throws LModelOpException
  {
    this.model.globalPrefixCaptionAdd(this.index, this.text);
  }

  @Override
  public String description()
  {
    return "Delete a global prefix caption";
  }
}
