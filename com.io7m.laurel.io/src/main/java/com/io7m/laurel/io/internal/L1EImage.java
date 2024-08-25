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

import com.io7m.blackthorne.core.BTElementHandlerConstructorType;
import com.io7m.blackthorne.core.BTElementHandlerType;
import com.io7m.blackthorne.core.BTElementParsingContextType;
import com.io7m.blackthorne.core.BTQualifiedName;
import com.io7m.laurel.model.LOldImage;
import com.io7m.laurel.model.LImageCaptionID;
import com.io7m.laurel.model.LImageID;
import org.xml.sax.Attributes;

import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import static com.io7m.laurel.io.internal.LNames.qName;

/**
 * An element handler.
 */

public final class L1EImage
  implements BTElementHandlerType<LImageCaptionID, LOldImage>
{
  private final TreeSet<LImageCaptionID> captions;
  private LImageID imageId;
  private String fileName;

  /**
   * An element handler.
   *
   * @param context The context
   */

  public L1EImage(
    final BTElementParsingContextType context)
  {
    this.captions = new TreeSet<>();
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
  {
    this.imageId =
      new LImageID(UUID.fromString(attributes.getValue("ID")));
    this.fileName =
      attributes.getValue("FileName");
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ? extends LImageCaptionID>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(
        qName("CaptionReference"),
        L1ECaptionReference::new
      )
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final LImageCaptionID result)
  {
    this.captions.add(result);
  }

  @Override
  public LOldImage onElementFinished(
    final BTElementParsingContextType context)
  {
    return new LOldImage(
      this.imageId,
      this.fileName,
      this.captions
    );
  }
}
