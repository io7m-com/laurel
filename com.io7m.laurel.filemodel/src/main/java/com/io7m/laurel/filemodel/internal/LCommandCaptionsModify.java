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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.CAPTIONS;

/**
 * Update captions.
 */

public final class LCommandCaptionsModify
  extends LCommandAbstract<List<LCaption>>
{
  private final ArrayList<SavedData> savedData;

  private record SavedData(
    long id,
    String textNew,
    String textOld)
  {

  }

  /**
   * Update captions.
   */

  public LCommandCaptionsModify()
  {
    this.savedData = new ArrayList<>();
  }

  /**
   * Update captions.
   *
   * @return A command factory
   */

  public static LCommandFactoryType<List<LCaption>> provider()
  {
    return new LCommandFactory<>(
      LCommandCaptionsModify.class.getCanonicalName(),
      LCommandCaptionsModify::fromProperties
    );
  }

  private static LCommandCaptionsModify fromProperties(
    final Properties p)
  {
    final var c = new LCommandCaptionsModify();

    for (int index = 0; index < Integer.MAX_VALUE; ++index) {
      final var idKey =
        "caption.%d.id".formatted(Integer.valueOf(index));
      final var textOldKey =
        "caption.%d.textOld".formatted(Integer.valueOf(index));
      final var textNewKey =
        "caption.%d.textNew".formatted(Integer.valueOf(index));

      if (!p.containsKey(idKey)) {
        break;
      }

      final var data =
        new SavedData(
          Long.parseUnsignedLong(p.getProperty(idKey)),
          p.getProperty(textNewKey),
          p.getProperty(textOldKey)
        );

      c.savedData.add(data);
    }

    c.setExecuted(true);
    return c;
  }

  @Override
  protected LCommandUndoable onExecute(
    final LFileModel model,
    final LDatabaseTransactionType transaction,
    final List<LCaption> captions)
  {
    final var context =
      transaction.get(DSLContext.class);

    final var max = captions.size();
    for (int index = 0; index < max; ++index) {
      final var caption = captions.get(index);
      model.eventWithProgressCurrentMax(
        index,
        max,
        "Updating caption '%s'",
        caption
      );

      final var recOpt =
        context.update(CAPTIONS)
          .set(CAPTIONS.CAPTION_TEXT, caption.name().text())
          .where(CAPTIONS.CAPTION_ID.eq(caption.id().value()))
          .returning(CAPTIONS.CAPTION_TEXT)
          .fetchOptional();

      if (recOpt.isEmpty()) {
        model.eventWithProgressCurrentMax(
          index,
          max,
          "Caption '%s' did not exist.",
          caption
        );
        continue;
      }

      final var rec = recOpt.get();
      this.savedData.add(
        new SavedData(
          caption.id().value(),
          caption.name().text(),
          rec.get(CAPTIONS.CAPTION_TEXT)
        )
      );
    }

    model.eventWithoutProgress("Updated %d captions.", this.savedData.size());
    LCommandModelUpdates.updateCaptionsAndCategories(context, model);

    if (!this.savedData.isEmpty()) {
      return LCommandUndoable.COMMAND_UNDOABLE;
    }

    return LCommandUndoable.COMMAND_NOT_UNDOABLE;
  }

  @Override
  protected void onUndo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    final var max = this.savedData.size();
    for (int index = 0; index < max; ++index) {
      final var data = this.savedData.get(index);
      model.eventWithProgressCurrentMax(
        index,
        max,
        "Restoring caption '%s'",
        data.textOld
      );

      context.update(CAPTIONS)
        .set(CAPTIONS.CAPTION_TEXT, data.textOld)
        .where(CAPTIONS.CAPTION_ID.eq(data.id))
        .execute();
    }

    model.eventWithoutProgress("Restored %d captions.", Integer.valueOf(max));
    LCommandModelUpdates.updateCaptionsAndCategories(context, model);
  }

  @Override
  protected void onRedo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    final var max = this.savedData.size();
    for (int index = 0; index < max; ++index) {
      final var data = this.savedData.get(index);
      model.eventWithProgressCurrentMax(
        index,
        max,
        "Adding caption '%s'",
        data.textNew
      );

      context.update(CAPTIONS)
        .set(CAPTIONS.CAPTION_TEXT, data.textNew)
        .where(CAPTIONS.CAPTION_ID.eq(data.id))
        .execute();
    }

    model.eventWithoutProgress("Added %d captions.", Integer.valueOf(max));
    LCommandModelUpdates.updateCaptionsAndCategories(context, model);
  }

  @Override
  public Properties toProperties()
  {
    final var p = new Properties();

    for (int index = 0; index < this.savedData.size(); ++index) {
      final var idKey =
        "caption.%d.id".formatted(Integer.valueOf(index));
      final var textNewKey =
        "caption.%d.textNew".formatted(Integer.valueOf(index));
      final var textOldKey =
        "caption.%d.textOld".formatted(Integer.valueOf(index));

      final var data = this.savedData.get(index);
      p.setProperty(idKey, Long.toUnsignedString(data.id));
      p.setProperty(textNewKey, data.textNew);
      p.setProperty(textOldKey, data.textOld);
    }

    return p;
  }

  @Override
  public String describe()
  {
    return "Update caption(s)";
  }
}
