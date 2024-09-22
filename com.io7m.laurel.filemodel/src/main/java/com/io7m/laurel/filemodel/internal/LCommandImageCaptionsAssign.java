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

import com.io7m.laurel.filemodel.LImageCaptionsAssignment;
import org.jooq.DSLContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.IMAGE_CAPTIONS;

/**
 * Assign captions to images.
 */

public final class LCommandImageCaptionsAssign
  extends LCommandAbstract<List<LImageCaptionsAssignment>>
{
  private final ArrayList<SavedData> savedData;

  private record SavedData(
    long imageId,
    long tagId)
  {

  }

  /**
   * Assign captions to images.
   */

  public LCommandImageCaptionsAssign()
  {
    this.savedData = new ArrayList<>();
  }

  /**
   * Assign captions to images.
   *
   * @return A command factory
   */

  public static LCommandFactoryType<List<LImageCaptionsAssignment>> provider()
  {
    return new LCommandFactory<>(
      LCommandImageCaptionsAssign.class.getCanonicalName(),
      LCommandImageCaptionsAssign::fromProperties
    );
  }

  private static LCommandImageCaptionsAssign fromProperties(
    final Properties p)
  {
    final var c = new LCommandImageCaptionsAssign();

    for (int index = 0; index < Integer.MAX_VALUE; ++index) {
      final var categoryIdKey =
        "image.%d.id".formatted(Integer.valueOf(index));
      final var categoryTagKey =
        "image.%d.caption".formatted(Integer.valueOf(index));

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
    final List<LImageCaptionsAssignment> images)
  {
    final var context =
      transaction.get(DSLContext.class);

    final var max =
      images.stream()
        .mapToInt(c -> c.captions().size())
        .sum();

    final var entries =
      images.stream()
        .flatMap(c -> {
          return c.captions()
            .stream()
            .map(t -> Map.entry(c.image(), t));
        })
        .toList();

    int index = 0;
    for (final var entry : entries) {
      final var image =
        entry.getKey();
      final var caption =
        entry.getValue();

      model.eventWithProgressCurrentMax(
        index,
        max,
        "Assigning caption to image '%s'.",
        image
      );

      final var recOpt =
        context.insertInto(IMAGE_CAPTIONS)
          .set(IMAGE_CAPTIONS.IMAGE_CAPTION_IMAGE, image.value())
          .set(IMAGE_CAPTIONS.IMAGE_CAPTION_CAPTION, caption.value())
          .onConflictDoNothing()
          .returning(
            IMAGE_CAPTIONS.IMAGE_CAPTION_IMAGE,
            IMAGE_CAPTIONS.IMAGE_CAPTION_CAPTION)
          .fetchOptional();

      ++index;

      if (recOpt.isEmpty()) {
        model.eventWithProgressCurrentMax(
          index,
          max,
          "Image/caption either did not exist or was already assigned.",
          image
        );
        continue;
      }

      this.savedData.add(
        new SavedData(
          image.value(),
          caption.value()
        )
      );
    }

    model.eventWithoutProgress("Assigned %d captions.", this.savedData.size());
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
        "Unassigning caption from image."
      );

      final var matches =
        IMAGE_CAPTIONS.IMAGE_CAPTION_IMAGE.eq(data.imageId())
          .and(IMAGE_CAPTIONS.IMAGE_CAPTION_CAPTION.eq(data.tagId()));

      context.deleteFrom(IMAGE_CAPTIONS)
        .where(matches)
        .execute();
    }

    model.eventWithoutProgress("Unassigned %d captions.", Integer.valueOf(max));
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
        "Reassigning caption to image."
      );

      context.insertInto(IMAGE_CAPTIONS)
        .set(IMAGE_CAPTIONS.IMAGE_CAPTION_IMAGE, data.imageId)
        .set(IMAGE_CAPTIONS.IMAGE_CAPTION_CAPTION, data.tagId)
        .onConflictDoNothing()
        .execute();
    }

    model.eventWithoutProgress("Assigned %d captions.", Integer.valueOf(max));
    LCommandModelUpdates.updateCaptionsAndCategories(context, model);
  }

  @Override
  public Properties toProperties()
  {
    final var p = new Properties();

    for (int index = 0; index < this.savedData.size(); ++index) {
      final var imageIdKey =
        "image.%d.id".formatted(Integer.valueOf(index));
      final var imageTagKey =
        "image.%d.caption".formatted(Integer.valueOf(index));

      final var data = this.savedData.get(index);
      p.setProperty(imageIdKey, Long.toUnsignedString(data.imageId));
      p.setProperty(imageTagKey, Long.toUnsignedString(data.tagId));
    }

    return p;
  }

  @Override
  public String describe()
  {
    return "Assign captions to images";
  }

}
