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


package com.io7m.laurel.io.internal;

import com.io7m.blackthorne.core.BTElementHandlerType;
import com.io7m.blackthorne.core.BTElementParsingContextType;
import com.io7m.laurel.model.LImageCaption;
import com.io7m.laurel.model.LImageCaptionID;
import org.xml.sax.Attributes;

import java.util.UUID;

/**
 * An element handler.
 */

public final class L1ECaption
  implements BTElementHandlerType<Object, LImageCaption>
{
  private LImageCaptionID id;
  private String text;

  /**
   * An element handler.
   *
   * @param context The context
   */

  public L1ECaption(
    final BTElementParsingContextType context)
  {

  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
  {
    this.id =
      new LImageCaptionID(UUID.fromString(attributes.getValue("ID")));
    this.text =
      attributes.getValue("Text");
  }

  @Override
  public LImageCaption onElementFinished(
    final BTElementParsingContextType context)
  {
    return new LImageCaption(
      this.id,
      this.text
    );
  }
}
