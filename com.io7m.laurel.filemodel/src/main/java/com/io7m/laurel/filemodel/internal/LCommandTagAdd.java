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

import java.util.List;
import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.TAGS;

/**
 * Add a tag.
 */

public final class LCommandTagAdd
  extends LCommandAbstract<LTag>
{
  private long id;
  private String text;

  /**
   * Add a tag.
   */

  public LCommandTagAdd()
  {

  }

  /**
   * Add a tag.
   *
   * @return A command factory
   */

  public static LCommandFactoryType<LTag> provider()
  {
    return new LCommandFactory<>(
      LCommandTagAdd.class.getCanonicalName(),
      LCommandTagAdd::fromProperties
    );
  }

  private static LCommandTagAdd fromProperties(
    final Properties p)
  {
    final var c = new LCommandTagAdd();
    c.id = Long.parseUnsignedLong(p.getProperty("id"));
    c.text = p.getProperty("text");
    c.setExecuted(true);
    return c;
  }

  private static List<LTag> listTags(
    final LDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    return context.select(TAGS.TAG_TEXT)
      .from(TAGS)
      .orderBy(TAGS.TAG_TEXT.asc())
      .stream()
      .map(r -> new LTag(r.get(TAGS.TAG_TEXT)))
      .toList();
  }

  @Override
  protected LCommandUndoable onExecute(
    final LFileModel model,
    final LDatabaseTransactionType transaction,
    final LTag tag)
  {
    final var context =
      transaction.get(DSLContext.class);

    final var recOpt =
      context.insertInto(TAGS)
        .set(TAGS.TAG_TEXT, tag.text())
        .onDuplicateKeyIgnore()
        .returning(TAGS.TAG_ID)
        .fetchOptional();

    if (recOpt.isEmpty()) {
      return LCommandUndoable.COMMAND_NOT_UNDOABLE;
    }

    final var rec = recOpt.get();
    this.id = rec.get(TAGS.TAG_ID).longValue();
    this.text = tag.text();

    model.setTagsAll(listTags(transaction));
    return LCommandUndoable.COMMAND_UNDOABLE;
  }

  @Override
  protected void onUndo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    context.deleteFrom(TAGS)
      .where(TAGS.TAG_ID.eq(this.id))
      .execute();

    model.setTagsAll(listTags(transaction));
  }

  @Override
  protected void onRedo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    context.insertInto(TAGS)
      .set(TAGS.TAG_ID, this.id)
      .set(TAGS.TAG_TEXT, this.text)
      .onDuplicateKeyUpdate()
      .set(TAGS.TAG_TEXT, this.text)
      .execute();

    model.setTagsAll(listTags(transaction));
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
    return "Add tag '%s'".formatted(this.text);
  }
}
