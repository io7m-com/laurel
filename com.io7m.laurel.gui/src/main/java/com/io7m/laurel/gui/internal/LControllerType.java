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

import com.io7m.laurel.gui.internal.model.LMCaption;
import com.io7m.laurel.gui.internal.model.LMImage;
import com.io7m.laurel.gui.internal.model.LMUndoState;
import com.io7m.laurel.gui.internal.model.LModelFileStatusType;
import com.io7m.laurel.gui.internal.model.LModelType;
import com.io7m.laurel.io.LExportRequest;
import com.io7m.laurel.model.LImageCaptionID;
import com.io7m.laurel.model.LImageID;
import com.io7m.repetoir.core.RPServiceType;
import com.io7m.seltzer.api.SStructuredErrorType;
import javafx.beans.property.ReadOnlyProperty;
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
   * @return The undo state
   */

  ReadOnlyProperty<LMUndoState> undoState();

  /**
   * @return The model
   */

  LModelType model();

  /**
   * @return The error stream
   */

  Flow.Publisher<SStructuredErrorType<String>> errors();

  /**
   * @return The busy state
   */

  ReadOnlyProperty<Boolean> busy();

  /**
   * @return {@code true} if the application is busy
   */

  default boolean isBusy()
  {
    return this.busy().getValue().booleanValue();
  }

  /**
   * @return {@code true} if the application is saved
   */

  default boolean isSaved()
  {
    return this.model()
      .fileStatus()
      .getValue()
      .isSaved();
  }

  /**
   * Save the image set to the given file (and use this file from now on)
   *
   * @param path The file
   *
   * @return The operation in progress
   */

  CompletableFuture<?> save(
    Path path);

  /**
   * Save the image set to the current file.
   *
   * @return The operation in progress
   */

  CompletableFuture<?> save();

  /**
   * Open an image set from the given file.
   *
   * @param path The file
   *
   * @return The operation in progress
   */

  CompletableFuture<?> open(
    Path path);

  /**
   * Start a new image set using the given file.
   *
   * @param file The file
   */

  void newSet(
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

  CompletableFuture<?> imagesAdd(
    List<Path> files);

  /**
   * Create a new caption.
   *
   * @param text The text
   */

  void captionNew(
    String text);

  /**
   * Unassign captions from images.
   *
   * @param images   The images
   * @param captions The captions
   */

  void imageCaptionUnassign(
    List<LImageID> images,
    List<LImageCaptionID> captions);

  /**
   * Assign captions to images.
   *
   * @param images   The images
   * @param captions The captions
   */

  void imageCaptionAssign(
    List<LImageID> images,
    List<LImageCaptionID> captions);

  /**
   * Delete captions.
   *
   * @param captions The captions
   */

  void captionRemove(
    List<LMCaption> captions);

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

  /**
   * @return The image list
   */

  SortedList<LMImage> imageList();

  /**
   * @return The assigned captions
   */

  SortedList<LMCaption> captionsAssigned();

  /**
   * @return The unassigned captions
   */

  SortedList<LMCaption> captionsUnassigned();

  /**
   * Set the unassigned captions filter.
   *
   * @param text The filter
   */

  void captionsUnassignedSetFilter(String text);

  /**
   * @return The file status
   */

  ReadOnlyProperty<LModelFileStatusType> fileStatus();

  /**
   * Import captioned images from a directory.
   *
   * @param path The directory
   *
   * @return The operation
   */

  CompletableFuture<Object> importDirectory(Path path);

  /**
   * Set the image filter.
   *
   * @param text The filter
   */

  void imagesSetFilter(
    String text);

  /**
   * @return The global prefix captions
   */

  ObservableList<String> globalPrefixCaptions();

  /**
   * Create a new global prefix caption.
   *
   * @param text The text
   */

  void globalPrefixCaptionNew(String text);

  /**
   * Delete the caption at the given index.
   *
   * @param index The index
   */

  void globalPrefixCaptionDelete(int index);

  /**
   * Modify the caption at the given index.
   *
   * @param index The index
   * @param text  The text
   */

  void globalPrefixCaptionModify(
    int index,
    String text);

  /**
   * Delete the given images.
   *
   * @param images The images
   */

  void imagesDelete(
    List<LMImage> images);

  /**
   * @return The selected image
   */

  ReadOnlyProperty<LMImage> imageSelected();

  /**
   * @return The copied captions, if any
   */

  ObservableList<LMCaption> captionsAssignedCopied();

  /**
   * @param captions The captions to copy
   */

  void captionsAssignedCopy(List<LMCaption> captions);

  /**
   * Paste the captions.
   */

  void captionsAssignedPaste();
}
