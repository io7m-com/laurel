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

import com.io7m.laurel.model.LCategory;
import com.io7m.laurel.model.LCategoryID;
import com.io7m.laurel.model.LCategoryName;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.CATEGORIES;

/**
 * Select a category.
 */

public final class LCommandCategorySelect
  extends LCommandAbstract<Optional<LCategoryID>>
{
  /**
   * Select a category.
   */

  public LCommandCategorySelect()
  {

  }

  /**
   * @return A command factory
   */

  public static LCommandFactoryType<Optional<LCategoryID>> provider()
  {
    return new LCommandFactory<>(
      LCommandCategorySelect.class.getCanonicalName(),
      LCommandCategorySelect::fromProperties
    );
  }

  private static LCommandCategorySelect fromProperties(
    final Properties p)
  {
    final var c = new LCommandCategorySelect();
    c.setExecuted(true);
    return c;
  }

  @Override
  protected LCommandUndoable onExecute(
    final LFileModel model,
    final LDatabaseTransactionType transaction,
    final Optional<LCategoryID> request)
  {
    final var context =
      transaction.get(DSLContext.class);

    if (request.isEmpty()) {
      model.setCategorySelected(Optional.empty());
      model.setCategoryCaptionsAssigned(List.of());
      return LCommandUndoable.COMMAND_NOT_UNDOABLE;
    }

    final var categoryId =
      request.get();

    final var rec =
      context.select(
        CATEGORIES.CATEGORY_TEXT,
        CATEGORIES.CATEGORY_ID,
        CATEGORIES.CATEGORY_REQUIRED)
        .from(CATEGORIES)
        .where(CATEGORIES.CATEGORY_ID.eq(categoryId.value()))
        .fetchOne();

    model.setCategoryCaptionsAssigned(
      LCommandModelUpdates.listCategoryCaptionsAssigned(context, categoryId)
    );
    model.setCategorySelected(Optional.of(
      new LCategory(
        new LCategoryID(rec.get(CATEGORIES.CATEGORY_ID)),
        new LCategoryName(rec.get(CATEGORIES.CATEGORY_TEXT)),
        rec.get(CATEGORIES.CATEGORY_REQUIRED) != 0L
      )
    ));
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
    return "Select category";
  }
}
