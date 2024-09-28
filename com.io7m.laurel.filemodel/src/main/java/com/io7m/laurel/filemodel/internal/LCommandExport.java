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

import com.io7m.laurel.filemodel.LExportRequest;
import com.io7m.laurel.filemodel.LFileModelEvent;
import com.io7m.laurel.filemodel.LFileModelEventError;
import com.io7m.laurel.filemodel.LFileModelEventType;
import com.io7m.laurel.model.LException;
import com.io7m.laurel.model.LGlobalCaption;
import com.io7m.laurel.model.LImageID;
import com.io7m.laurel.model.LImageWithID;
import com.io7m.mime2045.core.MimeType;
import com.io7m.mime2045.fileext.MimeFileExtensions;
import org.jooq.DSLContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.io7m.laurel.filemodel.internal.Tables.IMAGES;
import static com.io7m.laurel.filemodel.internal.Tables.IMAGE_BLOBS;

/**
 * Export.
 */

public final class LCommandExport
  extends LCommandAbstract<LExportRequest>
{
  private final HashMap<String, Object> attributes;
  private int imageCount;
  private int imageIndex;
  private final ArrayList<LFileModelEventType> events;

  /**
   * Export.
   */

  public LCommandExport()
  {
    this.events = new ArrayList<>();
    this.attributes = new HashMap<String, Object>();
  }

  /**
   * Validate.
   *
   * @return A command factory
   */

  public static LCommandFactoryType<LExportRequest> provider()
  {
    return new LCommandFactory<>(
      LCommandExport.class.getCanonicalName(),
      LCommandExport::fromProperties
    );
  }

  private static LCommandExport fromProperties(
    final Properties p)
  {
    final var c = new LCommandExport();
    c.setExecuted(true);
    return c;
  }

  @Override
  protected LCommandUndoable onExecute(
    final LFileModel model,
    final LDatabaseTransactionType transaction,
    final LExportRequest request)
    throws LException
  {
    final var context =
      transaction.get(DSLContext.class);

    try {
      this.createOutputDirectory(request.outputDirectory());
    } catch (final LException e) {
      throw this.handleException(e);
    }

    try {
      final var images = model.imageList().get();
      this.imageCount = images.size();
      this.imageIndex = 0;

      for (final var image : images) {
        this.exportImage(
          model,
          context,
          model.globalCaptionList().get(),
          image,
          request
        );
        ++this.imageIndex;
      }

      this.event(model, 1.0, "Exported dataset.");
      return LCommandUndoable.COMMAND_NOT_UNDOABLE;
    } catch (final Throwable e) {
      throw this.handleException(e);
    }
  }

  private LException mapException(
    final Throwable e)
  {
    if (e instanceof final LException es) {
      return es;
    }

    return new LException(
      Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
      e,
      "error-exception",
      this.takeAttributes(),
      Optional.empty()
    );
  }

  private Map<String, String> takeAttributes()
  {
    final var attributeMap = this.attributesCopy();
    this.attributes.clear();
    return attributeMap;
  }


  private LException handleException(
    final Throwable e)
  {
    final var x = this.mapException(e);
    this.events.add(
      new LFileModelEventError(
        x.getMessage(),
        OptionalDouble.of(0.0),
        x.errorCode().toString(),
        x.attributes(),
        x.remediatingAction(),
        Optional.of(x)
      )
    );
    return x;
  }

  private Map<String, String> attributesCopy()
  {
    return this.attributes.entrySet()
      .stream()
      .map(x -> Map.entry(x.getKey(), x.getValue().toString()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private void event(
    final LFileModel model,
    final double progress,
    final String text,
    final Object... arguments)
  {
    this.events.add(
      new LFileModelEvent(
        text.formatted(arguments),
        OptionalDouble.of(progress)
      )
    );

    model.setExportEvents(List.copyOf(this.events));
  }

  private void eventWithProgress(
    final LFileModel model,
    final String text,
    final Object... arguments)
  {
    this.events.add(
      new LFileModelEvent(
        text.formatted(arguments),
        OptionalDouble.of((double) this.imageIndex / (double) this.imageCount)
      )
    );

    model.setExportEvents(List.copyOf(this.events));
  }

  private void exportImage(
    final LFileModel model,
    final DSLContext context,
    final List<LGlobalCaption> globalCaptions,
    final LImageWithID image,
    final LExportRequest request)
    throws LException
  {
    final var idString =
      Long.toUnsignedString(image.id().value());
    final var idStringZeroed =
      "0".repeat(20 - idString.length());
    final var imageNumber =
      "%s%s".formatted(idStringZeroed, idString);
    final var imageExt =
      imageExtensionFor(image.image().type());
    final var imageName =
      "%s.%s".formatted(imageNumber, imageExt);
    final var captionName =
      "%s.caption".formatted(imageNumber);

    if (request.exportImages()) {
      this.writeImage(
        context,
        model,
        image,
        request.outputDirectory().resolve(imageName)
      );
    }

    this.writeCaptions(
      model,
      context,
      globalCaptions,
      image,
      request.outputDirectory().resolve(captionName)
    );
  }

  private void writeCaptions(
    final LFileModel model,
    final DSLContext context,
    final List<LGlobalCaption> globalCaptions,
    final LImageWithID image,
    final Path file)
    throws LException
  {
    this.eventWithProgress(model, "Writing caption file '%s'", file);
    this.attributes.put("Image", image.id());

    try {
      LCaptionFiles.serialize(
        this.attributes,
        globalCaptions,
        LCommandModelUpdates.listImageCaptionsAssigned(context, image.id()),
        file
      );
    } catch (final Exception e) {
      throw this.handleException(e);
    }
  }

  private void writeImage(
    final DSLContext context,
    final LFileModel model,
    final LImageWithID image,
    final Path file)
    throws LException
  {
    this.eventWithProgress(model, "Writing image file '%s'", file);
    this.attributes.put("Image", image.id());
    this.attributes.put("File", file);

    try {
      Files.write(file, imageData(context, image.id()));
    } catch (final Exception e) {
      throw this.handleException(e);
    }
  }

  private static byte[] imageData(
    final DSLContext context,
    final LImageID id)
  {
    return context.select(IMAGE_BLOBS.IMAGE_BLOB_DATA)
      .from(IMAGE_BLOBS)
      .join(IMAGES)
      .on(IMAGES.IMAGE_BLOB.eq(IMAGE_BLOBS.IMAGE_BLOB_ID))
      .where(IMAGES.IMAGE_ID.eq(id.value()))
      .fetchOne(IMAGE_BLOBS.IMAGE_BLOB_DATA);
  }

  private static String imageExtensionFor(
    final MimeType type)
  {
    return MimeFileExtensions.suggestFileExtension(type)
      .orElse("bin");
  }

  private void createOutputDirectory(
    final Path outputDirectory)
    throws LException
  {
    try {
      this.attributes.put("Output Directory", outputDirectory);
      Files.createDirectories(outputDirectory);
    } catch (final IOException e) {
      throw new LException(
        e.getMessage(),
        e,
        "error-create-directory",
        this.takeAttributes(),
        Optional.empty()
      );
    }
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
    return "Export";
  }
}
