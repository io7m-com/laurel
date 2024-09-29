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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.GLOBAL_CAPTIONS;

/**
 * Remove global captions.
 */

public final class LCommandGlobalCaptionsRemove
  extends LCommandAbstract<List<LCaptionID>>
{
  private final ArrayList<SavedData> savedData;

  private record SavedData(
    long id,
    String text,
    long order)
  {

  }

  /**
   * Remove global captions.
   */

  public LCommandGlobalCaptionsRemove()
  {
    this.savedData = new ArrayList<>();
  }

  /**
   * Remove global captions.
   *
   * @return A command factory
   */

  public static LCommandFactoryType<List<LCaptionID>> provider()
  {
    return new LCommandFactory<>(
      LCommandGlobalCaptionsRemove.class.getCanonicalName(),
      LCommandGlobalCaptionsRemove::fromProperties
    );
  }

  private static LCommandGlobalCaptionsRemove fromProperties(
    final Properties p)
  {
    final var c = new LCommandGlobalCaptionsRemove();

    for (int index = 0; index < Integer.MAX_VALUE; ++index) {
      final var idKey =
        "caption.%d.id".formatted(Integer.valueOf(index));
      final var textKey =
        "caption.%d.text".formatted(Integer.valueOf(index));
      final var orderKey =
        "caption.%d.order".formatted(Integer.valueOf(index));

      if (!p.containsKey(idKey)) {
        break;
      }

      final var data =
        new SavedData(
          Long.parseUnsignedLong(p.getProperty(idKey)),
          p.getProperty(textKey),
          Long.parseUnsignedLong(p.getProperty(orderKey))
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
    final List<LCaptionID> captions)
  {
    final var context =
      transaction.get(DSLContext.class);

    final var max = captions.size();
    for (int index = 0; index < max; ++index) {
      final var caption = captions.get(index);
      model.eventWithProgressCurrentMax(
        index,
        max,
        "Removing caption '%s'",
        caption);

      final var recOpt =
        context.deleteFrom(GLOBAL_CAPTIONS)
          .where(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID.eq(caption.value()))
          .returning(
            GLOBAL_CAPTIONS.GLOBAL_CAPTION_TEXT,
            GLOBAL_CAPTIONS.GLOBAL_CAPTION_ORDER)
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
          caption.value(),
          rec.get(GLOBAL_CAPTIONS.GLOBAL_CAPTION_TEXT),
          rec.get(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ORDER)
        )
      );
    }

    model.eventWithoutProgress("Removed %d captions.", this.savedData.size());
    model.setGlobalCaptions(LCommandModelUpdates.listGlobalCaptions(context));

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
        "Re-adding caption '%s'",
        data.text
      );

      context.insertInto(GLOBAL_CAPTIONS)
        .set(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID, data.id)
        .set(GLOBAL_CAPTIONS.GLOBAL_CAPTION_TEXT, data.text)
        .set(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ORDER, data.order)
        .onConflictDoNothing()
        .execute();
    }

    model.eventWithoutProgress("Re-added %d captions.", Integer.valueOf(max));
    model.setGlobalCaptions(LCommandModelUpdates.listGlobalCaptions(context));
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
        "Deleting caption '%s'",
        data.text
      );

      context.deleteFrom(GLOBAL_CAPTIONS)
        .where(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID.eq(data.id))
        .execute();
    }

    model.eventWithoutProgress("Deleted %d captions.", Integer.valueOf(max));
    model.setGlobalCaptions(LCommandModelUpdates.listGlobalCaptions(context));
  }

  @Override
  public Properties toProperties()
  {
    final var p = new Properties();

    for (int index = 0; index < this.savedData.size(); ++index) {
      final var idKey =
        "caption.%d.id".formatted(Integer.valueOf(index));
      final var textKey =
        "caption.%d.text".formatted(Integer.valueOf(index));
      final var orderKey =
        "caption.%d.order".formatted(Integer.valueOf(index));

      final var data = this.savedData.get(index);
      p.setProperty(idKey, Long.toUnsignedString(data.id));
      p.setProperty(textKey, data.text);
      p.setProperty(orderKey, Long.toUnsignedString(data.order));
    }

    return p;
  }

  @Override
  public String describe()
  {
    return "Remove global caption(s)";
  }
}
