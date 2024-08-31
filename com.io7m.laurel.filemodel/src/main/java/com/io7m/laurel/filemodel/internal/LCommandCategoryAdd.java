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
import org.jooq.DSLContext;

import java.util.List;
import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.CATEGORIES;

/**
 * Add a category.
 */

public final class LCommandCategoryAdd
  extends LCommandAbstract<LCategory>
{
  private long id;
  private String text;

  /**
   * Add a category.
   */

  public LCommandCategoryAdd()
  {

  }

  /**
   * Add a category.
   *
   * @return A command factory
   */

  public static LCommandFactoryType<LCategory> provider()
  {
    return new LCommandFactory<>(
      LCommandCategoryAdd.class.getCanonicalName(),
      LCommandCategoryAdd::fromProperties
    );
  }

  private static LCommandCategoryAdd fromProperties(
    final Properties p)
  {
    final var c = new LCommandCategoryAdd();
    c.id = Long.parseUnsignedLong(p.getProperty("id"));
    c.text = p.getProperty("text");
    c.setExecuted(true);
    return c;
  }

  private static List<LCategory> listCategories(
    final LDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    return context.select(CATEGORIES.CATEGORY_TEXT)
      .from(CATEGORIES)
      .orderBy(CATEGORIES.CATEGORY_TEXT.asc())
      .stream()
      .map(r -> new LCategory(r.get(CATEGORIES.CATEGORY_TEXT)))
      .toList();
  }

  @Override
  protected LCommandUndoable onExecute(
    final LFileModel model,
    final LDatabaseTransactionType transaction,
    final LCategory category)
  {
    final var context =
      transaction.get(DSLContext.class);

    model.eventWithoutProgress("Adding category '%s'.", category);

    final var recOpt =
      context.insertInto(CATEGORIES)
        .set(CATEGORIES.CATEGORY_TEXT, category.text())
        .onDuplicateKeyIgnore()
        .returning(CATEGORIES.CATEGORY_ID)
        .fetchOptional();

    if (recOpt.isEmpty()) {
      model.eventWithoutProgress("Category '%s' already existed.", category);
      return LCommandUndoable.COMMAND_NOT_UNDOABLE;
    }

    final var rec = recOpt.get();
    this.id = rec.get(CATEGORIES.CATEGORY_ID).longValue();
    this.text = category.text();

    model.setCategoriesAll(listCategories(transaction));
    model.eventWithoutProgress("Category '%s' added.", category);
    return LCommandUndoable.COMMAND_UNDOABLE;
  }

  @Override
  protected void onUndo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    model.eventWithoutProgress("Deleting category '%s'.", this.text);

    context.deleteFrom(CATEGORIES)
      .where(CATEGORIES.CATEGORY_ID.eq(this.id))
      .execute();

    model.setCategoriesAll(listCategories(transaction));
  }

  @Override
  protected void onRedo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    model.eventWithoutProgress("Adding category '%s'.", this.text);

    context.insertInto(CATEGORIES)
      .set(CATEGORIES.CATEGORY_ID, this.id)
      .set(CATEGORIES.CATEGORY_TEXT, this.text)
      .onDuplicateKeyUpdate()
      .set(CATEGORIES.CATEGORY_TEXT, this.text)
      .execute();

    model.setCategoriesAll(listCategories(transaction));
    model.eventWithoutProgress("Category '%s' added.", this.text);
  }

  @Override
  public Properties toProperties()
  {
    final var properties = new Properties();
    properties.setProperty("id", Long.toUnsignedString(this.id));
    properties.setProperty("text", this.text);
    return properties;
  }

  @Override
  public String describe()
  {
    return "Add category '%s'".formatted(this.text);
  }
}
