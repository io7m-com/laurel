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

import com.io7m.laurel.model.LException;
import com.io7m.laurel.model.LMetadataValue;
import org.jooq.DSLContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.METADATA;

/**
 * Remove metadata.
 */

public final class LCommandMetadataRemove
  extends LCommandAbstract<List<LMetadataValue>>
{
  private final ArrayList<SavedData> savedData;

  private record SavedData(
    String name,
    String value)
  {

  }

  /**
   * Remove metadata.
   */

  public LCommandMetadataRemove()
  {
    this.savedData = new ArrayList<>();
  }

  /**
   * @return A command factory
   */

  public static LCommandFactoryType<List<LMetadataValue>> provider()
  {
    return new LCommandFactory<>(
      LCommandMetadataRemove.class.getCanonicalName(),
      LCommandMetadataRemove::fromProperties
    );
  }

  private static LCommandMetadataRemove fromProperties(
    final Properties p)
  {
    final var c = new LCommandMetadataRemove();

    for (int index = 0; index < Integer.MAX_VALUE; ++index) {
      final var nameKey =
        "metadata.%d.name".formatted(Integer.valueOf(index));
      final var valueKey =
        "metadata.%d.value".formatted(Integer.valueOf(index));

      if (!p.containsKey(nameKey)) {
        break;
      }

      final var data =
        new SavedData(
          p.getProperty(nameKey),
          p.getProperty(valueKey)
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
    final List<LMetadataValue> requests)
    throws LException
  {
    final var context =
      transaction.get(DSLContext.class);

    final var max = requests.size();
    for (int index = 0; index < max; ++index) {
      final var request = requests.get(index);
      model.eventWithProgressCurrentMax(
        index,
        max,
        "Removing metadata '%s'.",
        request.name()
      );

      final var rec =
        context.deleteFrom(METADATA)
          .where(METADATA.META_NAME.eq(request.name()))
          .returning(METADATA.META_VALUE)
          .fetchOne(METADATA.META_VALUE);

      if (rec != null) {
        this.savedData.add(new SavedData(request.name(), rec));
      }
    }

    model.eventWithoutProgress("Removed %d values.", max);
    model.setMetadata(LCommandModelUpdates.listMetadata(context));
    return LCommandUndoable.COMMAND_UNDOABLE;
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
        "Replacing metadata '%s'.",
        data.name
      );

      context.insertInto(METADATA)
        .set(METADATA.META_NAME, data.name)
        .set(METADATA.META_VALUE, data.value)
        .execute();
    }

    model.eventWithoutProgress("Updated %d values.", this.savedData.size());
    model.setMetadata(LCommandModelUpdates.listMetadata(context));
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
        "Removing metadata '%s'.",
        data.name
      );

      context.deleteFrom(METADATA)
        .where(METADATA.META_NAME.eq(data.name()))
        .returning(METADATA.META_VALUE)
        .fetchOne(METADATA.META_VALUE);
    }

    model.eventWithoutProgress("Removed %d values.", this.savedData.size());
    model.setMetadata(LCommandModelUpdates.listMetadata(context));
  }

  @Override
  public Properties toProperties()
  {
    final var p = new Properties();

    for (int index = 0; index < this.savedData.size(); ++index) {
      final var nameKey =
        "metadata.%d.name".formatted(Integer.valueOf(index));
      final var valueKey =
        "metadata.%d.value".formatted(Integer.valueOf(index));

      final var data = this.savedData.get(index);
      p.setProperty(nameKey, data.name);
      p.setProperty(valueKey, data.value);
    }

    return p;
  }

  @Override
  public String describe()
  {
    return "Remove metadata value(s)";
  }
}
