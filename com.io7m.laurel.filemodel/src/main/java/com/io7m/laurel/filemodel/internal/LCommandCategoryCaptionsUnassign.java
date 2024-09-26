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

import com.io7m.laurel.filemodel.LCategoryCaptionsAssignment;
import org.jooq.DSLContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.CAPTION_CATEGORIES;

/**
 * Unassign captions from categories.
 */

public final class LCommandCategoryCaptionsUnassign
  extends LCommandAbstract<List<LCategoryCaptionsAssignment>>
{
  private final ArrayList<SavedData> savedData;

  private record SavedData(
    long categoryId,
    long tagId)
  {

  }

  /**
   * Unassign captions from categories.
   */

  public LCommandCategoryCaptionsUnassign()
  {
    this.savedData = new ArrayList<>();
  }

  /**
   * Unassign captions from categories.
   *
   * @return A command factory
   */

  public static LCommandFactoryType<List<LCategoryCaptionsAssignment>> provider()
  {
    return new LCommandFactory<>(
      LCommandCategoryCaptionsUnassign.class.getCanonicalName(),
      LCommandCategoryCaptionsUnassign::fromProperties
    );
  }

  private static LCommandCategoryCaptionsUnassign fromProperties(
    final Properties p)
  {
    final var c = new LCommandCategoryCaptionsUnassign();

    for (int index = 0; index < Integer.MAX_VALUE; ++index) {
      final var categoryIdKey =
        "category.%d.id".formatted(Integer.valueOf(index));
      final var categoryTagKey =
        "category.%d.caption".formatted(Integer.valueOf(index));

      if (!p.containsKey(categoryIdKey)) {
        break;
      }

      final var data =
        new SavedData(
          Long.parseUnsignedLong(p.getProperty(categoryIdKey)),
          Long.parseUnsignedLong(p.getProperty(categoryTagKey))
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
    final List<LCategoryCaptionsAssignment> categories)
  {
    final var context =
      transaction.get(DSLContext.class);

    final var max =
      categories.stream()
        .mapToInt(c -> c.captions().size())
        .sum();

    final var entries =
      categories.stream()
        .flatMap(c -> {
          return c.captions()
            .stream()
            .map(t -> Map.entry(c.category(), t));
        })
        .toList();

    int index = 0;
    for (final var entry : entries) {
      final var category =
        entry.getKey();
      final var caption =
        entry.getValue();

      model.eventWithProgressCurrentMax(
        index,
        max,
        "Unassigning caption '%s' from category '%s'.",
        caption,
        category
      );

      final var matches =
        CAPTION_CATEGORIES.CAPTION_CATEGORY_ID.eq(category.value())
          .and(CAPTION_CATEGORIES.CAPTION_CAPTION_ID.eq(caption.value()));

      final var recOpt =
        context.deleteFrom(CAPTION_CATEGORIES)
          .where(matches)
          .returning(
            CAPTION_CATEGORIES.CAPTION_CATEGORY_ID,
            CAPTION_CATEGORIES.CAPTION_CAPTION_ID)
          .fetchOptional();

      ++index;

      if (recOpt.isEmpty()) {
        model.eventWithProgressCurrentMax(
          index,
          max,
          "Category/caption either did not exist or was not assigned.",
          category
        );
        continue;
      }

      final var rec = recOpt.get();
      this.savedData.add(
        new SavedData(
          rec.get(CAPTION_CATEGORIES.CAPTION_CATEGORY_ID).longValue(),
          rec.get(CAPTION_CATEGORIES.CAPTION_CAPTION_ID).longValue()
        )
      );
    }

    model.eventWithoutProgress("Unassigned %d captions.", this.savedData.size());
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
        "Reassigning caption to category."
      );

      context.insertInto(CAPTION_CATEGORIES)
        .set(CAPTION_CATEGORIES.CAPTION_CATEGORY_ID, data.categoryId())
        .set(CAPTION_CATEGORIES.CAPTION_CAPTION_ID, data.tagId())
        .onConflictDoNothing()
        .execute();
    }

    model.eventWithoutProgress("Reassigned %d captions.", Integer.valueOf(max));
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
        "Unassigning caption from category."
      );

      final var matches =
        CAPTION_CATEGORIES.CAPTION_CATEGORY_ID.eq(data.categoryId())
          .and(CAPTION_CATEGORIES.CAPTION_CAPTION_ID.eq(data.tagId()));

      context.deleteFrom(CAPTION_CATEGORIES)
        .where(matches)
        .execute();
    }

    model.eventWithoutProgress("Unassigned %d captions.", Integer.valueOf(max));
    LCommandModelUpdates.updateCaptionsAndCategories(context, model);
  }

  @Override
  public Properties toProperties()
  {
    final var p = new Properties();

    for (int index = 0; index < this.savedData.size(); ++index) {
      final var categoryIdKey =
        "category.%d.id".formatted(Integer.valueOf(index));
      final var categoryTagKey =
        "category.%d.caption".formatted(Integer.valueOf(index));

      final var data = this.savedData.get(index);
      p.setProperty(categoryIdKey, Long.toUnsignedString(data.categoryId));
      p.setProperty(categoryTagKey, Long.toUnsignedString(data.tagId));
    }

    return p;
  }

  @Override
  public String describe()
  {
    return "Unassign captions from categories";
  }

}
