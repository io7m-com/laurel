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
import org.jooq.DSLContext;

import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.GLOBAL_CAPTIONS;

/**
 * Modify a global caption.
 */

public final class LCommandGlobalCaptionModify
  extends LCommandAbstract<LCaption>
{
  private SavedData savedData;

  private record SavedData(
    long id,
    String oldText,
    String newText)
  {

  }

  /**
   * Modify a global caption.
   */

  public LCommandGlobalCaptionModify()
  {

  }

  /**
   * Modify a global caption.
   *
   * @return A command factory
   */

  public static LCommandFactoryType<LCaption> provider()
  {
    return new LCommandFactory<>(
      LCommandGlobalCaptionModify.class.getCanonicalName(),
      LCommandGlobalCaptionModify::fromProperties
    );
  }

  private static LCommandGlobalCaptionModify fromProperties(
    final Properties p)
  {
    final var c = new LCommandGlobalCaptionModify();

    c.savedData = new SavedData(
      Long.parseUnsignedLong(p.getProperty("id")),
      p.getProperty("oldText"),
      p.getProperty("newText")
    );

    c.setExecuted(true);
    return c;
  }

  @Override
  protected LCommandUndoable onExecute(
    final LFileModel model,
    final LDatabaseTransactionType transaction,
    final LCaption caption)
  {
    final var context =
      transaction.get(DSLContext.class);

    final var rec =
      context.select(
          GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID,
          GLOBAL_CAPTIONS.GLOBAL_CAPTION_TEXT
        ).from(GLOBAL_CAPTIONS)
        .where(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID.eq(caption.id().value()))
        .fetchOne();

    if (rec == null) {
      return LCommandUndoable.COMMAND_NOT_UNDOABLE;
    }

    context.update(GLOBAL_CAPTIONS)
      .set(GLOBAL_CAPTIONS.GLOBAL_CAPTION_TEXT, caption.name().text())
      .where(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID.eq(caption.id().value()))
      .execute();

    this.savedData =
      new SavedData(
        rec.get(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID),
        rec.get(GLOBAL_CAPTIONS.GLOBAL_CAPTION_TEXT),
        caption.name().text()
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

    context.update(GLOBAL_CAPTIONS)
      .set(GLOBAL_CAPTIONS.GLOBAL_CAPTION_TEXT, savedData.oldText)
      .where(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID.eq(savedData.id))
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

    context.update(GLOBAL_CAPTIONS)
      .set(GLOBAL_CAPTIONS.GLOBAL_CAPTION_TEXT, savedData.newText)
      .where(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID.eq(savedData.id))
      .execute();

    model.setGlobalCaptions(LCommandModelUpdates.listGlobalCaptions(context));
  }

  @Override
  public Properties toProperties()
  {
    final var p = new Properties();
    p.setProperty("id", Long.toUnsignedString(this.savedData.id));
    p.setProperty("oldText", this.savedData.oldText);
    p.setProperty("newText", this.savedData.newText);
    return p;
  }

  @Override
  public String describe()
  {
    return "Modify global caption(s)";
  }
}
