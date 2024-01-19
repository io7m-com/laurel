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

import com.io7m.jattribute.core.AttributeReadableType;
import com.io7m.laurel.io.LExportRequest;
import com.io7m.laurel.model.LImage;
import com.io7m.laurel.model.LImageCaption;
import com.io7m.laurel.model.LImageCaptionID;
import com.io7m.laurel.model.LImageID;
import com.io7m.repetoir.core.RPServiceType;
import com.io7m.seltzer.api.SStructuredErrorType;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

/**
 * The main controller.
 */

public interface LControllerType
  extends RPServiceType, AutoCloseable
{
  /**
   * @return The observable list of images
   */

  SortedList<LImage> imageListReadable();

  /**
   * @return The assigned captions for the current image
   */

  ObservableList<LImageCaption> captionListAssigned();

  /**
   * @return The available unassigned captions for the current image
   */

  SortedList<LImageCaption> captionListAvailable();

  /**
   * @return The error stream
   */

  Flow.Publisher<SStructuredErrorType<String>> errors();

  /**
   * @return The undo state
   */

  AttributeReadableType<LUndoState> undoState();

  /**
   * @return The busy state
   */

  AttributeReadableType<Boolean> busy();

  /**
   * @return The image set state
   */

  AttributeReadableType<LImageSetStateType> imageSetState();

  /**
   * @return {@code true} if the application is busy
   */

  boolean isBusy();

  /**
   * @return {@code true} if the application is saved
   */

  boolean isSaved();

  /**
   * Save the image set to the given file (and use this file from now on)
   *
   * @param path The file
   *
   * @return The operation in progress
   */

  CompletableFuture<Object> save(
    Path path);

  /**
   * Save the image set to the current file.
   *
   * @return The operation in progress
   */

  CompletableFuture<Object> save();

  /**
   * Open an image set from the given file.
   *
   * @param path The file
   *
   * @return The operation in progress
   */

  CompletableFuture<Object> open(
    Path path);

  /**
   * Start a new image set using the given file.
   *
   * @param file The file
   *
   * @return The operation in progress
   */

  CompletableFuture<Object> newSet(
    Path file);

  /**
   * Select an image (or deselect the current one).
   *
   * @param imageOpt The image to select
   *
   * @return The current image file
   */

  Optional<Path> imageSelect(
    Optional<LImageID> imageOpt);

  /**
   * Undo the most recent operation.
   */

  void undo();

  /**
   * Redo the most recent operation.
   */

  void redo();

  /**
   * Add images to the current set.
   *
   * @param files The image files
   *
   * @return The operation in progress
   */

  CompletableFuture<Object> imagesAdd(
    List<Path> files);

  /**
   * Create a new caption.
   *
   * @param text The text
   */

  void captionNew(
    String text);

  /**
   * Unassign captions from an image.
   *
   * @param image    The image
   * @param captions The captions
   */

  void imageCaptionUnassign(
    LImageID image,
    List<LImageCaptionID> captions);

  /**
   * Assign captions to an image.
   *
   * @param image    The image
   * @param captions The captions
   */

  void imageCaptionAssign(
    LImageID image,
    List<LImageCaptionID> captions);

  /**
   * Delete captions.
   *
   * @param captions The captions
   */

  void captionRemove(
    List<LImageCaption> captions);

  /**
   * Increase the priority of a caption on an image.
   *
   * @param imageID   The image
   * @param captionID The caption
   */

  void imageCaptionPriorityIncrease(
    LImageID imageID,
    LImageCaptionID captionID);

  /**
   * Decreate the priority of a caption on an image.
   *
   * @param imageID   The image
   * @param captionID The caption
   */

  void imageCaptionPriorityDecrease(
    LImageID imageID,
    LImageCaptionID captionID);

  /**
   * Count the number of times a caption is used.
   *
   * @param captionID The caption
   *
   * @return The number of occurrences
   */

  long captionCount(
    LImageCaptionID captionID);

  /**
   * Close the open image set.
   */

  void closeSet();

  /**
   * Export captions.
   *
   * @param request The request
   *
   * @return The operation in progress
   */

  CompletableFuture<Object> export(
    LExportRequest request);
}
