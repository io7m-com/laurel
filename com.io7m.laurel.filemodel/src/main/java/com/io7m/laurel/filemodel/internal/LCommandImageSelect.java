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

import com.io7m.laurel.model.LCaption;
import com.io7m.laurel.model.LCaptionID;
import com.io7m.laurel.model.LCaptionName;
import com.io7m.laurel.model.LHashSHA256;
import com.io7m.laurel.model.LImage;
import com.io7m.laurel.model.LImageID;
import com.io7m.laurel.model.LImageWithID;
import org.jooq.DSLContext;

import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.CAPTIONS;
import static com.io7m.laurel.filemodel.internal.Tables.IMAGES;
import static com.io7m.laurel.filemodel.internal.Tables.IMAGE_BLOBS;
import static com.io7m.laurel.filemodel.internal.Tables.IMAGE_CAPTIONS;
import static com.io7m.laurel.filemodel.internal.Tables.IMAGE_CAPTIONS_COUNTS;

/**
 * Select an image.
 */

public final class LCommandImageSelect
  extends LCommandAbstract<Optional<LImageID>>
{
  /**
   * Select an image.
   */

  public LCommandImageSelect()
  {

  }

  /**
   * @return A command factory
   */

  public static LCommandFactoryType<Optional<LImageID>> provider()
  {
    return new LCommandFactory<>(
      LCommandImageSelect.class.getCanonicalName(),
      LCommandImageSelect::fromProperties
    );
  }

  private static LCommandImageSelect fromProperties(
    final Properties p)
  {
    final var c = new LCommandImageSelect();
    c.setExecuted(true);
    return c;
  }

  private static LCaption mapRecord(
    final org.jooq.Record r)
  {
    return new LCaption(
      new LCaptionID(r.get(CAPTIONS.CAPTION_ID)),
      new LCaptionName(r.get(CAPTIONS.CAPTION_TEXT)),
      r.get(LCommandModelUpdates.COUNT_FIELD)
    );
  }

  @Override
  protected LCommandUndoable onExecute(
    final LFileModel model,
    final LDatabaseTransactionType transaction,
    final Optional<LImageID> request)
  {
    final var context =
      transaction.get(DSLContext.class);

    if (request.isEmpty()) {
      model.setImageCaptionsAssigned(List.of());
      model.setImageSelected(Optional.empty());
      return LCommandUndoable.COMMAND_NOT_UNDOABLE;
    }

    final var imageId =
      request.get();

    final var imageRecOpt =
      context.select(
          IMAGES.IMAGE_BLOB,
          IMAGES.IMAGE_FILE,
          IMAGES.IMAGE_ID,
          IMAGES.IMAGE_NAME,
          IMAGES.IMAGE_SOURCE,
          IMAGE_BLOBS.IMAGE_BLOB_SHA256
        ).from(IMAGES)
        .join(IMAGE_BLOBS)
        .on(IMAGE_BLOBS.IMAGE_BLOB_ID.eq(IMAGES.IMAGE_ID))
        .where(IMAGES.IMAGE_ID.eq(imageId.value()))
        .fetchOptional();

    if (imageRecOpt.isEmpty()) {
      model.setImageCaptionsAssigned(List.of());
      model.setImageSelected(Optional.empty());
      return LCommandUndoable.COMMAND_NOT_UNDOABLE;
    }

    final var imageRec =
      imageRecOpt.get();

    final var captions =
      context.select(
          CAPTIONS.CAPTION_ID,
          CAPTIONS.CAPTION_TEXT,
          LCommandModelUpdates.COUNT_FIELD)
        .from(CAPTIONS)
        .join(IMAGE_CAPTIONS)
        .on(IMAGE_CAPTIONS.IMAGE_CAPTION_CAPTION.eq(CAPTIONS.CAPTION_ID))
        .leftJoin(IMAGE_CAPTIONS_COUNTS)
        .on(IMAGE_CAPTIONS_COUNTS.COUNT_CAPTION_ID.eq(CAPTIONS.CAPTION_ID))
        .where(IMAGE_CAPTIONS.IMAGE_CAPTION_IMAGE.eq(imageId.value()))
        .orderBy(CAPTIONS.CAPTION_TEXT.asc())
        .stream()
        .map(LCommandImageSelect::mapRecord)
        .toList();

    model.setImageCaptionsAssigned(captions);
    model.setImageSelected(
      Optional.of(
        new LImageWithID(
          imageId,
          new LImage(
            imageRec.get(IMAGES.IMAGE_NAME),
            Optional.ofNullable(imageRec.get(IMAGES.IMAGE_FILE))
              .map(Paths::get),
            Optional.ofNullable(imageRec.get(IMAGES.IMAGE_SOURCE))
              .map(URI::create),
            new LHashSHA256(imageRec.get(IMAGE_BLOBS.IMAGE_BLOB_SHA256))
          )
        )
      )
    );
    return LCommandUndoable.COMMAND_NOT_UNDOABLE;
  }

  @Override
  protected void onUndo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void onRedo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Properties toProperties()
  {
    return new Properties();
  }

  @Override
  public String describe()
  {
    return "Select image";
  }
}
