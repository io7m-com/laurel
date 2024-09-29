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


package com.io7m.laurel.cmdline.internal;

import com.io7m.laurel.filemodel.LFileModelEvent;
import com.io7m.laurel.filemodel.LFileModelEventError;
import com.io7m.laurel.filemodel.LFileModelEventType;
import com.io7m.laurel.filemodel.LFileModels;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType;
import com.io7m.quarrel.ext.logback.QLogback;
import com.io7m.seltzer.api.SStructuredErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * "import"
 */

public final class LCImport implements QCommandType, Flow.Subscriber<LFileModelEventType>
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LCImport.class);

  private static final QParameterNamed1<Path> INPUT_DIRECTORY =
    new QParameterNamed1<>(
      "--input-directory",
      List.of(),
      new QStringType.QConstant("The input directory."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<Path> OUTPUT_FILE =
    new QParameterNamed1<>(
      "--output-file",
      List.of(),
      new QStringType.QConstant("The output file."),
      Optional.empty(),
      Path.class
    );

  private final QCommandMetadata metadata;
  private final AtomicBoolean failed;
  private QCommandContextType context;

  /**
   * Construct a command.
   */

  public LCImport()
  {
    this.metadata = new QCommandMetadata(
      "import",
      new QStringType.QConstant("Import a directory into a dataset."),
      Optional.empty()
    );

    this.failed = new AtomicBoolean(false);
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return Stream.concat(
      Stream.of(INPUT_DIRECTORY, OUTPUT_FILE),
      QLogback.parameters().stream()
    ).toList();
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType newContext)
  {
    System.setProperty("org.jooq.no-tips", "true");
    System.setProperty("org.jooq.no-logo", "true");

    this.context = newContext;
    QLogback.configure(this.context);

    final var inputDirectory =
      this.context.parameterValue(INPUT_DIRECTORY);
    final var outputFile =
      this.context.parameterValue(OUTPUT_FILE);

    try (var importer =
           LFileModels.createImport(inputDirectory, outputFile)) {
      importer.events().subscribe(this);
      importer.execute().get();
    } catch (final ExecutionException e) {
      this.failed.set(true);
      final var cause = e.getCause();
      if (cause instanceof final SStructuredErrorType<?> s) {
        logStructuredError(s);
      } else {
        LOG.error("Exception: ", e);
      }
    } catch (final InterruptedException e) {
      this.failed.set(true);
      LOG.info("Interrupted");
    }

    if (this.failed.get()) {
      return QCommandStatus.FAILURE;
    }
    return QCommandStatus.SUCCESS;
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }

  @Override
  public void onSubscribe(
    final Flow.Subscription subscription)
  {
    subscription.request(Long.MAX_VALUE);
  }

  @Override
  public void onNext(
    final LFileModelEventType item)
  {
    switch (item) {
      case final LFileModelEvent event -> {
        LOG.info("{}", event.message());
      }
      case final LFileModelEventError error -> {
        this.failed.set(true);
        logStructuredError(error);
      }
    }
  }

  private static void logStructuredError(
    final SStructuredErrorType<?> error)
  {
    LOG.error("{}: {}", error.errorCode(), error.message());
    for (final var entry : error.attributes().entrySet()) {
      LOG.error("  {}: {}", entry.getKey(), entry.getValue());
    }
    error.exception()
      .ifPresent(throwable -> LOG.error("  Exception: ", throwable));
  }

  @Override
  public void onError(
    final Throwable throwable)
  {
    LOG.error("Exception: ", throwable);
    this.failed.set(true);
  }

  @Override
  public void onComplete()
  {

  }
}
