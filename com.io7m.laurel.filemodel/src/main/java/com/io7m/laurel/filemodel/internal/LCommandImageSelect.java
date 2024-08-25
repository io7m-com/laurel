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

import com.io7m.laurel.model.LHashSHA256;
import com.io7m.laurel.model.LImage;
import com.io7m.laurel.model.LTag;
import org.jooq.DSLContext;

import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.IMAGES;
import static com.io7m.laurel.filemodel.internal.Tables.IMAGE_BLOBS;
import static com.io7m.laurel.filemodel.internal.Tables.IMAGE_TAGS;
import static com.io7m.laurel.filemodel.internal.Tables.TAGS;

/**
 * Select an image.
 */

public final class LCommandImageSelect
  extends LCommandAbstract<Optional<String>>
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

  public static LCommandFactoryType<Optional<String>> provider()
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

  private static LTag mapRecord(
    final org.jooq.Record r)
  {
    return new LTag(r.get(TAGS.TAG_TEXT));
  }

  @Override
  protected LCommandUndoable onExecute(
    final LFileModel model,
    final LDatabaseTransactionType transaction,
    final Optional<String> request)
  {
    final var context =
      transaction.get(DSLContext.class);

    if (request.isEmpty()) {
      model.setTagsAssigned(List.of());
      model.setImageSelected(Optional.empty());
      return LCommandUndoable.COMMAND_NOT_UNDOABLE;
    }

    final var imageName =
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
        .where(IMAGES.IMAGE_NAME.eq(imageName))
        .fetchOptional();

    if (imageRecOpt.isEmpty()) {
      model.setTagsAssigned(List.of());
      model.setImageSelected(Optional.empty());
      return LCommandUndoable.COMMAND_NOT_UNDOABLE;
    }

    final var imageRec =
      imageRecOpt.get();
    final var imageId =
      imageRec.get(IMAGES.IMAGE_ID);

    final var tags =
      context.select(TAGS.TAG_TEXT)
        .from(TAGS)
        .join(IMAGE_TAGS)
        .on(IMAGE_TAGS.IMAGE_TAG_TAG.eq(TAGS.TAG_ID))
        .where(IMAGE_TAGS.IMAGE_TAG_IMAGE.eq(imageId))
        .orderBy(TAGS.TAG_TEXT.asc())
        .stream()
        .map(LCommandImageSelect::mapRecord)
        .toList();

    model.setTagsAssigned(tags);
    model.setImageSelected(
      Optional.of(
        new LImage(
          imageName,
          Optional.ofNullable(imageRec.get(IMAGES.IMAGE_FILE))
            .map(Paths::get),
          Optional.ofNullable(imageRec.get(IMAGES.IMAGE_SOURCE))
            .map(URI::create),
          new LHashSHA256(imageRec.get(IMAGE_BLOBS.IMAGE_BLOB_SHA256))
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
