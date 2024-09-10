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

import org.jooq.DSLContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.CATEGORIES;
import static com.io7m.laurel.filemodel.internal.Tables.TAGS;
import static com.io7m.laurel.filemodel.internal.Tables.TAG_CATEGORIES;

/**
 * Unassign tags from categories.
 */

public final class LCommandCategoryTagsUnassign
  extends LCommandAbstract<List<LCategoryAndTags>>
{
  private final ArrayList<SavedData> savedData;

  private record SavedData(
    long categoryId,
    long tagId)
  {

  }

  /**
   * Unassign tags from categories.
   */

  public LCommandCategoryTagsUnassign()
  {
    this.savedData = new ArrayList<>();
  }

  /**
   * Unassign tags from categories.
   *
   * @return A command factory
   */

  public static LCommandFactoryType<List<LCategoryAndTags>> provider()
  {
    return new LCommandFactory<>(
      LCommandCategoryTagsUnassign.class.getCanonicalName(),
      LCommandCategoryTagsUnassign::fromProperties
    );
  }

  private static LCommandCategoryTagsUnassign fromProperties(
    final Properties p)
  {
    final var c = new LCommandCategoryTagsUnassign();

    for (int index = 0; index < Integer.MAX_VALUE; ++index) {
      final var categoryIdKey =
        "category.%d.id".formatted(Integer.valueOf(index));
      final var categoryTagKey =
        "category.%d.tag".formatted(Integer.valueOf(index));

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
    final List<LCategoryAndTags> categories)
  {
    final var context =
      transaction.get(DSLContext.class);

    final var max =
      categories.stream()
        .mapToInt(c -> c.tags().size())
        .sum();

    final var entries =
      categories.stream()
        .flatMap(c -> {
          return c.tags()
            .stream()
            .map(t -> Map.entry(c.category(), t));
        })
        .toList();

    int index = 0;
    for (final var entry : entries) {
      final var category =
        entry.getKey();
      final var tag =
        entry.getValue();

      model.eventWithProgressCurrentMax(
        index,
        max,
        "Unassigning tag '%s' from category '%s'.",
        tag,
        category
      );

      final var categoryId =
        context.select(CATEGORIES.CATEGORY_ID)
          .from(CATEGORIES)
          .where(CATEGORIES.CATEGORY_TEXT.eq(category.text()));

      final var tagId =
        context.select(TAGS.TAG_ID)
          .from(TAGS)
          .where(TAGS.TAG_TEXT.eq(tag.text()));

      final var matches =
        TAG_CATEGORIES.TAG_CATEGORY_ID.eq(categoryId)
          .and(TAG_CATEGORIES.TAG_TAG_ID.eq(tagId));

      final var recOpt =
        context.deleteFrom(TAG_CATEGORIES)
          .where(matches)
          .returning(
            TAG_CATEGORIES.TAG_CATEGORY_ID,
            TAG_CATEGORIES.TAG_TAG_ID)
          .fetchOptional();

      ++index;

      if (recOpt.isEmpty()) {
        model.eventWithProgressCurrentMax(
          index,
          max,
          "Category/tag either did not exist or was not assigned.",
          category
        );
        continue;
      }

      final var rec = recOpt.get();
      this.savedData.add(
        new SavedData(
          rec.get(TAG_CATEGORIES.TAG_CATEGORY_ID).longValue(),
          rec.get(TAG_CATEGORIES.TAG_TAG_ID).longValue()
        )
      );
    }

    model.eventWithoutProgress("Unassigned %d tags.", this.savedData.size());
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
        "Reassigning tag to category."
      );

      context.insertInto(TAG_CATEGORIES)
        .set(TAG_CATEGORIES.TAG_CATEGORY_ID, data.categoryId())
        .set(TAG_CATEGORIES.TAG_TAG_ID, data.tagId())
        .onConflictDoNothing()
        .execute();
    }

    model.eventWithoutProgress("Reassigned %d tags.", Integer.valueOf(max));
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
        "Unassigning tag from category."
      );

      final var matches =
        TAG_CATEGORIES.TAG_CATEGORY_ID.eq(data.categoryId())
          .and(TAG_CATEGORIES.TAG_TAG_ID.eq(data.tagId()));

      context.deleteFrom(TAG_CATEGORIES)
        .where(matches)
        .execute();
    }

    model.eventWithoutProgress("Unassigned %d tags.", Integer.valueOf(max));
    LCommandModelUpdates.updateTagsAndCategories(context, model);
  }

  @Override
  public Properties toProperties()
  {
    final var p = new Properties();

    for (int index = 0; index < this.savedData.size(); ++index) {
      final var categoryIdKey =
        "category.%d.id".formatted(Integer.valueOf(index));
      final var categoryTagKey =
        "category.%d.tag".formatted(Integer.valueOf(index));

      final var data = this.savedData.get(index);
      p.setProperty(categoryIdKey, Long.toUnsignedString(data.categoryId));
      p.setProperty(categoryTagKey, Long.toUnsignedString(data.tagId));
    }

    return p;
  }

  @Override
  public String describe()
  {
    return "Assign tags to categories";
  }

}
