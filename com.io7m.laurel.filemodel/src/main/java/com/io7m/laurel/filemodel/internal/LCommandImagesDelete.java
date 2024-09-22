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
import com.io7m.laurel.model.LImageID;
import org.jooq.DSLContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.io7m.laurel.filemodel.internal.Tables.IMAGES;
import static com.io7m.laurel.filemodel.internal.Tables.IMAGE_CAPTIONS;

/**
 * Add images.
 */

public final class LCommandImagesDelete
  extends LCommandAbstract<List<LImageID>>
{
  private final ArrayList<SavedData> savedData;

  private record SavedData(
    long savedBlobId,
    long savedImageId,
    String savedSourceText,
    String savedFile,
    String savedName,
    Set<Long> savedCaptions)
  {

  }

  /**
   * Add images.
   */

  public LCommandImagesDelete()
  {
    this.savedData = new ArrayList<>();
  }

  /**
   * @return A command factory
   */

  public static LCommandFactoryType<List<LImageID>> provider()
  {
    return new LCommandFactory<>(
      LCommandImagesDelete.class.getCanonicalName(),
      LCommandImagesDelete::fromProperties
    );
  }

  private static LCommandImagesDelete fromProperties(
    final Properties p)
  {
    final var c = new LCommandImagesDelete();

    for (int index = 0; index < Integer.MAX_VALUE; ++index) {
      final var blobKey =
        "image.%d.blob".formatted(Integer.valueOf(index));
      final var idKey =
        "image.%d.id".formatted(Integer.valueOf(index));
      final var sourceKey =
        "image.%d.source".formatted(Integer.valueOf(index));
      final var fileKey =
        "image.%d.file".formatted(Integer.valueOf(index));
      final var nameKey =
        "image.%d.name".formatted(Integer.valueOf(index));
      final var captionsKey =
        "image.%d.captions".formatted(Integer.valueOf(index));

      if (!p.containsKey(idKey)) {
        break;
      }

      final var captionsText =
        p.getProperty(captionsKey);
      final var captionStrings =
        captionsText.split(",");

      final var captionIds =
        Stream.of(captionStrings)
          .filter(s -> !s.isEmpty())
          .map(Long::parseUnsignedLong)
          .collect(Collectors.toSet());

      final var data =
        new SavedData(
          Long.parseUnsignedLong(p.getProperty(blobKey)),
          Long.parseUnsignedLong(p.getProperty(idKey)),
          p.getProperty(sourceKey),
          p.getProperty(fileKey),
          p.getProperty(nameKey),
          captionIds
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
    final List<LImageID> requests)
    throws LException
  {
    final var context =
      transaction.get(DSLContext.class);

    final var max = requests.size();
    for (int index = 0; index < max; ++index) {
      final var id = requests.get(index);
      model.setAttribute("Image", id);
      model.eventWithProgressCurrentMax(index, max, "Deleting image '%s'.", id);

      final var captions =
        LCommandModelUpdates.listImageCaptions(context, id);

      final var deletedOpt =
        context.deleteFrom(IMAGES)
          .where(IMAGES.IMAGE_ID.eq(Long.valueOf(id.value())))
          .returning(
            IMAGES.IMAGE_BLOB,
            IMAGES.IMAGE_SOURCE,
            IMAGES.IMAGE_FILE,
            IMAGES.IMAGE_ID,
            IMAGES.IMAGE_NAME
          )
          .fetchOptional();

      if (deletedOpt.isEmpty()) {
        model.eventWithProgressCurrentMax(
          index,
          max,
          "Image did not exist."
        );
        continue;
      }

      final var deleted = deletedOpt.orElseThrow();
      this.savedData.add(new SavedData(
        deleted.get(IMAGES.IMAGE_BLOB),
        deleted.get(IMAGES.IMAGE_ID),
        deleted.get(IMAGES.IMAGE_SOURCE),
        deleted.get(IMAGES.IMAGE_FILE),
        deleted.get(IMAGES.IMAGE_NAME),
        captions.stream()
          .map(x -> Long.valueOf(x.value()))
          .collect(Collectors.toSet())
      ));
    }

    model.setImagesAll(LCommandModelUpdates.listImages(context));
    LCommandModelUpdates.updateCaptionsAndCategories(context, model);
    model.eventWithoutProgress("Deleted %d images.", Integer.valueOf(max));
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
      final var saved = this.savedData.get(index);
      model.setAttribute("Image", saved.savedImageId);
      model.eventWithProgressCurrentMax(
        index,
        max,
        "Undeleting image '%s'.",
        saved.savedImageId
      );

      context.insertInto(IMAGES)
        .set(IMAGES.IMAGE_ID, saved.savedImageId)
        .set(IMAGES.IMAGE_NAME, saved.savedName)
        .set(IMAGES.IMAGE_BLOB, saved.savedBlobId)
        .set(IMAGES.IMAGE_SOURCE, saved.savedSourceText)
        .set(IMAGES.IMAGE_FILE, saved.savedFile)
        .onConflictDoNothing()
        .execute();

      for (final var tag : saved.savedCaptions) {
        context.insertInto(IMAGE_CAPTIONS)
          .set(IMAGE_CAPTIONS.IMAGE_CAPTION_CAPTION, tag)
          .set(IMAGE_CAPTIONS.IMAGE_CAPTION_IMAGE, saved.savedImageId)
          .onConflictDoNothing()
          .execute();
      }
    }

    model.setImagesAll(LCommandModelUpdates.listImages(context));
    LCommandModelUpdates.updateCaptionsAndCategories(context, model);
    model.eventWithoutProgress("Undeleted %d images.", Integer.valueOf(max));
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
      final var saved = this.savedData.get(index);
      model.setAttribute("Image", saved.savedImageId);
      model.eventWithProgressCurrentMax(
        index,
        max,
        "Re-deleting image '%s'.",
        saved.savedImageId
      );

      context.deleteFrom(IMAGES)
        .where(IMAGES.IMAGE_ID.eq(saved.savedImageId))
        .execute();
    }

    model.setImagesAll(LCommandModelUpdates.listImages(context));
    LCommandModelUpdates.updateCaptionsAndCategories(context, model);
    model.eventWithoutProgress("Re-deleted %d images.", Integer.valueOf(max));
  }

  @Override
  public Properties toProperties()
  {
    final var p = new Properties();

    for (int index = 0; index < this.savedData.size(); ++index) {
      final var blobKey =
        "image.%d.blob".formatted(Integer.valueOf(index));
      final var idKey =
        "image.%d.id".formatted(Integer.valueOf(index));
      final var sourceKey =
        "image.%d.source".formatted(Integer.valueOf(index));
      final var fileKey =
        "image.%d.file".formatted(Integer.valueOf(index));
      final var nameKey =
        "image.%d.name".formatted(Integer.valueOf(index));
      final var captionsKey =
        "image.%d.captions".formatted(Integer.valueOf(index));

      final var data =
        this.savedData.get(index);

      final var captionsText =
        data.savedCaptions
          .stream()
          .map(Long::toUnsignedString)
          .collect(Collectors.joining(","));

      p.setProperty(idKey, Long.toUnsignedString(data.savedImageId));
      p.setProperty(blobKey, Long.toUnsignedString(data.savedBlobId));
      p.setProperty(fileKey, data.savedFile);
      p.setProperty(sourceKey, data.savedSourceText);
      p.setProperty(nameKey, data.savedName);
      p.setProperty(captionsKey, captionsText);
    }

    return p;
  }

  @Override
  public String describe()
  {
    return "Delete image(s)";
  }
}
