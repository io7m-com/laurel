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

import com.io7m.laurel.model.LCategoryID;
import org.jooq.DSLContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.CATEGORIES;

/**
 * Set categories as not required.
 */

public final class LCommandCategoriesUnsetRequired
  extends LCommandAbstract<List<LCategoryID>>
{
  private final ArrayList<SavedData> savedData;

  private record SavedData(long id)
  {

  }

  /**
   * Set categories as not required.
   */

  public LCommandCategoriesUnsetRequired()
  {
    this.savedData = new ArrayList<>();
  }

  /**
   * Set categories as not required.
   *
   * @return A command factory
   */

  public static LCommandFactoryType<List<LCategoryID>> provider()
  {
    return new LCommandFactory<>(
      LCommandCategoriesUnsetRequired.class.getCanonicalName(),
      LCommandCategoriesUnsetRequired::fromProperties
    );
  }

  private static LCommandCategoriesUnsetRequired fromProperties(
    final Properties p)
  {
    final var c = new LCommandCategoriesUnsetRequired();

    for (int index = 0; index < Integer.MAX_VALUE; ++index) {
      final var idKey =
        "category.%d.id".formatted(Integer.valueOf(index));

      if (!p.containsKey(idKey)) {
        break;
      }

      final var data =
        new SavedData(
          Long.parseUnsignedLong(p.getProperty(idKey))
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
    final List<LCategoryID> categories)
  {
    final var context =
      transaction.get(DSLContext.class);

    final var max = categories.size();
    for (int index = 0; index < max; ++index) {
      final var category = categories.get(index);
      model.eventWithProgressCurrentMax(
        index,
        max,
        "Updating category '%s'",
        category);

      final var recOpt =
        context.update(CATEGORIES)
          .set(CATEGORIES.CATEGORY_REQUIRED, 0L)
          .where(CATEGORIES.CATEGORY_ID.eq(category.value())
                   .and(CATEGORIES.CATEGORY_REQUIRED.eq(1L)))
          .returning(CATEGORIES.CATEGORY_ID)
          .fetchOptional();

      if (recOpt.isEmpty()) {
        model.eventWithProgressCurrentMax(
          index,
          max,
          "Category '%s' either did not exist or was not required.",
          category
        );
        continue;
      }

      final var rec = recOpt.get();
      this.savedData.add(
        new SavedData(
          rec.get(CATEGORIES.CATEGORY_ID).longValue()
        )
      );
    }

    model.eventWithoutProgress("Updated %d categories.", this.savedData.size());
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
        "Updating category '%s'",
        data.id
      );
      context.update(CATEGORIES)
        .set(CATEGORIES.CATEGORY_REQUIRED, 1L)
        .where(CATEGORIES.CATEGORY_ID.eq(data.id))
        .execute();
    }

    model.eventWithoutProgress("Updated %d categories.", Integer.valueOf(max));
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
        "Updating category '%s'",
        data.id
      );
      context.update(CATEGORIES)
        .set(CATEGORIES.CATEGORY_REQUIRED, 0L)
        .where(CATEGORIES.CATEGORY_ID.eq(data.id))
        .execute();
    }

    model.eventWithoutProgress("Updated %d categories.", Integer.valueOf(max));
    LCommandModelUpdates.updateCaptionsAndCategories(context, model);
  }

  @Override
  public Properties toProperties()
  {
    final var p = new Properties();

    for (int index = 0; index < this.savedData.size(); ++index) {
      final var idKey =
        "category.%d.id".formatted(Integer.valueOf(index));

      final var data = this.savedData.get(index);
      p.setProperty(idKey, Long.toUnsignedString(data.id));
    }

    return p;
  }

  @Override
  public String describe()
  {
    return "Unset categories as required";
  }

}
