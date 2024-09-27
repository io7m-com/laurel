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

import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import com.io7m.laurel.filemodel.LFileModelEvent;
import com.io7m.laurel.filemodel.LFileModelEventError;
import com.io7m.laurel.filemodel.LFileModelEventType;
import com.io7m.laurel.filemodel.LFileModelImportType;
import com.io7m.laurel.filemodel.LFileModelType;
import com.io7m.laurel.filemodel.LImageCaptionsAssignment;
import com.io7m.laurel.model.LCaptionID;
import com.io7m.laurel.model.LCaptionName;
import com.io7m.laurel.model.LException;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * A file import operation.
 */

public final class LFileModelImport implements LFileModelImportType
{
  private final Path directory;
  private final Path outputFile;
  private final SubmissionPublisher<LFileModelEventType> events;
  private final HashMap<String, Object> attributes;
  private final ArrayList<Path> imageFiles;
  private final Tika tika;
  private final AtomicBoolean failed;
  private final HashMap<Path, List<LCaptionName>> captions;
  private final CloseableCollectionType<ClosingResourceFailedException> resources;
  private final ReentrantLock runningLock;
  private LFileModelType model;

  LFileModelImport(
    final Path inDirectory,
    final Path inOutputFile)
  {
    this.directory =
      Objects.requireNonNull(inDirectory, "directory");
    this.outputFile =
      Objects.requireNonNull(inOutputFile, "outputFile");
    this.resources =
      CloseableCollection.create();
    this.events =
      this.resources.add(
        new SubmissionPublisher<>(
          Runnable::run,
          1
        ));
    this.attributes =
      new HashMap<>();
    this.imageFiles =
      new ArrayList<>();
    this.captions =
      new HashMap<>();
    this.tika =
      new Tika();
    this.failed =
      new AtomicBoolean(false);
    this.runningLock =
      new ReentrantLock();
  }

  /**
   * Create an import operation.
   *
   * @param directory  The source directory
   * @param outputFile The output file
   *
   * @return The operation
   */

  public static LFileModelImportType create(
    final Path directory,
    final Path outputFile)
  {
    return new LFileModelImport(directory, outputFile);
  }

  @Override
  public Flow.Publisher<LFileModelEventType> events()
  {
    return this.events;
  }

  @Override
  public CompletableFuture<Void> execute()
  {
    final var future = new CompletableFuture<Void>();
    Thread.ofVirtual()
      .start(() -> {
        try {
          this.executeActual();
          future.complete(null);
        } catch (final Throwable e) {
          future.completeExceptionally(e);
        }
      });
    return future;
  }

  private void executeActual()
    throws LException
  {
    this.runningLock.lock();

    try {
      this.executeLocked();
    } finally {
      this.runningLock.unlock();
    }
  }

  private void executeLocked()
    throws LException
  {
    try {
      this.createModel();
      this.listFiles();
      this.openCaptions();
      this.saveCaptions();
      this.importImages();
      this.assignCaptions();
      this.finish();
    } catch (final Throwable e) {
      this.failed.set(true);
      throw e;
    } finally {
      if (this.failed.get()) {
        this.events.submit(
          new LFileModelEvent(
            "Import failed. Partial results may be present in the output file.",
            OptionalDouble.of(1.0)
          )
        );
      } else {
        this.events.submit(
          new LFileModelEvent(
            "Import completed.",
            OptionalDouble.of(1.0)
          )
        );
      }
    }
  }

  private void finish()
  {
    this.event("Compacting database…");

    try {
      this.model.compact().get(1L, TimeUnit.MINUTES);
    } catch (final Throwable e) {
      this.failed.set(true);
      this.handleException(e);
    }
  }

  private void saveCaptions()
    throws LException
  {
    this.event("Saving captions…");

    final var captionNames =
      List.copyOf(
        this.captions.values()
          .stream()
          .flatMap(Collection::stream)
          .collect(Collectors.toSet())
      );

    final var max = captionNames.size();
    for (int index = 0; index < max; ++index) {
      try {
        final var caption = captionNames.get(index);
        this.eventProgress(
          index,
          max,
          "Creating caption '%s'".formatted(caption));
        this.model.captionAdd(caption).get(1L, TimeUnit.MINUTES);
      } catch (final Throwable e) {
        this.failed.set(true);
        this.handleException(e);
      }
    }

    if (this.failed.get()) {
      throw this.errorImport();
    }
  }

  private void assignCaptions()
    throws LException
  {
    this.event("Assigning captions…");

    final var images =
      this.model.imageList().get();
    final var captionIds =
      new HashMap<LCaptionName, LCaptionID>();
    final var assignments =
      new ArrayList<LImageCaptionsAssignment>(images.size());

    for (final var caption : this.model.captionList().get()) {
      captionIds.put(caption.name(), caption.id());
    }

    for (final var image : images) {
      final var captionIdList =
        this.captions.get(image.image().file().orElseThrow())
          .stream()
          .map(captionIds::get)
          .collect(Collectors.toSet());

      assignments.add(new LImageCaptionsAssignment(image.id(), captionIdList));
    }

    this.model.imageCaptionsAssign(assignments);

    if (this.failed.get()) {
      throw this.errorImport();
    }
  }

  private void importImages()
    throws LException
  {
    this.event("Importing images…");

    final var max = this.imageFiles.size();
    for (int index = 0; index < max; ++index) {
      try {
        final var imageFile =
          this.imageFiles.get(index);

        this.attributes.put("File", imageFile);
        this.eventProgress(index, max, "Loading image file %s.", imageFile);

        this.model.imageAdd(
          imageFile.toString(),
          imageFile,
          Optional.of(imageFile.toUri())
        ).get(1L, TimeUnit.MINUTES);
      } catch (final Throwable e) {
        this.failed.set(true);
        this.handleException(e);
      }
    }

    if (this.failed.get()) {
      throw this.errorImport();
    }
  }

  private void createModel()
    throws LException
  {
    this.model =
      this.resources.add(LFileModel.open(this.outputFile, false));
  }

  private void openCaptions()
    throws LException
  {
    this.event("Loading captions…");

    final var max = this.imageFiles.size();
    for (int index = 0; index < max; ++index) {
      try {
        final var imageFile =
          this.imageFiles.get(index);
        final var imageName =
          imageFile.getFileName();
        final var capName =
          FilenameUtils.removeExtension(imageName.toString()) + ".caption";
        final var capFile =
          imageFile.resolveSibling(capName);

        this.attributes.put("File", capFile);
        this.eventProgress(index, max, "Loading caption file %s.", capFile);

        final List<LCaptionName> captionList =
          this.parseCaptions(Files.readString(capFile));
        this.captions.put(imageFile, captionList);
      } catch (final Throwable e) {
        this.failed.set(true);
        this.handleException(e);
      }
    }

    if (this.failed.get()) {
      throw this.errorImport();
    }
  }

  private LException errorImport()
  {
    return new LException(
      "Import failed.",
      "error-import",
      this.attributesCopy(),
      Optional.empty()
    );
  }

  private List<LCaptionName> parseCaptions(
    final String rawText)
  {
    final var results =
      new ArrayList<LCaptionName>();

    final var segments =
      List.of(rawText.split(","));

    for (final var text : segments) {
      final var trimmed = text.trim();
      if (trimmed.isBlank()) {
        continue;
      }
      this.attributes.put("Caption", trimmed);
      try {
        results.add(new LCaptionName(trimmed));
      } catch (final Throwable e) {
        this.failed.set(true);
        this.handleException(e);
      }
    }

    return List.copyOf(results);
  }

  private void listFiles()
    throws LException
  {
    this.imageFiles.clear();
    this.attributes.put("Base Directory", this.directory);

    this.event("Listing files…");
    try (var stream = Files.walk(this.directory)) {
      this.imageFiles.addAll(
        stream.filter(this::isImageFile)
          .toList()
      );

      this.event(
        "Found %d probable image files.",
        this.imageFiles.size()
      );
    } catch (final Exception e) {
      throw this.handleException(e);
    }
  }

  private void event(
    final String message,
    final Object... arguments)
  {
    this.events.submit(
      new LFileModelEvent(
        message.formatted(arguments),
        OptionalDouble.empty()
      )
    );
  }

  private void eventProgress(
    final int current,
    final int max,
    final String message,
    final Object... arguments)
  {
    final var progress =
      OptionalDouble.of((double) current / (double) max);

    this.events.submit(
      new LFileModelEvent(
        message.formatted(arguments),
        progress
      )
    );
  }

  private boolean isImageFile(
    final Path file)
  {
    try {
      if (Files.isRegularFile(file)) {
        this.attributes.put("File", file);
        final var type = this.tika.detect(file);
        this.attributes.put("Type", type);
        final var isImage = type.startsWith("image/");
        if (isImage) {
          this.event(
            "File '%s' has type '%s' and is therefore probably an image.",
            file,
            type
          );
        } else {
          this.event(
            "File '%s' has type '%s' and is therefore probably not an image.",
            file,
            type
          );
        }
        return isImage;
      }
      return false;
    } catch (final Throwable e) {
      this.handleException(e);
      return false;
    }
  }

  private LException handleException(
    final Throwable e)
  {
    final var message =
      Objects.requireNonNullElse(e.getMessage(), e.getClass().getName());
    final var attributeMap =
      this.attributesCopy();

    this.attributes.clear();

    final var error =
      new LFileModelEventError(
        message,
        OptionalDouble.empty(),
        "error-exception",
        attributeMap,
        Optional.empty(),
        Optional.of(e)
      );

    this.events.submit(error);
    return new LException(
      message,
      e,
      "error-exception",
      attributeMap,
      Optional.empty()
    );
  }

  private Map<String, String> attributesCopy()
  {
    return this.attributes.entrySet()
      .stream()
      .map(x -> Map.entry(x.getKey(), x.getValue().toString()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public void close()
  {
    try {
      this.resources.close();
    } catch (final ClosingResourceFailedException e) {
      throw new IllegalStateException(e);
    }
  }
}
