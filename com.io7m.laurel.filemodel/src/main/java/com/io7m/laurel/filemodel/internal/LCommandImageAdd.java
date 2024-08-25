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
import com.io7m.laurel.model.LImage;
import org.jooq.DSLContext;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static com.io7m.laurel.filemodel.internal.Tables.IMAGES;
import static com.io7m.laurel.filemodel.internal.Tables.IMAGE_BLOBS;

/**
 * Add an image.
 */

public final class LCommandImageAdd
  extends LCommandAbstract<LImageRequest>
{
  private Long savedBlobId;
  private Long savedImageId;
  private String savedSourceText;
  private String savedFile;
  private String savedName;

  /**
   * Add an image.
   */

  public LCommandImageAdd()
  {

  }

  /**
   * @return A command factory
   */

  public static LCommandFactoryType<LImageRequest> provider()
  {
    return new LCommandFactory<>(
      LCommandImageAdd.class.getCanonicalName(),
      LCommandImageAdd::fromProperties
    );
  }

  private static LCommandImageAdd fromProperties(
    final Properties p)
  {
    final var c = new LCommandImageAdd();
    c.savedBlobId = Long.parseUnsignedLong(p.getProperty("blob"));
    c.savedImageId = Long.parseUnsignedLong(p.getProperty("id"));
    c.savedSourceText = p.getProperty("source");
    c.savedFile = p.getProperty("file");
    c.savedName = p.getProperty("name");
    c.setExecuted(true);
    return c;
  }

  @Override
  protected LCommandUndoable onExecute(
    final LFileModel model,
    final LDatabaseTransactionType transaction,
    final LImageRequest request)
    throws LException
  {
    final var context =
      transaction.get(DSLContext.class);

    final var file = request.file().toAbsolutePath();
    model.setAttribute("ImageFile", file);

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

    this.savedName =
      request.name();
    this.savedBlobId =
      blobRec.get(IMAGE_BLOBS.IMAGE_BLOB_ID);
    this.savedFile =
      file.toString();
    this.savedSourceText =
      request.source().map(URI::toString).orElse(null);

    this.savedImageId =
      context.insertInto(IMAGES)
        .set(IMAGES.IMAGE_BLOB, this.savedBlobId)
        .set(IMAGES.IMAGE_FILE, this.savedFile)
        .set(IMAGES.IMAGE_SOURCE, this.savedSourceText)
        .set(IMAGES.IMAGE_NAME, this.savedName)
        .returning(IMAGES.IMAGE_ID)
        .fetchOne()
        .get(IMAGES.IMAGE_ID);

    model.setImagesAll(listImages(transaction));
    return LCommandUndoable.COMMAND_UNDOABLE;
  }

  private static List<LImage> listImages(
    final LDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    return context.select(
        IMAGES.IMAGE_SOURCE,
        IMAGES.IMAGE_NAME,
        IMAGES.IMAGE_FILE,
        IMAGE_BLOBS.IMAGE_BLOB_SHA256
      )
      .from(IMAGES)
      .join(IMAGE_BLOBS)
      .on(IMAGE_BLOBS.IMAGE_BLOB_ID.eq(IMAGES.IMAGE_BLOB))
      .orderBy(IMAGES.IMAGE_NAME)
      .stream()
      .map(LCommandImageAdd::mapRecord)
      .toList();
  }

  private static LImage mapRecord(
    final org.jooq.Record r)
  {
    return new LImage(
      r.get(IMAGES.IMAGE_NAME),
      Optional.ofNullable(r.get(IMAGES.IMAGE_FILE)).map(Paths::get),
      Optional.ofNullable(r.get(IMAGES.IMAGE_SOURCE)).map(URI::create),
      new LHashSHA256(r.get(IMAGE_BLOBS.IMAGE_BLOB_SHA256))
    );
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

    context.deleteFrom(IMAGES)
      .where(IMAGES.IMAGE_ID.eq(this.savedImageId))
      .execute();

    model.setImagesAll(listImages(transaction));
  }

  @Override
  protected void onRedo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    context.insertInto(IMAGES)
      .set(IMAGES.IMAGE_ID, this.savedImageId)
      .set(IMAGES.IMAGE_BLOB, this.savedBlobId)
      .set(IMAGES.IMAGE_FILE, this.savedFile)
      .set(IMAGES.IMAGE_SOURCE, this.savedSourceText)
      .set(IMAGES.IMAGE_NAME, this.savedName)
      .execute();

    model.setImagesAll(listImages(transaction));
  }

  @Override
  public Properties toProperties()
  {
    final var p = new Properties();
    p.setProperty("id", Long.toUnsignedString(this.savedImageId));
    p.setProperty("blob", Long.toUnsignedString(this.savedBlobId));
    p.setProperty("file", this.savedFile);
    p.setProperty("source", this.savedSourceText);
    p.setProperty("name", this.savedName);
    return p;
  }

  @Override
  public String describe()
  {
    return "Add image '%s'".formatted(this.savedName);
  }
}
