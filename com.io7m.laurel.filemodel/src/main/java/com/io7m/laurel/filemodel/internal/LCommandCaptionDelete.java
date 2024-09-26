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
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static com.io7m.laurel.filemodel.internal.Tables.CAPTIONS;
import static com.io7m.laurel.filemodel.internal.Tables.CAPTION_CATEGORIES;
import static com.io7m.laurel.filemodel.internal.Tables.IMAGE_CAPTIONS;

/**
 * Delete captions.
 */

public final class LCommandCaptionDelete
  extends LCommandAbstract<Set<LCaptionID>>
{
  private final ArrayList<SavedData> savedData;

  private record SavedData(
    long captionId,
    String captionText,
    Set<Long> images,
    Set<Long> categories)
  {

  }

  /**
   * Delete captions.
   */

  public LCommandCaptionDelete()
  {
    this.savedData = new ArrayList<>();
  }

  /**
   * Delete captions.
   *
   * @return A command factory
   */

  public static LCommandFactoryType<Set<LCaptionID>> provider()
  {
    return new LCommandFactory<>(
      LCommandCaptionDelete.class.getCanonicalName(),
      LCommandCaptionDelete::fromProperties
    );
  }

  private static LCommandCaptionDelete fromProperties(
    final Properties p)
  {
    final var c = new LCommandCaptionDelete();

    for (int index = 0; index < Integer.MAX_VALUE; ++index) {
      final var idKey =
        "caption.%d.id".formatted(Integer.valueOf(index));
      final var textKey =
        "caption.%d.text".formatted(Integer.valueOf(index));
      final var categoriesKey =
        "caption.%d.categories".formatted(Integer.valueOf(index));
      final var imagesKey =
        "caption.%d.images".formatted(Integer.valueOf(index));

      if (!p.containsKey(idKey)) {
        break;
      }

      final var data =
        new SavedData(
          Long.parseUnsignedLong(p.getProperty(idKey)),
          p.getProperty(textKey),
          Arrays.stream(p.getProperty(imagesKey)
                          .split(","))
            .filter(x -> !x.isEmpty())
            .map(x -> Long.parseUnsignedLong(x))
            .collect(Collectors.toSet()),
          Arrays.stream(p.getProperty(categoriesKey)
                          .split(","))
            .filter(x -> !x.isEmpty())
            .map(x -> Long.parseUnsignedLong(x))
            .collect(Collectors.toSet())
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
    final Set<LCaptionID> captions)
  {
    final var context =
      transaction.get(DSLContext.class);

    int index = 0;
    for (final var caption : captions) {
      model.eventWithProgressCurrentMax(
        index,
        captions.size(),
        "Deleting caption %s",
        caption
      );
      ++index;

      final var categories =
        context.select(CAPTION_CATEGORIES.CAPTION_CATEGORY_ID)
          .from(CAPTION_CATEGORIES)
          .where(CAPTION_CATEGORIES.CAPTION_CAPTION_ID.eq(caption.value()))
          .orderBy(CAPTION_CATEGORIES.CAPTION_CAPTION_ID)
          .fetchSet(CAPTION_CATEGORIES.CAPTION_CATEGORY_ID);

      final var images =
        context.select(IMAGE_CAPTIONS.IMAGE_CAPTION_IMAGE)
          .from(IMAGE_CAPTIONS)
          .where(IMAGE_CAPTIONS.IMAGE_CAPTION_CAPTION.eq(caption.value()))
          .orderBy(IMAGE_CAPTIONS.IMAGE_CAPTION_IMAGE)
          .fetchSet(IMAGE_CAPTIONS.IMAGE_CAPTION_IMAGE);

      final var deleted =
        context.deleteFrom(CAPTIONS)
          .where(CAPTIONS.CAPTION_ID.eq(caption.value()))
          .returning(CAPTIONS.CAPTION_TEXT)
          .fetchOne(CAPTIONS.CAPTION_TEXT);

      if (deleted == null) {
        continue;
      }

      this.savedData.add(
        new SavedData(caption.value(), deleted, images, categories)
      );
    }

    if (this.savedData.isEmpty()) {
      return LCommandUndoable.COMMAND_NOT_UNDOABLE;
    } else {
      LCommandModelUpdates.updateCaptionsAndCategories(context, model);
      return LCommandUndoable.COMMAND_UNDOABLE;
    }
  }

  @Override
  protected void onUndo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    int index = 0;
    for (final var data : this.savedData) {
      model.eventWithProgressCurrentMax(
        index,
        this.savedData.size(),
        "Undeleting caption %s",
        data.captionId
      );
      ++index;

      context.insertInto(CAPTIONS)
        .set(CAPTIONS.CAPTION_ID, data.captionId)
        .set(CAPTIONS.CAPTION_TEXT, data.captionText)
        .onConflictDoNothing()
        .execute();

      for (final var category : data.categories) {
        context.insertInto(CAPTION_CATEGORIES)
          .set(CAPTION_CATEGORIES.CAPTION_CAPTION_ID, data.captionId)
          .set(CAPTION_CATEGORIES.CAPTION_CATEGORY_ID, category)
          .onConflictDoNothing()
          .execute();
      }

      for (final var image : data.images) {
        context.insertInto(IMAGE_CAPTIONS)
          .set(IMAGE_CAPTIONS.IMAGE_CAPTION_CAPTION, data.captionId)
          .set(IMAGE_CAPTIONS.IMAGE_CAPTION_IMAGE, image)
          .onConflictDoNothing()
          .execute();
      }
    }

    LCommandModelUpdates.updateCaptionsAndCategories(context, model);
  }

  @Override
  protected void onRedo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    int index = 0;
    for (final var data : this.savedData) {
      model.eventWithProgressCurrentMax(
        index,
        this.savedData.size(),
        "Deleting caption %s",
        data.captionId
      );
      ++index;

      context.deleteFrom(CAPTIONS)
        .where(CAPTIONS.CAPTION_ID.eq(data.captionId))
        .execute();
    }

    LCommandModelUpdates.updateCaptionsAndCategories(context, model);
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
      final var categoriesKey =
        "caption.%d.categories".formatted(Integer.valueOf(index));
      final var imagesKey =
        "caption.%d.images".formatted(Integer.valueOf(index));

      final var data = this.savedData.get(index);
      p.setProperty(idKey, Long.toUnsignedString(data.captionId));
      p.setProperty(textKey, data.captionText);
      p.setProperty(
        categoriesKey,
        data.categories.stream()
          .map(Long::toUnsignedString)
          .collect(Collectors.joining(",")));
      p.setProperty(
        imagesKey,
        data.images.stream()
          .map(Long::toUnsignedString)
          .collect(Collectors.joining(",")));
    }

    return p;
  }

  @Override
  public String describe()
  {
    return "Delete captions";
  }

}
