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

import com.io7m.darco.api.DDatabaseCreate;
import com.io7m.darco.api.DDatabaseException;
import com.io7m.darco.api.DDatabaseTelemetryNoOp;
import com.io7m.darco.api.DDatabaseUnit;
import com.io7m.darco.api.DDatabaseUpgrade;
import com.io7m.jattribute.core.AttributeReadableType;
import com.io7m.jattribute.core.AttributeType;
import com.io7m.jattribute.core.Attributes;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.laurel.filemodel.LCategoryCaptionsAssignment;
import com.io7m.laurel.filemodel.LFileModelEvent;
import com.io7m.laurel.filemodel.LFileModelType;
import com.io7m.laurel.filemodel.LImageCaptionsAssignment;
import com.io7m.laurel.model.LCaption;
import com.io7m.laurel.model.LCaptionName;
import com.io7m.laurel.model.LCategory;
import com.io7m.laurel.model.LCategoryID;
import com.io7m.laurel.model.LCategoryName;
import com.io7m.laurel.model.LException;
import com.io7m.laurel.model.LImageID;
import com.io7m.laurel.model.LImageWithID;
import com.io7m.laurel.model.LMetadataValue;
import com.io7m.seltzer.api.SStructuredErrorType;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.locks.ReentrantLock;

import static com.io7m.laurel.filemodel.internal.Tables.IMAGES;
import static com.io7m.laurel.filemodel.internal.Tables.IMAGE_BLOBS;
import static com.io7m.laurel.filemodel.internal.Tables.REDO;
import static com.io7m.laurel.filemodel.internal.Tables.UNDO;
import static java.time.ZoneOffset.UTC;

/**
 * The file model.
 */

public final class LFileModel implements LFileModelType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LFileModel.class);

  private static final Attributes ATTRIBUTES =
    Attributes.create(throwable -> {
      LOG.error("Uncaught attribute exception: ", throwable);
    });

  private final AttributeType<List<LCaption>> categoryCaptionsAssigned;
  private final AttributeType<List<LCaption>> categoryCaptionsUnassigned;
  private final AttributeType<List<LCaption>> imageCaptionsAssigned;
  private final AttributeType<List<LCaption>> imageCaptionsUnassigned;
  private final AttributeType<List<LCaption>> tagsAll;
  private final AttributeType<List<LCategory>> categoriesAll;
  private final AttributeType<List<LCategory>> categoriesRequired;
  private final AttributeType<List<LImageWithID>> imagesAll;
  private final AttributeType<List<LMetadataValue>> metadata;
  private final AttributeType<Optional<? extends LCommandType<?>>> redo;
  private final AttributeType<Optional<? extends LCommandType<?>>> undo;
  private final AttributeType<Optional<LCategory>> categorySelected;
  private final AttributeType<Optional<LImageWithID>> imageSelected;
  private final AttributeType<Optional<String>> redoText;
  private final AttributeType<Optional<String>> undoText;
  private final AttributeType<SortedMap<LCategoryID, List<LCaption>>> categoryCaptions;
  private final CloseableCollectionType<LException> resources;
  private final ConcurrentHashMap<String, String> attributes;
  private final LDatabaseType database;
  private final ReentrantLock commandLock;
  private final SubmissionPublisher<LFileModelEvent> events;

  private LFileModel(
    final LDatabaseType inDatabase)
  {
    this.database =
      Objects.requireNonNull(inDatabase, "database");
    this.metadata =
      ATTRIBUTES.withValue(List.of());
    this.categoriesAll =
      ATTRIBUTES.withValue(List.of());
    this.categoriesRequired =
      ATTRIBUTES.withValue(List.of());
    this.categoryCaptions =
      ATTRIBUTES.withValue(Collections.emptySortedMap());
    this.tagsAll =
      ATTRIBUTES.withValue(List.of());
    this.imageCaptionsAssigned =
      ATTRIBUTES.withValue(List.of());
    this.imageCaptionsUnassigned =
      ATTRIBUTES.withValue(List.of());
    this.categoryCaptionsAssigned =
      ATTRIBUTES.withValue(List.of());
    this.categoryCaptionsUnassigned =
      ATTRIBUTES.withValue(List.of());
    this.imagesAll =
      ATTRIBUTES.withValue(List.of());
    this.imageSelected =
      ATTRIBUTES.withValue(Optional.empty());
    this.categorySelected =
      ATTRIBUTES.withValue(Optional.empty());
    this.undo =
      ATTRIBUTES.withValue(Optional.empty());
    this.undoText =
      this.undo.map(o -> o.map(LCommandType::describe));
    this.redo =
      ATTRIBUTES.withValue(Optional.empty());
    this.redoText =
      this.redo.map(o -> o.map(LCommandType::describe));
    this.commandLock =
      new ReentrantLock();
    this.attributes =
      new ConcurrentHashMap<>();

    this.resources =
      CloseableCollection.create(() -> {
        return new LException(
          "One or more resources could not be closed.",
          "error-resource-close",
          Map.of(),
          Optional.empty()
        );
      });

    this.resources.add(this.database);
    this.events = this.resources.add(new SubmissionPublisher<>());

    this.resources.add(
      this.tagsAll.subscribe(
        (_0, _1) -> this.onImageCaptionsUnassignedRecalculate())
    );
    this.resources.add(
      this.imageCaptionsAssigned.subscribe(
        (_0, _1) -> this.onImageCaptionsUnassignedRecalculate())
    );

    this.resources.add(
      this.tagsAll.subscribe(
        (_0, _1) -> this.onCategoryCaptionsUnassignedRecalculate())
    );
    this.resources.add(
      this.categoryCaptionsAssigned.subscribe(
        (_0, _1) -> this.onCategoryCaptionsUnassignedRecalculate())
    );
  }

  /**
   * Open a file model.
   *
   * @param file     The file
   * @param readOnly {@code true} if the file should be read-only
   *
   * @return A file model
   *
   * @throws LException On errors
   */

  public static LFileModelType open(
    final Path file,
    final boolean readOnly)
    throws LException
  {
    final var model = openModel(file, readOnly);
    model.load();
    return model;
  }

  private static LFileModel openModel(
    final Path file,
    final boolean readOnly)
    throws LException
  {
    try {
      final var databases =
        new LDatabaseFactory();

      if (readOnly) {
        return new LFileModel(
          databases.open(
            new LDatabaseConfiguration(
              DDatabaseTelemetryNoOp.get(),
              DDatabaseCreate.DO_NOT_CREATE_DATABASE,
              DDatabaseUpgrade.DO_NOT_UPGRADE_DATABASE,
              file,
              readOnly
            ),
            event -> {

            }
          )
        );
      }

      return new LFileModel(
        databases.open(
          new LDatabaseConfiguration(
            DDatabaseTelemetryNoOp.get(),
            DDatabaseCreate.CREATE_DATABASE,
            DDatabaseUpgrade.UPGRADE_DATABASE,
            file,
            readOnly
          ),
          event -> {

          }
        )
      );
    } catch (final DDatabaseException e) {
      throw new LException(
        e.getMessage(),
        e,
        e.errorCode(),
        e.attributes(),
        e.remediatingAction()
      );
    }
  }

  private static long nowMilliseconds()
  {
    return OffsetDateTime.now(UTC)
      .toInstant()
      .toEpochMilli();
  }

  private static void dbUndoMoveToRedo(
    final LDatabaseTransactionType t,
    final Record oldCommandRec)
  {
    final var context =
      t.get(DSLContext.class);

    final var id =
      oldCommandRec.get(UNDO.UNDO_ID);

    context.deleteFrom(UNDO)
      .where(UNDO.UNDO_ID.eq(id))
      .execute();

    context.insertInto(REDO)
      .set(REDO.REDO_ID, id)
      .set(REDO.REDO_DESCRIPTION, oldCommandRec.get(UNDO.UNDO_DESCRIPTION))
      .set(REDO.REDO_DATA, oldCommandRec.get(UNDO.UNDO_DATA))
      .set(REDO.REDO_TIME, oldCommandRec.get(UNDO.UNDO_TIME))
      .execute();
  }

  private static void dbRedoMoveToUndo(
    final LDatabaseTransactionType t,
    final Record oldCommandRec)
  {
    final var context =
      t.get(DSLContext.class);

    final var id =
      oldCommandRec.get(REDO.REDO_ID);

    context.deleteFrom(REDO)
      .where(REDO.REDO_ID.eq(id))
      .execute();

    context.insertInto(UNDO)
      .set(UNDO.UNDO_ID, id)
      .set(UNDO.UNDO_DESCRIPTION, oldCommandRec.get(REDO.REDO_DESCRIPTION))
      .set(UNDO.UNDO_DATA, oldCommandRec.get(REDO.REDO_DATA))
      .set(UNDO.UNDO_TIME, oldCommandRec.get(REDO.REDO_TIME))
      .execute();
  }

  private static LCommandType<?> parseUndoCommandFromProperties(
    final org.jooq.Record rec)
    throws IOException
  {
    final var properties =
      new Properties();

    try (var stream = new ByteArrayInputStream(rec.get(UNDO.UNDO_DATA))) {
      properties.loadFromXML(stream);
    }

    return LCommands.forProperties(properties);
  }

  private static LCommandType<?> parseRedoCommandFromProperties(
    final org.jooq.Record rec)
    throws IOException
  {
    final var properties =
      new Properties();

    try (var stream = new ByteArrayInputStream(rec.get(REDO.REDO_DATA))) {
      properties.loadFromXML(stream);
    }

    return LCommands.forProperties(properties);
  }

  private static Optional<org.jooq.Record> dbUndoGetTip(
    final LDatabaseTransactionType t)
  {
    final var context =
      t.get(DSLContext.class);

    return context.select(
        UNDO.UNDO_DATA,
        UNDO.UNDO_TIME,
        UNDO.UNDO_DESCRIPTION,
        UNDO.UNDO_ID
      ).from(UNDO)
      .orderBy(UNDO.UNDO_TIME.desc(), UNDO.UNDO_ID.desc())
      .limit(1)
      .fetchOptional()
      .map(x -> x);
  }

  private static Optional<org.jooq.Record> dbRedoGetTip(
    final LDatabaseTransactionType t)
  {
    final var context =
      t.get(DSLContext.class);

    return context.select(
        REDO.REDO_DATA,
        REDO.REDO_TIME,
        REDO.REDO_DESCRIPTION,
        REDO.REDO_ID
      ).from(REDO)
      .orderBy(REDO.REDO_TIME.asc(), REDO.REDO_ID.asc())
      .limit(1)
      .fetchOptional()
      .map(x -> x);
  }

  private void onImageCaptionsUnassignedRecalculate()
  {
    final var unassigned =
      new TreeSet<>(this.tagsAll.get());
    final var assigned =
      new HashSet<>(this.imageCaptionsAssigned.get());

    unassigned.removeAll(assigned);

    this.imageCaptionsUnassigned.set(unassigned.stream().toList());
  }

  private void onCategoryCaptionsUnassignedRecalculate()
  {
    final var unassigned =
      new TreeSet<>(this.tagsAll.get());
    final var assigned =
      new HashSet<>(this.categoryCaptionsAssigned.get());

    unassigned.removeAll(assigned);

    this.categoryCaptionsUnassigned.set(unassigned.stream().toList());
  }

  private void load()
  {
    this.runCommand(
      new LCommandLoad(),
      DDatabaseUnit.UNIT
    );
  }

  void setAttribute(
    final String name,
    final String value)
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(value, "value");

    this.attributes.put(name, value);
  }

  void setAttribute(
    final String name,
    final Object value)
  {
    this.setAttribute(name, value.toString());
  }

  @Override
  public SubmissionPublisher<LFileModelEvent> events()
  {
    return this.events;
  }

  @Override
  public CompletableFuture<?> categoryAdd(
    final LCategoryName text)
  {
    return this.runCommand(new LCommandCategoriesAdd(), List.of(text));
  }

  @Override
  public CompletableFuture<?> captionAdd(
    final LCaptionName text)
  {
    return this.runCommand(new LCommandCaptionsAdd(), List.of(text));
  }

  @Override
  public CompletableFuture<?> categorySetRequired(
    final Set<LCategoryID> categories)
  {
    return this.runCommand(
      new LCommandCategoriesSetRequired(), List.copyOf(categories));
  }

  @Override
  public CompletableFuture<?> categorySetNotRequired(
    final Set<LCategoryID> categories)
  {
    return this.runCommand(
      new LCommandCategoriesUnsetRequired(), List.copyOf(categories));
  }

  @Override
  public CompletableFuture<?> imageAdd(
    final String name,
    final Path file,
    final Optional<URI> source)
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(source, "source");

    return this.runCommand(
      new LCommandImagesAdd(),
      List.of(new LImageRequest(name, file, source))
    );
  }

  @Override
  public CompletableFuture<?> imagesDelete(
    final List<LImageID> ids)
  {
    Objects.requireNonNull(ids, "ids");

    return this.runCommand(
      new LCommandImagesDelete(),
      ids
    );
  }

  @Override
  public CompletableFuture<?> imageCaptionsAssign(
    final List<LImageCaptionsAssignment> assignments)
  {
    Objects.requireNonNull(assignments, "assignments");

    return this.runCommand(new LCommandImageCaptionsAssign(), assignments);
  }

  @Override
  public CompletableFuture<?> imageCaptionsUnassign(
    final List<LImageCaptionsAssignment> assignments)
  {
    Objects.requireNonNull(assignments, "assignments");

    return this.runCommand(new LCommandImageCaptionsUnassign(), assignments);
  }

  @Override
  public CompletableFuture<?> imageSelect(
    final Optional<LImageID> name)
  {
    Objects.requireNonNull(name, "name");

    return this.runCommand(
      new LCommandImageSelect(),
      name
    );
  }

  @Override
  public CompletableFuture<?> categoryCaptionsAssign(
    final List<LCategoryCaptionsAssignment> categories)
  {
    Objects.requireNonNull(categories, "categories");

    return this.runCommand(
      new LCommandCategoryCaptionsAssign(),
      categories
    );
  }

  @Override
  public CompletableFuture<?> categoryCaptionsUnassign(
    final List<LCategoryCaptionsAssignment> categories)
  {
    Objects.requireNonNull(categories, "categories");

    return this.runCommand(
      new LCommandCategoryCaptionsUnassign(),
      categories
    );
  }

  @Override
  public CompletableFuture<?> categorySelect(
    final Optional<LCategoryID> id)
  {
    Objects.requireNonNull(id, "id");

    return this.runCommand(
      new LCommandCategorySelect(),
      id
    );
  }

  @Override
  public CompletableFuture<?> metadataPut(
    final List<LMetadataValue> values)
  {
    Objects.requireNonNull(values, "metadata");

    return this.runCommand(
      new LCommandMetadataAdd(),
      values
    );
  }

  @Override
  public CompletableFuture<?> metadataRemove(
    final List<LMetadataValue> values)
  {
    Objects.requireNonNull(values, "metadata");

    return this.runCommand(
      new LCommandMetadataRemove(),
      values
    );
  }

  private <P, C extends LCommandType<P>>
  CompletableFuture<?>
  runCommand(
    final C command,
    final P parameters)
  {
    final var future = new CompletableFuture<Void>();
    Thread.ofVirtual()
      .start(() -> {
        try {
          this.executeCommandLocked(command, parameters);
          future.complete(null);
        } catch (final Throwable e) {
          future.completeExceptionally(e);
        }
      });
    return future;
  }

  private <P, C extends LCommandType<P>>
  void executeCommandLocked(
    final C command,
    final P parameters)
    throws Exception
  {
    this.commandLock.lock();

    try {
      this.attributes.clear();

      try (var t = this.database.openTransaction()) {
        final var undoable =
          command.execute(this, t, parameters);

        switch (undoable) {
          case COMMAND_UNDOABLE -> {
            final var context = t.get(DSLContext.class);
            context.insertInto(UNDO)
              .set(UNDO.UNDO_DESCRIPTION, command.describe())
              .set(UNDO.UNDO_TIME, Long.valueOf(nowMilliseconds()))
              .set(UNDO.UNDO_DATA, command.serialize())
              .execute();
          }
          case COMMAND_NOT_UNDOABLE -> {

          }
        }

        t.commit();

        if (command.requiresCompaction()) {
          final var context = t.get(DSLContext.class);
          context.execute("VACUUM");
        }

        switch (undoable) {
          case COMMAND_UNDOABLE -> {
            this.undo.set(Optional.of(command));
          }
          case COMMAND_NOT_UNDOABLE -> {

          }
        }
      } catch (final Throwable e) {
        LOG.debug("Exception: ", e);
        throw this.handleThrowable(e);
      }
    } finally {
      this.commandLock.unlock();
    }
  }

  private LException handleThrowable(
    final Throwable e)
  {
    if (e instanceof final SStructuredErrorType<?> x) {
      this.attributes.putAll(x.attributes());
      return new LException(
        e.getMessage(),
        e,
        x.errorCode().toString(),
        this.attributes(),
        x.remediatingAction()
      );
    }

    return new LException(
      e.getMessage(),
      e,
      "error-exception",
      this.attributes(),
      Optional.empty()
    );
  }

  @Override
  public void close()
    throws LException
  {
    this.resources.close();
  }

  @Override
  public AttributeReadableType<List<LMetadataValue>> metadataList()
  {
    return this.metadata;
  }

  @Override
  public AttributeReadableType<Optional<LCategory>> categorySelected()
  {
    return this.categorySelected;
  }

  @Override
  public AttributeReadableType<Optional<LImageWithID>> imageSelected()
  {
    return this.imageSelected;
  }

  @Override
  public AttributeReadableType<List<LImageWithID>> imageList()
  {
    return this.imagesAll;
  }

  @Override
  public AttributeReadableType<List<LCategory>> categoriesRequired()
  {
    return this.categoriesRequired;
  }

  @Override
  public AttributeReadableType<List<LCaption>> captionList()
  {
    return this.tagsAll;
  }

  @Override
  public AttributeReadableType<List<LCaption>> imageCaptionsAssigned()
  {
    return this.imageCaptionsAssigned;
  }

  @Override
  public AttributeReadableType<List<LCaption>> imageCaptionsUnassigned()
  {
    return this.imageCaptionsUnassigned;
  }

  @Override
  public AttributeReadableType<Optional<String>> undoText()
  {
    return this.undoText;
  }

  @Override
  public AttributeReadableType<List<LCaption>> categoryCaptionsAssigned()
  {
    return this.categoryCaptionsAssigned;
  }

  @Override
  public AttributeReadableType<List<LCaption>> categoryCaptionsUnassigned()
  {
    return this.categoryCaptionsUnassigned;
  }

  @Override
  public CompletableFuture<?> undo()
  {
    final var future = new CompletableFuture<Void>();
    Thread.ofVirtual()
      .start(() -> {
        try {
          this.executeUndo();
          future.complete(null);
        } catch (final Throwable e) {
          future.completeExceptionally(e);
        }
      });
    return future;
  }

  private void executeUndo()
    throws Exception
  {
    this.commandLock.lock();

    try {
      this.attributes.clear();

      try (var t = this.database.openTransaction()) {
        final var oldCommandRecOpt = dbUndoGetTip(t);
        if (oldCommandRecOpt.isEmpty()) {
          return;
        }

        final var oldCommandRec =
          oldCommandRecOpt.get();
        final var oldCommand =
          parseUndoCommandFromProperties(oldCommandRec);

        oldCommand.undo(this, t);
        dbUndoMoveToRedo(t, oldCommandRec);
        t.commit();

        this.redo.set(Optional.of(oldCommand));

        final var newCommandRecOpt = dbUndoGetTip(t);
        if (newCommandRecOpt.isPresent()) {
          this.undo.set(Optional.of(
            parseUndoCommandFromProperties(newCommandRecOpt.get()))
          );
        } else {
          this.undo.set(Optional.empty());
        }

      } catch (final Throwable e) {
        throw this.handleThrowable(e);
      }
    } finally {
      this.commandLock.unlock();
    }
  }

  @Override
  public CompletableFuture<?> redo()
  {
    final var future = new CompletableFuture<Void>();
    Thread.ofVirtual()
      .start(() -> {
        try {
          this.executeRedo();
          future.complete(null);
        } catch (final Throwable e) {
          future.completeExceptionally(e);
        }
      });
    return future;
  }

  @Override
  public CompletableFuture<?> compact()
  {
    return this.runCommand(
      new LCommandCompact(),
      DDatabaseUnit.UNIT
    );
  }

  @Override
  public AttributeReadableType<Optional<String>> redoText()
  {
    return this.redoText;
  }

  @Override
  public AttributeReadableType<List<LCategory>> categoryList()
  {
    return this.categoriesAll;
  }

  @Override
  public AttributeReadableType<SortedMap<LCategoryID, List<LCaption>>> categoryCaptions()
  {
    return this.categoryCaptions;
  }

  @Override
  public CompletableFuture<Optional<InputStream>> imageStream(
    final LImageID id)
  {
    final var future = new CompletableFuture<Optional<InputStream>>();
    Thread.ofVirtual()
      .start(() -> {
        try {
          future.complete(this.executeImageStream(id));
        } catch (final Throwable e) {
          future.completeExceptionally(e);
        }
      });
    return future;
  }

  private Optional<InputStream> executeImageStream(
    final LImageID id)
    throws LException
  {
    this.commandLock.lock();

    try {
      this.attributes.clear();

      try (var t = this.database.openTransaction()) {
        final var context = t.get(DSLContext.class);
        return context.select(IMAGE_BLOBS.IMAGE_BLOB_DATA)
          .from(IMAGE_BLOBS)
          .join(IMAGES)
          .on(IMAGES.IMAGE_BLOB.eq(IMAGE_BLOBS.IMAGE_BLOB_ID))
          .where(IMAGES.IMAGE_ID.eq(id.value()))
          .fetchOptional()
          .map(rec -> new ByteArrayInputStream(rec.get(IMAGE_BLOBS.IMAGE_BLOB_DATA)));
      } catch (final Throwable e) {
        throw this.handleThrowable(e);
      }
    } finally {
      this.commandLock.unlock();
    }
  }

  private void executeRedo()
    throws Exception
  {
    this.commandLock.lock();

    try {
      this.attributes.clear();

      try (var t = this.database.openTransaction()) {
        final var oldCommandRecOpt = dbRedoGetTip(t);
        if (oldCommandRecOpt.isEmpty()) {
          return;
        }

        final var oldCommandRec =
          oldCommandRecOpt.get();
        final var oldCommand =
          parseRedoCommandFromProperties(oldCommandRec);

        oldCommand.redo(this, t);
        dbRedoMoveToUndo(t, oldCommandRec);
        t.commit();

        this.undo.set(Optional.of(oldCommand));

        final var newCommandRecOpt = dbRedoGetTip(t);
        if (newCommandRecOpt.isPresent()) {
          this.redo.set(Optional.of(
            parseRedoCommandFromProperties(newCommandRecOpt.get()))
          );
        } else {
          this.redo.set(Optional.empty());
        }
      } catch (final Throwable e) {
        throw this.handleThrowable(e);
      }
    } finally {
      this.commandLock.unlock();
    }
  }

  void setImagesAll(
    final List<LImageWithID> images)
  {
    this.imagesAll.set(Objects.requireNonNull(images, "images"));
  }

  Map<String, String> attributes()
  {
    return Map.copyOf(this.attributes);
  }

  void setImageCaptionsAssigned(
    final List<LCaption> captions)
  {
    this.imageCaptionsAssigned.set(captions);
  }

  void setImageSelected(
    final Optional<LImageWithID> image)
  {
    this.imageSelected.set(image);
  }

  void clearUndo()
  {
    this.undo.set(Optional.empty());
    this.redo.set(Optional.empty());
  }

  void event(
    final LFileModelEvent event)
  {
    this.events.submit(event);
  }

  void eventWithoutProgress(
    final String format,
    final Object... arguments)
  {
    this.event(new LFileModelEvent(
      String.format(format, arguments),
      OptionalDouble.empty()
    ));
  }

  void eventWithProgressCurrentMax(
    final int current,
    final int max,
    final String format,
    final Object... arguments)
  {
    this.event(new LFileModelEvent(
      String.format(format, arguments),
      OptionalDouble.of(
        (double) current / (double) max
      )
    ));
  }

  void setCategoriesAndCaptions(
    final List<LCaption> newCaptionsAll,
    final List<LCategory> newCategoriesAll,
    final List<LCategory> newCategoriesRequired,
    final SortedMap<LCategoryID, List<LCaption>> newCategoryCaptions)
  {
    this.tagsAll.set(newCaptionsAll);
    this.categoriesAll.set(newCategoriesAll);
    this.categoriesRequired.set(newCategoriesRequired);
    this.categoryCaptions.set(newCategoryCaptions);
  }

  void setCategorySelected(
    final Optional<LCategory> category)
  {
    this.categorySelected.set(category);
  }

  void setCategoryCaptionsAssigned(
    final List<LCaption> captions)
  {
    this.categoryCaptionsAssigned.set(captions);
  }

  void setMetadata(
    final List<LMetadataValue> meta)
  {
    this.metadata.set(meta);
  }
}
