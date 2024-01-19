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
import com.io7m.jattribute.core.AttributeReadableType;
import com.io7m.jattribute.core.AttributeType;
import com.io7m.jattribute.core.Attributes;
import com.io7m.laurel.io.LExportRequest;
import com.io7m.laurel.io.LExporters;
import com.io7m.laurel.io.LParsers;
import com.io7m.laurel.io.LSerializers;
import com.io7m.laurel.model.LCaptionDeleted;
import com.io7m.laurel.model.LCaptionUpdated;
import com.io7m.laurel.model.LEventType;
import com.io7m.laurel.model.LImage;
import com.io7m.laurel.model.LImageCaption;
import com.io7m.laurel.model.LImageCaptionID;
import com.io7m.laurel.model.LImageID;
import com.io7m.laurel.model.LImageRemoved;
import com.io7m.laurel.model.LImageSetCommandException;
import com.io7m.laurel.model.LImageSetCommandType;
import com.io7m.laurel.model.LImageSetType;
import com.io7m.laurel.model.LImageSets;
import com.io7m.laurel.model.LImageUpdated;
import com.io7m.seltzer.api.SStructuredError;
import com.io7m.seltzer.api.SStructuredErrorType;
import javafx.application.Platform;
import javafx.beans.Observable;
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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

import static com.io7m.laurel.gui.internal.LImageSetStateNone.NO_IMAGE_SET;

/**
 * The controller.
 */

public final class LController
  implements LControllerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LController.class);
  private static final LSerializers SERIALIZERS =
    new LSerializers();
  private static final LParsers PARSERS =
    new LParsers();

  private final AttributeType<Boolean> busy;
  private final SubmissionPublisher<SStructuredErrorType<String>> errors;
  private final ConcurrentHashMap<String, String> attributes;
  private final AttributeType<LImageSetStateType> imageSetState;
  private final ObservableList<LImage> imageList;
  private final SortedList<LImage> imageListReadable;
  private final ObservableList<LImageCaption> captionListAssigned;
  private final ObservableList<LImageCaption> captionListAssignedReadable;
  private final ObservableList<LImageCaption> captionListAvailable;
  private final AttributeType<LUndoState> undoState;
  private final AttributeType<Optional<LImageID>> imageSelected;
  private final SortedList<LImageCaption> captionListAvailableSorted;

  /**
   * Construct a controller.
   *
   * @param inAttributes The attributes
   */

  public LController(
    final Attributes inAttributes)
  {
    this.imageSetState =
      inAttributes.create(NO_IMAGE_SET);
    this.busy =
      inAttributes.create(Boolean.FALSE);
    this.undoState =
      inAttributes.create(LUndoState.empty());

    this.errors =
      new SubmissionPublisher<>();
    this.attributes =
      new ConcurrentHashMap<>();

    this.imageSelected =
      inAttributes.create(Optional.empty());
    this.imageList =
      FXCollections.observableArrayList(param -> {
        return new Observable[]{
          new SimpleObjectProperty<>(param),
        };
      });
    this.imageListReadable =
      new SortedList<>(this.imageList);

    this.captionListAssigned =
      FXCollections.observableArrayList(param -> {
        return new Observable[]{
          new SimpleObjectProperty<>(param),
        };
      });
    this.captionListAssignedReadable =
      FXCollections.unmodifiableObservableList(this.captionListAssigned);

    this.captionListAvailable =
      FXCollections.observableArrayList();
    this.captionListAvailableSorted =
      new SortedList<>(this.captionListAvailable);

    this.imageSelected.subscribe((oldValue, newValue) -> {
      this.onImageSelected(newValue);
    });
  }

  private void updateCaptions()
  {
    final LImageSetType imageSet;
    try {
      imageSet = this.fileStateOrThrow().imageSet();
    } catch (final Exception e) {
      return;
    }

    final var selectedOpt =
      this.imageSelected.get();

    if (selectedOpt.isPresent()) {
      final var imageId =
        selectedOpt.get();
      final var captionsAll =
        new TreeSet<>(imageSet.captions().values());

      final List<LImageCaption> captionsAssigned =
        this.imageSetState()
          .get()
          .imageSet()
          .captionsForImage(imageId);

      captionsAll.removeAll(captionsAssigned);
      this.captionListAssigned.setAll(captionsAssigned);
      this.captionListAvailable.setAll(captionsAll);
    } else {
      this.captionListAssigned.clear();
      this.captionListAvailable.setAll(imageSet.captions().values());
    }
  }

  private void updateImages()
  {
    final LImageSetType imageSet;
    try {
      imageSet = this.fileStateOrThrow().imageSet();
    } catch (final Exception e) {
      return;
    }

    /*
     * For whatever reason, we can't use setAll() on the image list as this
     * causes the list view to lose the selection.
     */

    final var images =
      imageSet.images();
    final var toAdd =
      new TreeSet<>(images.keySet());
    final var toRemove =
      new TreeSet<LImage>();

    /*
     * For all images in the list, an image is to be removed if it doesn't
     * exist in the current image set. An image is to be added if it exists
     * in the image set but doesn't exist in the current list.
     */

    for (final var existing : this.imageList) {
      if (!images.containsKey(existing.imageID())) {
        toRemove.add(existing);
      }
      toAdd.remove(existing.imageID());
    }

    this.imageList.removeAll(toRemove);
    for (final var imageId : toAdd) {
      this.imageList.add(images.get(imageId));
    }

    /*
     * Now the image list reflects the set of images in the current image
     * set, but the actual values might not be quite up-to-date.
     */

    for (int index = 0; index < this.imageList.size(); ++index) {
      this.imageList.set(
        index,
        images.get(this.imageList.get(index).imageID()));
    }
  }

  private void onImageSelected(
    final Optional<LImageID> newValue)
  {
    Platform.runLater(this::updateCaptions);
  }

  @Override
  public SortedList<LImage> imageListReadable()
  {
    return this.imageListReadable;
  }

  @Override
  public ObservableList<LImageCaption> captionListAssigned()
  {
    return this.captionListAssignedReadable;
  }

  @Override
  public SortedList<LImageCaption> captionListAvailable()
  {
    return this.captionListAvailableSorted;
  }

  @Override
  public Flow.Publisher<SStructuredErrorType<String>> errors()
  {
    return this.errors;
  }

  @Override
  public AttributeReadableType<LUndoState> undoState()
  {
    return this.undoState;
  }

  @Override
  public AttributeReadableType<Boolean> busy()
  {
    return this.busy;
  }

  @Override
  public AttributeReadableType<LImageSetStateType> imageSetState()
  {
    return this.imageSetState;
  }

  @Override
  public boolean isBusy()
  {
    return this.busy.get().booleanValue();
  }

  @Override
  public String description()
  {
    return "Main controller.";
  }

  @Override
  public boolean isSaved()
  {
    return switch (this.imageSetState.get()) {
      case final LImageSetSaved saved -> {
        yield true;
      }
      case final LImageSetStateNone none -> {
        yield true;
      }
      case final LImageSetUnsaved unsaved -> {
        yield false;
      }
    };
  }

  @Override
  public CompletableFuture<Object> save(
    final Path path)
  {
    Objects.requireNonNull(path, "path");

    this.attributes.clear();
    this.attributes.put("File", path.toString());

    final var tmpFile =
      path.resolveSibling(path.getFileName() + ".tmp");

    final var future = new CompletableFuture<>();
    Thread.ofVirtual()
      .start(() -> {
        try {
          this.busy.set(Boolean.TRUE);

          final var state = this.imageSetState.get();
          SERIALIZERS.serializeFile(tmpFile, state.imageSet());
          Files.move(
            tmpFile,
            path,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING
          );

          LOG.info("Saved: {} -> {}", tmpFile, path);
          this.imageSetState.set(new LImageSetSaved(path, state.imageSet()));
          future.complete(null);
        } catch (final Throwable e) {
          LOG.error("", e);
          this.publishError(e);
          future.completeExceptionally(e);
        } finally {
          this.busy.set(Boolean.FALSE);
        }
      });

    return future;
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
  public CompletableFuture<Object> open(
    final Path path)
  {
    Objects.requireNonNull(path, "path");

    this.attributes.clear();
    this.attributes.put("File", path.toString());

    final var future = new CompletableFuture<>();
    Thread.ofVirtual()
      .start(() -> {
        try {
          this.busy.set(Boolean.TRUE);
          LOG.info("Open: {}", path);

          final var newImageSet = PARSERS.parseFile(path);
          this.closeExistingSet();
          this.imageSetState.set(new LImageSetSaved(path, newImageSet));

          newImageSet.events()
            .subscribe(new LPerpetualSubscriber<>(this::onImageSetEvent));

          Platform.runLater(() -> {
            this.updateImages();
            this.updateCaptions();
          });

          this.undoState.set(LUndoState.empty());
          future.complete(null);
        } catch (final ParsingException e) {
          LOG.error("", e);
          this.publishParseErrors(e.statusValues());
          this.publishError(e);
          future.completeExceptionally(e);
        } catch (final Throwable e) {
          LOG.error("", e);
          this.publishError(e);
          future.completeExceptionally(e);
        } finally {
          this.busy.set(Boolean.FALSE);
        }
      });
    return future;
  }

  private void onImageSetEvent(
    final LEventType e)
  {
    LOG.debug("Image event: {}", e);

    switch (e) {
      case final LCaptionDeleted captionDeleted -> {
        Platform.runLater(this::updateCaptions);
      }
      case final LCaptionUpdated captionUpdated -> {
        Platform.runLater(this::updateCaptions);
      }
      case final LImageRemoved imageRemoved -> {
        Platform.runLater(this::updateImages);
      }
      case final LImageUpdated imageUpdated -> {
        Platform.runLater(this::updateImages);
      }
    }
  }

  private void closeExistingSet()
  {
    final var existingImageSet = this.imageSetState.get().imageSet();
    existingImageSet.close();
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
  public CompletableFuture<Object> newSet(
    final Path file)
  {
    this.attributes.clear();
    this.attributes.put("File", file.toString());

    final var future = new CompletableFuture<>();
    Thread.ofVirtual()
      .start(() -> {
        try {
          this.busy.set(Boolean.TRUE);
          this.undoState.set(LUndoState.empty());
          this.closeExistingSet();

          final var newImageSet = LImageSets.empty();
          this.imageSetState.set(new LImageSetUnsaved(file, newImageSet));

          newImageSet.events()
            .subscribe(new LPerpetualSubscriber<>(this::onImageSetEvent));

          Platform.runLater(() -> {
            this.captionListAssigned.clear();
            this.captionListAvailable.clear();
            this.imageList.clear();
          });
          future.complete(null);
        } catch (final Throwable e) {
          LOG.error("", e);
          this.publishError(e);
          future.completeExceptionally(e);
        } finally {
          this.busy.set(Boolean.FALSE);
        }
      });

    return future;
  }

  @Override
  public Optional<Path> imageSelect(
    final Optional<LImageID> imageOpt)
  {
    this.imageSelected.set(imageOpt);

    return imageOpt.flatMap(imageID -> {
      final var file = this.fileStateOrThrow().file();
      return Optional.ofNullable(
        this.imageSetState.get()
          .imageSet()
          .images()
          .get(imageID)
          .fileName()
      ).map(name -> {
        return file.getParent().resolve(name);
      });
    });
  }

  @Override
  public void undo()
  {
    final var undoStateNow =
      this.undoState.get();
    final var undoStackNew =
      new LinkedList<>(undoStateNow.undoStack());
    final var redoStackNew =
      new LinkedList<>(undoStateNow.redoStack());

    final var command = undoStackNew.removeFirst();
    if (command != null) {
      LOG.debug("Undo: {}", command.description());
      try {
        command.undo();
      } catch (final LImageSetCommandException e) {
        this.publishError(e);
        return;
      }
      redoStackNew.addFirst(command);
      this.undoState.set(new LUndoState(undoStackNew, redoStackNew));
    }
  }

  @Override
  public void redo()
  {
    final var undoStateNow =
      this.undoState.get();
    final var undoStackNew =
      new LinkedList<>(undoStateNow.undoStack());
    final var redoStackNew =
      new LinkedList<>(undoStateNow.redoStack());

    final var command = redoStackNew.removeFirst();
    if (command != null) {
      LOG.debug("Redo: {}", command.description());
      try {
        command.execute();
      } catch (final LImageSetCommandException e) {
        this.publishError(e);
        return;
      }
      undoStackNew.addFirst(command);
      this.undoState.set(new LUndoState(undoStackNew, redoStackNew));
    }
  }

  @Override
  public CompletableFuture<Object> imagesAdd(
    final List<Path> files)
  {
    Objects.requireNonNull(files, "files");

    this.attributes.clear();

    final var future = new CompletableFuture<>();
    Thread.ofVirtual()
      .start(() -> {
        try {
          this.busy.set(Boolean.TRUE);
          LOG.info("Add Files: {}", files);

          final var imageSet =
            this.imageSetState.get()
              .imageSet();

          int index = 0;

          final var commands =
            new ArrayList<LImageSetCommandType>(files.size());

          for (final var file : files) {
            this.attributes.put(
              "File[%d]".formatted(Integer.valueOf(index)), file.toString());

            final var bufferedImage = ImageIO.read(file.toFile());
            if (bufferedImage == null) {
              throw new IOException(
                "Unable to open image: %s".formatted(file)
              );
            }

            final var image =
              new LImage(
                new LImageID(UUID.randomUUID()),
                file.getFileName().toString(),
                List.of()
              );

            commands.add(imageSet.imageUpdate(image));
            ++index;
          }

          final var command =
            imageSet.compose("Load images", commands);
          command.execute();

          this.undoState.set(this.undoState.get().add(command));
          future.complete(null);
        } catch (final Throwable e) {
          LOG.error("", e);
          this.publishError(e);
          future.completeExceptionally(e);
        } finally {
          this.busy.set(Boolean.FALSE);
        }
      });

    return future;
  }

  @Override
  public CompletableFuture<Object> save()
  {
    return this.save(this.fileStateOrThrow().file());
  }

  @Override
  public void captionNew(
    final String text)
  {
    final var state =
      this.fileStateOrThrow();
    final var newCaption =
      new LImageCaption(new LImageCaptionID(UUID.randomUUID()), text);

    this.executeSaveStateChangingCommand(
      state.imageSet().captionUpdate(newCaption),
      state.file(),
      state.imageSet()
    );
  }

  private LImageSetStateWithFileType fileStateOrThrow()
  {
    return switch (this.imageSetState.get()) {
      case final LImageSetSaved saved -> {
        yield saved;
      }
      case final LImageSetStateNone ignored -> {
        throw new IllegalStateException();
      }
      case final LImageSetUnsaved unsaved -> {
        yield unsaved;
      }
    };
  }

  @Override
  public void imageCaptionUnassign(
    final LImageID image,
    final List<LImageCaptionID> captions)
  {
    final var state =
      this.fileStateOrThrow();
    final var subcommands =
      new ArrayList<LImageSetCommandType>(captions.size());

    for (final var caption : captions) {
      subcommands.add(state.imageSet().captionUnassign(image, caption));
    }

    this.executeSaveStateChangingCommand(
      state.imageSet().compose("Assign captions", subcommands),
      state.file(),
      state.imageSet()
    );
  }

  @Override
  public void imageCaptionAssign(
    final LImageID image,
    final List<LImageCaptionID> captions)
  {
    final var state =
      this.fileStateOrThrow();
    final var subcommands =
      new ArrayList<LImageSetCommandType>(captions.size());

    for (final var caption : captions) {
      subcommands.add(state.imageSet().captionAssign(image, caption));
    }

    this.executeSaveStateChangingCommand(
      state.imageSet().compose("Unassign captions", subcommands),
      state.file(),
      state.imageSet()
    );
  }

  @Override
  public void captionRemove(
    final List<LImageCaption> captions)
  {
    final var state =
      this.fileStateOrThrow();
    final var subcommands =
      new ArrayList<LImageSetCommandType>(captions.size());

    for (final var caption : captions) {
      subcommands.add(state.imageSet().captionRemove(caption.id()));
    }

    this.executeSaveStateChangingCommand(
      state.imageSet().compose("Remove captions", subcommands),
      state.file(),
      state.imageSet()
    );
  }

  private void executeSaveStateChangingCommand(
    final LImageSetCommandType command,
    final Path file,
    final LImageSetType imageSet)
  {
    try {
      command.execute();
    } catch (final LImageSetCommandException e) {
      this.publishError(e);
      return;
    }

    this.undoState.set(this.undoState.get().add(command));
    this.imageSetState.set(new LImageSetUnsaved(file, imageSet));
  }

  @Override
  public void imageCaptionPriorityIncrease(
    final LImageID imageID,
    final LImageCaptionID captionID)
  {
    final var state =
      this.fileStateOrThrow();

    this.executeSaveStateChangingCommand(
      state.imageSet().captionPriorityIncrease(imageID, captionID),
      state.file(),
      state.imageSet()
    );
  }

  @Override
  public void imageCaptionPriorityDecrease(
    final LImageID imageID,
    final LImageCaptionID captionID)
  {
    final var state =
      this.fileStateOrThrow();

    this.executeSaveStateChangingCommand(
      state.imageSet().captionPriorityDecrease(imageID, captionID),
      state.file(),
      state.imageSet()
    );
  }

  @Override
  public long captionCount(
    final LImageCaptionID captionID)
  {
    return this.fileStateOrThrow().imageSet().captionAssignmentCount(captionID);
  }

  @Override
  public void closeSet()
  {
    this.attributes.clear();
    this.undoState.set(LUndoState.empty());
    this.closeExistingSet();

    Platform.runLater(() -> {
      this.captionListAssigned.clear();
      this.captionListAvailable.clear();
      this.imageList.clear();
      this.imageSetState.set(NO_IMAGE_SET);
    });
  }

  @Override
  public CompletableFuture<Object> export(
    final LExportRequest request)
  {
    Objects.requireNonNull(request, "Request");

    this.attributes.clear();
    this.attributes.put("Directory", request.outputDirectory().toString());

    final var future = new CompletableFuture<>();
    Thread.ofVirtual()
      .start(() -> {
        try {
          this.busy.set(Boolean.TRUE);
          LOG.info("Export: {}", request);

          final var state = this.fileStateOrThrow();
          LExporters.export(
            request,
            state.file().getParent(),
            state.imageSet()
          );

          future.complete(null);
        } catch (final Throwable e) {
          LOG.error("", e);
          this.publishError(e);
          future.completeExceptionally(e);
        } finally {
          this.busy.set(Boolean.FALSE);
        }
      });
    return future;
  }
}
