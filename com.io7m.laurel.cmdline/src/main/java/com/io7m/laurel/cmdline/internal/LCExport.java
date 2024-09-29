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

import com.io7m.laurel.filemodel.LExportRequest;
import com.io7m.laurel.filemodel.LFileModelEvent;
import com.io7m.laurel.filemodel.LFileModelEventError;
import com.io7m.laurel.filemodel.LFileModelEventType;
import com.io7m.laurel.filemodel.LFileModelStatusIdle;
import com.io7m.laurel.filemodel.LFileModelStatusLoading;
import com.io7m.laurel.filemodel.LFileModels;
import com.io7m.laurel.model.LException;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * "export"
 */

public final class LCExport implements QCommandType,
  Flow.Subscriber<LFileModelEventType>
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LCExport.class);

  private static final QParameterNamed1<Path> INPUT_FILE =
    new QParameterNamed1<>(
      "--input-file",
      List.of(),
      new QStringType.QConstant("The input file."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<Path> OUTPUT_DIRECTORY =
    new QParameterNamed1<>(
      "--output-directory",
      List.of(),
      new QStringType.QConstant("The output directory."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<Boolean> EXPORT_IMAGES =
    new QParameterNamed1<>(
      "--export-images",
      List.of(),
      new QStringType.QConstant("Whether to export images."),
      Optional.of(Boolean.TRUE),
      Boolean.class
    );

  private final QCommandMetadata metadata;
  private final AtomicBoolean failed;
  private QCommandContextType context;

  /**
   * Construct a command.
   */

  public LCExport()
  {
    this.metadata = new QCommandMetadata(
      "export",
      new QStringType.QConstant("Export a dataset into a directory."),
      Optional.empty()
    );

    this.failed = new AtomicBoolean(false);
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return Stream.concat(
      Stream.of(INPUT_FILE, OUTPUT_DIRECTORY, EXPORT_IMAGES),
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

    final var inputFile =
      this.context.parameterValue(INPUT_FILE);
    final var outputDirectory =
      this.context.parameterValue(OUTPUT_DIRECTORY);

    try {
      try (var model = LFileModels.open(inputFile, true)) {
        model.events().subscribe(this);

        LOG.info("Waiting for dataset to finish loading...");
        final var loadLatch = new CountDownLatch(1);
        model.status().subscribe((oldValue, newValue) -> {
          if (oldValue instanceof LFileModelStatusLoading
              && newValue instanceof LFileModelStatusIdle) {
            loadLatch.countDown();
          }
        });
        loadLatch.await();

        LOG.info("Exporting dataset...");
        model.export(new LExportRequest(
          outputDirectory,
          this.context.<Boolean>parameterValue(EXPORT_IMAGES)
            .booleanValue()
        )).get();

        LOG.info("Export completed.");
        return QCommandStatus.SUCCESS;
      }
    } catch (final LException e) {
      logStructuredError(e);
    } catch (final InterruptedException e) {
      LOG.info("Interrupted");
    } catch (final ExecutionException e) {
      final var cause = e.getCause();
      if (cause instanceof final SStructuredErrorType<?> s) {
        logStructuredError(s);
      } else {
        LOG.error("Exception: ", e);
      }
    }
    return QCommandStatus.FAILURE;
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
