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
import com.io7m.laurel.filemodel.LFileModelEvent;
import com.io7m.laurel.filemodel.LFileModelType;
import com.io7m.laurel.model.LException;
import com.io7m.laurel.model.LImage;
import com.io7m.laurel.model.LTag;
import com.io7m.seltzer.api.SStructuredErrorType;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.locks.ReentrantLock;

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

  private final AttributeType<List<LImage>> imagesAll;
  private final AttributeType<List<LTag>> tagsAll;
  private final AttributeType<List<LTag>> tagsAssigned;
  private final AttributeType<Optional<? extends LCommandType<?>>> redo;
  private final AttributeType<Optional<? extends LCommandType<?>>> undo;
  private final AttributeType<Optional<LImage>> imageSelected;
  private final AttributeType<Optional<String>> redoText;
  private final AttributeType<Optional<String>> undoText;
  private final ConcurrentHashMap<String, String> attributes;
  private final LDatabaseType database;
  private final ReentrantLock commandLock;
  private final CloseableCollectionType<LException> resources;
  private final SubmissionPublisher<LFileModelEvent> events;

  private LFileModel(
    final LDatabaseType inDatabase)
  {
    this.database =
      Objects.requireNonNull(inDatabase, "database");
    this.tagsAll =
      ATTRIBUTES.create(List.of());
    this.tagsAssigned =
      ATTRIBUTES.create(List.of());
    this.imagesAll =
      ATTRIBUTES.create(List.of());
    this.imageSelected =
      ATTRIBUTES.create(Optional.empty());
    this.undo =
      ATTRIBUTES.create(Optional.empty());
    this.undoText =
      this.undo.map(o -> o.map(LCommandType::describe));
    this.redo =
      ATTRIBUTES.create(Optional.empty());
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
    this.events =
      this.resources.add(new SubmissionPublisher<>());
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
  public CompletableFuture<?> tagAdd(
    final LTag text)
  {
    return this.runCommand(new LCommandTagAdd(), text);
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
      new LCommandImageAdd(),
      new LImageRequest(name, file, source)
    );
  }

  @Override
  public CompletableFuture<?> imageSelect(
    final Optional<String> name)
  {
    Objects.requireNonNull(name, "name");

    return this.runCommand(
      new LCommandImageSelect(),
      name
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
  public AttributeReadableType<Optional<LImage>> imageSelected()
  {
    return this.imageSelected;
  }

  @Override
  public AttributeReadableType<List<LImage>> imageList()
  {
    return this.imagesAll;
  }

  @Override
  public AttributeReadableType<List<LTag>> tagList()
  {
    return this.tagsAll;
  }

  @Override
  public AttributeReadableType<List<LTag>> tagsAssigned()
  {
    return this.tagsAssigned;
  }

  @Override
  public AttributeReadableType<Optional<String>> undoText()
  {
    return this.undoText;
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
    final List<LImage> images)
  {
    this.imagesAll.set(Objects.requireNonNull(images, "images"));
  }

  void setTagsAll(
    final List<LTag> tags)
  {
    this.tagsAll.set(Objects.requireNonNull(tags, "tags"));
  }

  Map<String, String> attributes()
  {
    return Map.copyOf(this.attributes);
  }

  void setTagsAssigned(
    final List<LTag> tags)
  {
    this.tagsAssigned.set(tags);
  }

  void setImageSelected(
    final Optional<LImage> image)
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
}
