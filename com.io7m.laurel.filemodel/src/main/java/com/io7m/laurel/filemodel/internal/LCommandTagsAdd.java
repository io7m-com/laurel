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

import com.io7m.laurel.model.LTag;
import org.jooq.DSLContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.TAGS;

/**
 * Add tags.
 */

public final class LCommandTagsAdd
  extends LCommandAbstract<List<LTag>>
{
  private final ArrayList<SavedData> savedData;

  private record SavedData(
    long id,
    String text)
  {

  }

  /**
   * Add tags.
   */

  public LCommandTagsAdd()
  {
    this.savedData = new ArrayList<>();
  }

  /**
   * Add tags.
   *
   * @return A command factory
   */

  public static LCommandFactoryType<List<LTag>> provider()
  {
    return new LCommandFactory<>(
      LCommandTagsAdd.class.getCanonicalName(),
      LCommandTagsAdd::fromProperties
    );
  }

  private static LCommandTagsAdd fromProperties(
    final Properties p)
  {
    final var c = new LCommandTagsAdd();

    for (int index = 0; index < Integer.MAX_VALUE; ++index) {
      final var idKey =
        "tag.%d.id".formatted(Integer.valueOf(index));
      final var textKey =
        "tag.%d.text".formatted(Integer.valueOf(index));

      if (!p.containsKey(idKey)) {
        break;
      }

      final var data =
        new SavedData(
          Long.parseUnsignedLong(p.getProperty(idKey)),
          p.getProperty(textKey)
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
    final List<LTag> tags)
  {
    final var context =
      transaction.get(DSLContext.class);

    final var max = tags.size();
    for (int index = 0; index < max; ++index) {
      final var tag = tags.get(index);
      model.eventWithProgressCurrentMax(index, max, "Adding tag '%s'", tag);

      final var recOpt =
        context.insertInto(TAGS)
          .set(TAGS.TAG_TEXT, tag.text())
          .onDuplicateKeyIgnore()
          .returning(TAGS.TAG_ID)
          .fetchOptional();

      if (recOpt.isEmpty()) {
        model.eventWithProgressCurrentMax(
          index,
          max,
          "Tag '%s' already existed.",
          tag
        );
        continue;
      }

      final var rec = recOpt.get();
      this.savedData.add(
        new SavedData(
          rec.get(TAGS.TAG_ID).longValue(),
          tag.text()
        )
      );
    }

    model.eventWithoutProgress("Added %d tags.", this.savedData.size());
    LCommandModelUpdates.updateTagsAndCategories(context, model);

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
        "Removing tag '%s'",
        data.text
      );
      context.deleteFrom(TAGS)
        .where(TAGS.TAG_ID.eq(data.id))
        .execute();
    }

    model.eventWithoutProgress("Removed %d tags.", Integer.valueOf(max));
    LCommandModelUpdates.updateTagsAndCategories(context, model);
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
        "Adding tag '%s'",
        data.text
      );
      context.insertInto(TAGS)
        .set(TAGS.TAG_ID, data.id)
        .set(TAGS.TAG_TEXT, data.text)
        .onDuplicateKeyUpdate()
        .set(TAGS.TAG_TEXT, data.text)
        .execute();
    }

    model.eventWithoutProgress("Added %d tags.", Integer.valueOf(max));
    LCommandModelUpdates.updateTagsAndCategories(context, model);
  }

  @Override
  public Properties toProperties()
  {
    final var p = new Properties();

    for (int index = 0; index < this.savedData.size(); ++index) {
      final var idKey =
        "tag.%d.id".formatted(Integer.valueOf(index));
      final var textKey =
        "tag.%d.text".formatted(Integer.valueOf(index));

      final var data = this.savedData.get(index);
      p.setProperty(idKey, Long.toUnsignedString(data.id));
      p.setProperty(textKey, data.text);
    }

    return p;
  }

  @Override
  public String describe()
  {
    return "Add tag(s)";
  }
}
