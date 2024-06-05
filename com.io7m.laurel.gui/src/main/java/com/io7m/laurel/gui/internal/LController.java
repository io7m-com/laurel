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


package com.io7m.laurel.gui.internal;

import com.io7m.anethum.api.ParseStatusType;
import com.io7m.anethum.api.ParsingException;
import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.laurel.gui.internal.model.LMCaption;
import com.io7m.laurel.gui.internal.model.LMImage;
import com.io7m.laurel.gui.internal.model.LMImageCreate;
import com.io7m.laurel.gui.internal.model.LMUndoState;
import com.io7m.laurel.gui.internal.model.LModel;
import com.io7m.laurel.gui.internal.model.LModelFileStatusType;
import com.io7m.laurel.gui.internal.model.LModelOpCaptionCreate;
import com.io7m.laurel.gui.internal.model.LModelOpCaptionDelete;
import com.io7m.laurel.gui.internal.model.LModelOpCaptionsAssign;
import com.io7m.laurel.gui.internal.model.LModelOpCaptionsUnassign;
import com.io7m.laurel.gui.internal.model.LModelOpException;
import com.io7m.laurel.gui.internal.model.LModelOpGlobalPrefixCaptionCreate;
import com.io7m.laurel.gui.internal.model.LModelOpGlobalPrefixCaptionDelete;
import com.io7m.laurel.gui.internal.model.LModelOpGlobalPrefixCaptionModify;
import com.io7m.laurel.gui.internal.model.LModelOpImagesCreate;
import com.io7m.laurel.gui.internal.model.LModelOpImagesDelete;
import com.io7m.laurel.gui.internal.model.LModelOpType;
import com.io7m.laurel.gui.internal.model.LModelType;
import com.io7m.laurel.io.LExportRequest;
import com.io7m.laurel.io.LExporters;
import com.io7m.laurel.io.LImageSets;
import com.io7m.laurel.io.LImporters;
import com.io7m.laurel.io.LParsers;
import com.io7m.laurel.io.LSerializers;
import com.io7m.laurel.model.LImageCaptionID;
import com.io7m.laurel.model.LImageID;
import com.io7m.laurel.model.LImageSet;
import com.io7m.seltzer.api.SStructuredError;
import com.io7m.seltzer.api.SStructuredErrorType;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.Collectors;

/**
 * The controller.
 */

public final class LController implements LControllerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LController.class);

  private static final LSerializers SERIALIZERS =
    new LSerializers();
  private static final LParsers PARSERS =
    new LParsers();

  private final SimpleBooleanProperty busy;
  private final SubmissionPublisher<SStructuredErrorType<String>> errors;
  private final ConcurrentHashMap<String, String> attributes;
  private final LModel model;
  private final SimpleObjectProperty<LMUndoState> undoState;
  private final ObservableList<LMCaption> captionsCopied;

  /**
   * Construct a controller.
   */

  public LController()
  {
    this.model =
      new LModel();
    this.busy =
      new SimpleBooleanProperty(false);

    this.errors =
      new SubmissionPublisher<>();
    this.attributes =
      new ConcurrentHashMap<>();
    this.undoState =
      new SimpleObjectProperty<>(LMUndoState.empty());
    this.captionsCopied =
      FXCollections.observableArrayList();
  }

  @Override
  public ReadOnlyProperty<LMUndoState> undoState()
  {
    return this.undoState;
  }

  @Override
  public LModelType model()
  {
    return this.model;
  }

  @Override
  public Flow.Publisher<SStructuredErrorType<String>> errors()
  {
    return this.errors;
  }

  @Override
  public ReadOnlyProperty<Boolean> busy()
  {
    return this.busy;
  }

  @Override
  public String description()
  {
    return "Main controller.";
  }

  @Override
  public CompletableFuture<Object> save(
    final Path path)
  {
    Objects.requireNonNull(path, "path");

    this.attributes.clear();
    this.attributes.put("File", path.toString());
    this.busySet(true);

    final var tmpFile =
      path.resolveSibling(path.getFileName() + ".tmp");
    final var imageSet =
      this.model.createImageSet();

    final var future = new CompletableFuture<>();
    future.whenComplete((o, throwable) -> {
      Platform.runLater(() -> this.busySet(false));
    });

    Thread.ofVirtual()
      .start(() -> {
        try {
          SERIALIZERS.serializeFile(tmpFile, imageSet);

          Files.move(
            tmpFile,
            path,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING
          );

          LOG.info("Saved: {} -> {}", tmpFile, path);
          Platform.runLater(() -> {
            this.model.fileStatus().set(new LModelFileStatusType.Saved(path));
            future.complete(null);
          });
        } catch (final Throwable e) {
          LOG.error("", e);
          this.publishError(e);
          future.completeExceptionally(e);
        }
      });

    return future;
  }

  private void busySet(
    final boolean newValue)
  {
    this.busy.set(newValue);
  }

  private void publishError(
    final Throwable e)
  {
    if (e instanceof final SStructuredErrorType<?> c) {
      this.attributes.putAll(c.attributes());
      this.errors.submit(new SStructuredError<>(
        c.errorCode().toString(),
        c.message(),
        Map.copyOf(this.attributes),
        c.remediatingAction(),
        Optional.of(e)
      ));
    } else {
      this.errors.submit(new SStructuredError<>(
        "Error",
        Objects.requireNonNullElse(
          e.getMessage(),
          e.getClass().getCanonicalName()),
        Map.copyOf(this.attributes),
        Optional.empty(),
        Optional.of(e)
      ));
    }
  }

  @Override
  public String toString()
  {
    return String.format(
      "[LController 0x%08x]",
      Integer.valueOf(this.hashCode())
    );
  }

  @Override
  public CompletableFuture<?> open(
    final Path path)
  {
    Objects.requireNonNull(path, "path");

    this.attributes.clear();
    this.attributes.put("File", path.toString());
    this.busySet(true);

    final var future = new CompletableFuture<>();
    future.whenComplete((ignored0, ignored1) -> {
      LOG.debug("Open completed.");
      Platform.runLater(() -> this.busySet(false));
    });

    Thread.ofVirtual()
      .start(() -> {
        try {
          LOG.info("Open: {}", path);
          final var newImageSet = PARSERS.parseFile(path);
          Platform.runLater(() -> {
            try {
              this.model.replaceWith(path, newImageSet);
              future.complete(null);
            } catch (final Exception e) {
              LOG.error("", e);
              this.publishError(e);
              future.completeExceptionally(e);
            }
          });
        } catch (final ParsingException e) {
          LOG.error("", e);
          this.publishParseErrors(e.statusValues());
          this.publishError(e);
          future.completeExceptionally(e);
        } catch (final Throwable e) {
          LOG.error("", e);
          this.publishError(e);
          future.completeExceptionally(e);
        }
      });

    return future;
  }

  @Override
  public CompletableFuture<Object> importDirectory(
    final Path path)
  {
    Objects.requireNonNull(path, "path");

    this.attributes.clear();
    this.attributes.put("Directory", path.toString());
    this.busySet(true);

    final var future = new CompletableFuture<>();
    future.whenComplete((ignored0, ignored1) -> {
      LOG.debug("Import completed.");
      Platform.runLater(() -> this.busySet(false));
    });

    Thread.ofVirtual()
      .start(() -> {
        try {
          LOG.info("Import: {}", path);

          final var newImageSet =
            LImporters.importDirectory(path, error -> {
              this.attributes.putAll(error.attributes());
            });

          Platform.runLater(() -> {
            try {
              this.model.replaceWith(
                path.resolve("Captions.xml"),
                newImageSet
              );
              future.complete(null);
            } catch (final Exception e) {
              LOG.error("", e);
              this.publishError(e);
              future.completeExceptionally(e);
            }
          });
        } catch (final Throwable e) {
          LOG.error("", e);
          this.publishError(e);
          future.completeExceptionally(e);
        }
      });

    return future;
  }

  private void publishParseErrors(
    final List<ParseStatusType> statusValues)
  {
    for (final var status : statusValues) {
      this.attributes.put(
        "ParseError[%d:%d]".formatted(
          Integer.valueOf(status.lexical().line()),
          Integer.valueOf(status.lexical().column())
        ),
        "%s: %s".formatted(status.errorCode(), status.message())
      );
    }
  }

  @Override
  public void close()
  {
    this.errors.close();
  }

  @Override
  public void newSet(
    final Path file)
  {
    this.attributes.clear();
    this.attributes.put("File", file.toString());

    try {
      this.busySet(true);
      this.model.clear();
      this.model.fileStatus().set(new LModelFileStatusType.Unsaved(file));
    } catch (final Throwable e) {
      LOG.error("", e);
      this.publishError(e);
    } finally {
      this.busySet(false);
    }
  }

  @Override
  public Optional<Path> imageSelect(
    final Optional<LImageID> imageOpt)
  {
    return this.model.imageSelect(imageOpt);
  }

  @Override
  public void undo()
  {
    final var undoThen =
      this.undoState.getValue();
    final var undoStackNew =
      new LinkedList<>(undoThen.undoStack());
    final var redoStackNew =
      new LinkedList<>(undoThen.redoStack());

    final var command = undoStackNew.removeFirst();
    if (command != null) {
      LOG.debug("Undo: {}", command.description());
      try {
        command.undo();
      } catch (final Exception e) {
        this.publishError(e);
        return;
      }
      redoStackNew.addFirst(command);
      this.undoState.set(new LMUndoState(undoStackNew, redoStackNew));
    }
  }

  @Override
  public void redo()
  {
    final var undoThen =
      this.undoState.getValue();
    final var undoStackNew =
      new LinkedList<>(undoThen.undoStack());
    final var redoStackNew =
      new LinkedList<>(undoThen.redoStack());

    final var command = redoStackNew.removeFirst();
    if (command != null) {
      LOG.debug("Redo: {}", command.description());
      try {
        command.execute();
      } catch (final Exception e) {
        this.publishError(e);
        return;
      }
      undoStackNew.addFirst(command);
      this.undoState.set(new LMUndoState(undoStackNew, redoStackNew));
    }
  }

  @Override
  public CompletableFuture<Object> imagesAdd(
    final List<Path> files)
  {
    Objects.requireNonNull(files, "files");

    this.attributes.clear();
    this.busySet(true);

    final var future = new CompletableFuture<>();
    future.whenComplete((ignored0, ignored1) -> {
      Platform.runLater(() -> this.busySet(false));
    });

    Thread.ofVirtual()
      .start(() -> {
        try {
          LOG.info("Add Files: {}", files);

          var index = 0;
          final var imageCreates = new ArrayList<LMImageCreate>();
          for (final var file : files) {
            this.attributes.put(
              "File[%d]".formatted(Integer.valueOf(index)), file.toString());

            final var bufferedImage = ImageIO.read(file.toFile());
            if (bufferedImage == null) {
              throw new IOException(
                "Unable to open image: %s".formatted(file)
              );
            }

            final var imageID = new LImageID(UUID.randomUUID());
            imageCreates.add(
              new LMImageCreate(imageID, file.toAbsolutePath().normalize())
            );
            ++index;
          }

          Platform.runLater(() -> {
            try {
              this.executeSaveStateChangingCommand(
                this.fileOrThrow(),
                new LModelOpImagesCreate(this.model, imageCreates)
              );
              future.complete(null);
            } catch (final Exception e) {
              LOG.error("", e);
              this.publishError(e);
              future.completeExceptionally(e);
            }
          });
        } catch (final Throwable e) {
          LOG.error("", e);
          this.publishError(e);
          future.completeExceptionally(e);
        }
      });

    return future;
  }

  @Override
  public CompletableFuture<Object> save()
  {
    return this.save(this.fileOrThrow());
  }

  private Path fileOrThrow()
  {
    return switch (this.model.fileStatus().get()) {
      case final LModelFileStatusType.None none -> {
        throw new IllegalStateException();
      }
      case final LModelFileStatusType.Saved saved -> {
        yield saved.file();
      }
      case final LModelFileStatusType.Unsaved unsaved -> {
        yield unsaved.file();
      }
    };
  }

  @Override
  public void captionNew(
    final String text)
  {
    this.executeSaveStateChangingCommand(
      this.fileOrThrow(),
      new LModelOpCaptionCreate(
        this.model,
        new LImageCaptionID(UUID.randomUUID()),
        text.trim()
      )
    );
  }

  @Override
  public void imageCaptionUnassign(
    final List<LImageID> images,
    final List<LImageCaptionID> captions)
  {
    Objects.requireNonNull(images, "images");
    Objects.requireNonNull(captions, "captions");

    this.executeSaveStateChangingCommand(
      this.fileOrThrow(),
      new LModelOpCaptionsUnassign(this.model, images, captions)
    );
  }

  @Override
  public void imageCaptionAssign(
    final List<LImageID> images,
    final List<LImageCaptionID> captions)
  {
    Objects.requireNonNull(images, "images");
    Objects.requireNonNull(captions, "captions");

    this.executeSaveStateChangingCommand(
      this.fileOrThrow(),
      new LModelOpCaptionsAssign(this.model, images, captions)
    );
  }

  @Override
  public void captionRemove(
    final List<LMCaption> captions)
  {
    this.captionsCopied.removeAll(captions);

    this.executeSaveStateChangingCommand(
      this.fileOrThrow(),
      new LModelOpCaptionDelete(
        this.model,
        captions.stream()
          .map(LMCaption::id)
          .collect(Collectors.toList())
      )
    );
  }

  private void executeSaveStateChangingCommand(
    final Path file,
    final LModelOpType operation)
  {
    try {
      operation.execute();
    } catch (final LModelOpException e) {
      this.publishError(e);
      return;
    }

    final var undoStateThen = this.undoState.getValue();
    this.undoState.set(undoStateThen.add(operation));
    this.model.fileStatus().set(new LModelFileStatusType.Unsaved(file));
  }

  @Override
  public void closeSet()
  {

  }

  @Override
  public CompletableFuture<Object> export(
    final LExportRequest request)
  {
    Objects.requireNonNull(request, "Request");

    this.attributes.clear();
    this.attributes.put("Directory", request.outputDirectory().toString());
    this.busySet(true);

    final var future = new CompletableFuture<>();
    future.whenComplete((ignored0, ignored1) -> {
      Platform.runLater(() -> this.busySet(false));
    });

    final var file =
      this.fileOrThrow();

    final LImageSet imageSet;
    try {
      imageSet = this.model.createImageSet();
    } catch (final Exception e) {
      future.completeExceptionally(e);
      return future;
    }

    Thread.ofVirtual()
      .start(() -> {
        try {
          LOG.info("Export: {}", request);
          LExporters.export(request, file.getParent(), imageSet);
          future.complete(null);
        } catch (final Throwable e) {
          LOG.error("", e);
          this.publishError(e);
          future.completeExceptionally(e);
        }
      });

    return future;
  }

  @Override
  public SortedList<LMImage> imageList()
  {
    return this.model.imagesView();
  }

  @Override
  public SortedList<LMCaption> captionsAssigned()
  {
    return this.model.captionsAssigned();
  }

  @Override
  public SortedList<LMCaption> captionsUnassigned()
  {
    return this.model.captionsUnassigned();
  }

  @Override
  public void captionsUnassignedSetFilter(
    final String text)
  {
    Objects.requireNonNull(text, "text");
    this.model.captionsUnassignedSetFilter(text);
  }

  @Override
  public void imagesSetFilter(
    final String text)
  {
    Objects.requireNonNull(text, "text");
    this.model.imagesSetFilter(text);
  }

  @Override
  public ObservableList<String> globalPrefixCaptions()
  {
    return this.model.globalPrefixCaptions();
  }

  @Override
  public void globalPrefixCaptionNew(
    final String text)
  {
    this.executeSaveStateChangingCommand(
      this.fileOrThrow(),
      new LModelOpGlobalPrefixCaptionCreate(
        this.model,
        Math.max(0, this.model.globalPrefixCaptions().size() - 1),
        text
      )
    );
  }

  @Override
  public void globalPrefixCaptionDelete(
    final int index)
  {
    this.executeSaveStateChangingCommand(
      this.fileOrThrow(),
      new LModelOpGlobalPrefixCaptionDelete(this.model, index)
    );
  }

  @Override
  public void globalPrefixCaptionModify(
    final int index,
    final String text)
  {
    this.executeSaveStateChangingCommand(
      this.fileOrThrow(),
      new LModelOpGlobalPrefixCaptionModify(this.model, index, text)
    );
  }

  @Override
  public void imagesDelete(
    final List<LMImage> images)
  {
    this.executeSaveStateChangingCommand(
      this.fileOrThrow(),
      new LModelOpImagesDelete(this.model, images)
    );
  }

  @Override
  public ReadOnlyProperty<LMImage> imageSelected()
  {
    return this.model.imageSelected();
  }

  @Override
  public ObservableList<LMCaption> captionsAssignedCopied()
  {
    return this.captionsCopied;
  }

  @Override
  public void captionsAssignedCopy(
    final List<LMCaption> captions)
  {
    this.captionsCopied.setAll(captions);
  }

  @Override
  public void captionsAssignedPaste()
  {
    this.executeSaveStateChangingCommand(
      this.fileOrThrow(),
      new LModelOpCaptionsAssign(
        this.model,
        List.of(this.imageSelected().getValue().id()),
        this.captionsCopied.stream()
          .map(LMCaption::id)
          .collect(Collectors.toList())
      )
    );
  }

  @Override
  public CompletableFuture<Object> merge(
    final List<Path> files)
  {
    Objects.requireNonNull(files, "files");

    this.attributes.clear();

    for (int index = 0; index < files.size(); ++index) {
      this.attributes.put(
        "File [%d]".formatted(Integer.valueOf(index)),
        files.get(index).toString()
      );
    }

    this.busySet(true);

    final var future = new CompletableFuture<>();
    future.whenComplete((ignored0, ignored1) -> {
      LOG.debug("Merge completed.");
      Platform.runLater(() -> this.busySet(false));
    });

    Thread.ofVirtual()
      .start(() -> {
        final var exceptions =
          new ExceptionTracker<Exception>();

        final var imageSets =
          new ArrayList<LImageSet>(files.size());

        for (final var file : files) {
          try {
            LOG.info("Open: {}", file);
            imageSets.add(PARSERS.parseFile(file));
          } catch (final ParsingException e) {
            LOG.error("", e);
            this.publishParseErrors(e.statusValues());
            this.publishError(e);
            exceptions.addException(e);
          } catch (final Throwable e) {
            LOG.error("", e);
            this.publishError(e);
            exceptions.addException(new Exception(e));
          }
        }

        try {
          exceptions.throwIfNecessary();
        } catch (final Exception e) {
          future.completeExceptionally(e);
          return;
        }

        final var merged =
          LImageSets.mergeAll(imageSets);

        Platform.runLater(() -> {
          try {
            this.model.replaceWith(
              Paths.get(""),
              merged
            );
            future.complete(null);
          } catch (final Exception e) {
            LOG.error("", e);
            this.publishError(e);
            future.completeExceptionally(e);
          }
        });
      });

    return future;
  }

  @Override
  public ReadOnlyProperty<LModelFileStatusType> fileStatus()
  {
    return this.model.fileStatus();
  }
}
