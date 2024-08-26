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

import com.io7m.darco.api.DDatabaseUnit;
import org.jooq.DSLContext;

import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.IMAGES;
import static com.io7m.laurel.filemodel.internal.Tables.IMAGE_BLOBS;
import static com.io7m.laurel.filemodel.internal.Tables.REDO;
import static com.io7m.laurel.filemodel.internal.Tables.UNDO;

/**
 * Compact a file.
 */

public final class LCommandCompact
  extends LCommandAbstract<DDatabaseUnit>
{
  /**
   * Compact a file.
   */

  public LCommandCompact()
  {

  }

  /**
   * @return A command factory
   */

  public static LCommandFactoryType<DDatabaseUnit> provider()
  {
    return new LCommandFactory<>(
      LCommandCompact.class.getCanonicalName(),
      LCommandCompact::fromProperties
    );
  }

  private static LCommandCompact fromProperties(
    final Properties p)
  {
    final var c = new LCommandCompact();
    c.setExecuted(true);
    return c;
  }

  @Override
  protected LCommandUndoable onExecute(
    final LFileModel model,
    final LDatabaseTransactionType transaction,
    final DDatabaseUnit request)
  {
    final var context =
      transaction.get(DSLContext.class);

    context.truncate(UNDO)
      .execute();
    context.truncate(REDO)
      .execute();
    context.deleteFrom(IMAGE_BLOBS)
      .where(IMAGE_BLOBS.IMAGE_BLOB_ID.notIn(
        context.select(IMAGES.IMAGE_BLOB)
          .from(IMAGES)
      ))
      .execute();

    model.clearUndo();
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
    return "Compact a file.";
  }
}
