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

import com.io7m.laurel.model.LCaptionID;
import org.jooq.DSLContext;

import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.GLOBAL_CAPTIONS;

/**
 * Move a global caption to a lower order.
 */

public final class LCommandGlobalCaptionOrderLower
  extends LCommandAbstract<LCaptionID>
{
  private SavedData savedData;

  private record SavedData(
    long thisID,
    long thisOrder,
    long thatID,
    long thatOrder)
  {

  }

  /**
   * Move a global caption to a lower order.
   */

  public LCommandGlobalCaptionOrderLower()
  {

  }

  /**
   * Move a global caption to a lower order.
   *
   * @return A command factory
   */

  public static LCommandFactoryType<LCaptionID> provider()
  {
    return new LCommandFactory<>(
      LCommandGlobalCaptionOrderLower.class.getCanonicalName(),
      LCommandGlobalCaptionOrderLower::fromProperties
    );
  }

  private static LCommandGlobalCaptionOrderLower fromProperties(
    final Properties p)
  {
    final var c = new LCommandGlobalCaptionOrderLower();

    c.savedData = new SavedData(
      Long.parseUnsignedLong(p.getProperty("thisID")),
      Long.parseUnsignedLong(p.getProperty("thisOrder")),
      Long.parseUnsignedLong(p.getProperty("thatID")),
      Long.parseUnsignedLong(p.getProperty("thatOrder"))
    );

    c.setExecuted(true);
    return c;
  }

  @Override
  protected LCommandUndoable onExecute(
    final LFileModel model,
    final LDatabaseTransactionType transaction,
    final LCaptionID thisID)
  {
    final var context =
      transaction.get(DSLContext.class);

    final var thisOrder =
      context.select(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ORDER)
        .from(GLOBAL_CAPTIONS)
        .where(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID.eq(thisID.value()))
        .fetchOne(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ORDER);

    final var before =
      context.select(
          GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID,
          GLOBAL_CAPTIONS.GLOBAL_CAPTION_ORDER)
        .from(GLOBAL_CAPTIONS)
        .where(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ORDER.lt(thisOrder))
        .orderBy(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ORDER.desc())
        .limit(1)
        .fetchOne();

    if (before == null) {
      return LCommandUndoable.COMMAND_NOT_UNDOABLE;
    }

    final var thatId =
      before.get(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID);
    final var thatOrder =
      before.get(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ORDER);

    final var thisUpdate =
      context.update(GLOBAL_CAPTIONS)
        .set(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ORDER, thatOrder)
        .where(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID.eq(thisID.value()));

    final var thatUpdate =
      context.update(GLOBAL_CAPTIONS)
        .set(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ORDER, thisOrder)
        .where(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID.eq(thatId));

    context.batch(thisUpdate, thatUpdate)
      .execute();

    this.savedData =
      new SavedData(
        thisID.value(),
        thisOrder,
        thatId,
        thatOrder
      );

    model.setGlobalCaptions(LCommandModelUpdates.listGlobalCaptions(context));
    return LCommandUndoable.COMMAND_UNDOABLE;
  }

  @Override
  protected void onUndo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    final var thatUpdate =
      context.update(GLOBAL_CAPTIONS)
        .set(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ORDER, savedData.thatOrder)
        .where(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID.eq(savedData.thatID));

    final var thisUpdate =
      context.update(GLOBAL_CAPTIONS)
        .set(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ORDER, savedData.thisOrder)
        .where(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID.eq(savedData.thisID));

    context.batch(thisUpdate, thatUpdate)
      .execute();

    model.setGlobalCaptions(LCommandModelUpdates.listGlobalCaptions(context));
  }

  @Override
  protected void onRedo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    final var thatUpdate =
      context.update(GLOBAL_CAPTIONS)
        .set(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ORDER, savedData.thisOrder)
        .where(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID.eq(savedData.thatID));

    final var thisUpdate =
      context.update(GLOBAL_CAPTIONS)
        .set(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ORDER, savedData.thatOrder)
        .where(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID.eq(savedData.thisID));

    context.batch(thisUpdate, thatUpdate)
      .execute();

    model.setGlobalCaptions(LCommandModelUpdates.listGlobalCaptions(context));
  }

  @Override
  public Properties toProperties()
  {
    final var p = new Properties();
    p.setProperty("thisID", Long.toUnsignedString(this.savedData.thisID));
    p.setProperty("thisOrder", Long.toUnsignedString(this.savedData.thisOrder));
    p.setProperty("thatID", Long.toUnsignedString(this.savedData.thatID));
    p.setProperty("thatOrder", Long.toUnsignedString(this.savedData.thatOrder));
    return p;
  }

  @Override
  public String describe()
  {
    return "Lower global caption order";
  }
}
