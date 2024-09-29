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

import com.io7m.laurel.model.LException;
import org.jooq.DSLContext;

import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.IMAGES;

/**
 * Set an image source.
 */

public final class LCommandImageSourceSet
  extends LCommandAbstract<LImageSourceSet>
{
  private SavedData savedData;

  private record SavedData(
    long savedImageId,
    String savedOldURI,
    String savedNewURI)
  {

  }

  /**
   * Set an image source.
   */

  public LCommandImageSourceSet()
  {

  }

  /**
   * @return A command factory
   */

  public static LCommandFactoryType<LImageSourceSet> provider()
  {
    return new LCommandFactory<>(
      LCommandImageSourceSet.class.getCanonicalName(),
      LCommandImageSourceSet::fromProperties
    );
  }

  private static LCommandImageSourceSet fromProperties(
    final Properties p)
  {
    final var c = new LCommandImageSourceSet();

    c.savedData = new SavedData(
      Long.parseUnsignedLong(p.getProperty("image")),
      p.getProperty("oldURI"),
      p.getProperty("newURI")
    );

    c.setExecuted(true);
    return c;
  }

  @Override
  protected LCommandUndoable onExecute(
    final LFileModel model,
    final LDatabaseTransactionType transaction,
    final LImageSourceSet request)
    throws LException
  {
    final var context =
      transaction.get(DSLContext.class);

    final var old =
      context.select(IMAGES.IMAGE_SOURCE)
        .from(IMAGES)
        .where(IMAGES.IMAGE_ID.eq(request.image().value()))
        .fetchOne(IMAGES.IMAGE_SOURCE);

    final var updated =
      context.update(IMAGES)
        .set(IMAGES.IMAGE_SOURCE, request.source().toString())
        .where(IMAGES.IMAGE_ID.eq(request.image().value()))
        .execute();

    if (updated == 0) {
      return LCommandUndoable.COMMAND_NOT_UNDOABLE;
    }

    this.savedData = new SavedData(
      request.image().value(),
      old,
      request.source().toString()
    );

    model.setImagesAll(LCommandModelUpdates.listImages(context));
    return LCommandUndoable.COMMAND_UNDOABLE;
  }


  @Override
  protected void onUndo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    context.update(IMAGES)
      .set(IMAGES.IMAGE_SOURCE, this.savedData.savedOldURI)
      .where(IMAGES.IMAGE_ID.eq(this.savedData.savedImageId))
      .execute();

    model.setImagesAll(LCommandModelUpdates.listImages(context));
  }

  @Override
  protected void onRedo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    context.update(IMAGES)
      .set(IMAGES.IMAGE_SOURCE, this.savedData.savedNewURI)
      .where(IMAGES.IMAGE_ID.eq(this.savedData.savedImageId))
      .execute();

    model.setImagesAll(LCommandModelUpdates.listImages(context));
  }

  @Override
  public Properties toProperties()
  {
    final var p = new Properties();
    p.setProperty("image", Long.toUnsignedString(this.savedData.savedImageId));
    p.setProperty("oldURI", this.savedData.savedOldURI);
    p.setProperty("newURI", this.savedData.savedNewURI);
    return p;
  }

  @Override
  public String describe()
  {
    return "Set image source";
  }
}
