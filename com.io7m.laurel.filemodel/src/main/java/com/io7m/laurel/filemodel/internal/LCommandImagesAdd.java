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
import com.io7m.laurel.model.LHashSHA256;
import org.jooq.DSLContext;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.IMAGES;
import static com.io7m.laurel.filemodel.internal.Tables.IMAGE_BLOBS;

/**
 * Add images.
 */

public final class LCommandImagesAdd
  extends LCommandAbstract<List<LImageRequest>>
{
  private final ArrayList<SavedData> savedData;

  private record SavedData(
    long savedBlobId,
    long savedImageId,
    String savedSourceText,
    String savedFile,
    String savedName)
  {

  }

  /**
   * Add images.
   */

  public LCommandImagesAdd()
  {
    this.savedData = new ArrayList<>();
  }

  /**
   * @return A command factory
   */

  public static LCommandFactoryType<List<LImageRequest>> provider()
  {
    return new LCommandFactory<>(
      LCommandImagesAdd.class.getCanonicalName(),
      LCommandImagesAdd::fromProperties
    );
  }

  private static LCommandImagesAdd fromProperties(
    final Properties p)
  {
    final var c = new LCommandImagesAdd();

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

      if (!p.containsKey(idKey)) {
        break;
      }

      final var data =
        new SavedData(
          Long.parseUnsignedLong(p.getProperty(blobKey)),
          Long.parseUnsignedLong(p.getProperty(idKey)),
          p.getProperty(sourceKey),
          p.getProperty(fileKey),
          p.getProperty(nameKey)
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
    final List<LImageRequest> requests)
    throws LException
  {
    final var context =
      transaction.get(DSLContext.class);

    final var max = requests.size();
    for (int index = 0; index < max; ++index) {
      final var request = requests.get(index);
      final var file = request.file();
      model.setAttribute("ImageFile", file);
      model.eventWithProgressCurrentMax(index, max, "Adding image '%s'.", file);

      final var imageBytes =
        loadImage(model, request);
      final var imageHash =
        hashOf(imageBytes);

      final var blobRec =
        context.insertInto(IMAGE_BLOBS)
          .set(IMAGE_BLOBS.IMAGE_BLOB_SHA256, imageHash.value())
          .set(IMAGE_BLOBS.IMAGE_BLOB_DATA, imageBytes)
          .returning(IMAGE_BLOBS.IMAGE_BLOB_ID)
          .fetchOne();

      final var savedName =
        request.name();
      final var savedBlobId =
        blobRec.get(IMAGE_BLOBS.IMAGE_BLOB_ID);
      final var savedFile =
        file.toString();
      final var savedSourceText =
        request.source().map(URI::toString).orElse(null);

      final var savedImageId =
        context.insertInto(IMAGES)
          .set(IMAGES.IMAGE_BLOB, savedBlobId)
          .set(IMAGES.IMAGE_FILE, savedFile)
          .set(IMAGES.IMAGE_SOURCE, savedSourceText)
          .set(IMAGES.IMAGE_NAME, savedName)
          .returning(IMAGES.IMAGE_ID)
          .fetchOne()
          .get(IMAGES.IMAGE_ID);

      this.savedData.add(
        new SavedData(
          savedBlobId,
          savedImageId,
          savedSourceText,
          savedFile,
          savedName
        )
      );
    }

    model.setImagesAll(LCommandModelUpdates.listImages(context));
    model.eventWithoutProgress("Added %d images.", max);
    return LCommandUndoable.COMMAND_UNDOABLE;
  }



  private static LHashSHA256 hashOf(
    final byte[] imageBytes)
  {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }

    digest.update(imageBytes);
    return new LHashSHA256(HexFormat.of().formatHex(digest.digest()));
  }

  private static byte[] loadImage(
    final LFileModel model,
    final LImageRequest request)
    throws LException
  {
    try {
      final var imageBytes =
        Files.readAllBytes(request.file());

      try (var imageStream = new ByteArrayInputStream(imageBytes)) {
        final var image = ImageIO.read(imageStream);
        if (image == null) {
          throw new LException(
            "Failed to load image.",
            "error-image-format",
            model.attributes(),
            Optional.empty()
          );
        }
      }

      return imageBytes;
    } catch (final IOException e) {
      throw new LException(
        "Failed to open image file.",
        e,
        "error-io",
        model.attributes(),
        Optional.empty()
      );
    }
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
        "Deleting image '%s'.",
        data.savedFile
      );
      context.deleteFrom(IMAGES)
        .where(IMAGES.IMAGE_ID.eq(data.savedImageId))
        .execute();
    }

    model.setImagesAll(LCommandModelUpdates.listImages(context));
    model.eventWithoutProgress("Deleted %d images.", this.savedData.size());
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
        "Adding image '%s'.",
        data.savedFile
      );
      context.insertInto(IMAGES)
        .set(IMAGES.IMAGE_ID, data.savedImageId)
        .set(IMAGES.IMAGE_BLOB, data.savedBlobId)
        .set(IMAGES.IMAGE_FILE, data.savedFile)
        .set(IMAGES.IMAGE_SOURCE, data.savedSourceText)
        .set(IMAGES.IMAGE_NAME, data.savedName)
        .execute();
    }

    model.setImagesAll(LCommandModelUpdates.listImages(context));
    model.eventWithoutProgress("Added %d images.", this.savedData.size());
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

      final var data = this.savedData.get(index);
      p.setProperty(idKey, Long.toUnsignedString(data.savedImageId));
      p.setProperty(blobKey, Long.toUnsignedString(data.savedBlobId));
      p.setProperty(fileKey, data.savedFile);
      p.setProperty(sourceKey, data.savedSourceText);
      p.setProperty(nameKey, data.savedName);
    }

    return p;
  }

  @Override
  public String describe()
  {
    return "Add image(s)";
  }
}
