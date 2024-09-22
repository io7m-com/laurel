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


package com.io7m.laurel.filemodel;

import com.io7m.jattribute.core.AttributeReadableType;
import com.io7m.laurel.model.LCaptionName;
import com.io7m.laurel.model.LCaption;
import com.io7m.laurel.model.LCategory;
import com.io7m.laurel.model.LCategoryID;
import com.io7m.laurel.model.LCategoryName;
import com.io7m.laurel.model.LException;
import com.io7m.laurel.model.LImageID;
import com.io7m.laurel.model.LImageWithID;
import com.io7m.laurel.model.LMetadataValue;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

/**
 * The interface to a file model.
 */

public interface LFileModelType
  extends AutoCloseable
{
  /**
   * @return The file model events
   */

  Flow.Publisher<LFileModelEvent> events();

  /**
   * Set categories as required.
   *
   * @param categories The categories
   *
   * @return The operation in progress
   */

  CompletableFuture<?> categorySetRequired(
    Set<LCategoryID> categories);

  /**
   * Set categories as not required.
   *
   * @param categories The categories
   *
   * @return The operation in progress
   */

  CompletableFuture<?> categorySetNotRequired(
    Set<LCategoryID> categories);

  /**
   * Add a category.
   *
   * @param text The category
   *
   * @return The operation in progress
   */

  CompletableFuture<?> categoryAdd(
    LCategoryName text);

  /**
   * Add a caption.
   *
   * @param text The caption
   *
   * @return The operation in progress
   */

  CompletableFuture<?> captionAdd(
    LCaptionName text);

  /**
   * Load an image and add it to the file.
   *
   * @param name   The (unique) name
   * @param file   The file
   * @param source The URI source, if any
   *
   * @return The operation in progress
   */

  CompletableFuture<?> imageAdd(
    String name,
    Path file,
    Optional<URI> source
  );

  /**
   * Delete images.
   *
   * @param ids The ids
   *
   * @return The operation in progress
   */

  CompletableFuture<?> imagesDelete(
    List<LImageID> ids
  );

  /**
   * Assign captions to an image.
   *
   * @param assignments The assignments
   *
   * @return The operation in progress
   */

  CompletableFuture<?> imageCaptionsAssign(
    List<LImageCaptionsAssignment> assignments);

  /**
   * Unassign captions from an image.
   *
   * @param assignments The assignments
   *
   * @return The operation in progress
   */

  CompletableFuture<?> imageCaptionsUnassign(
    List<LImageCaptionsAssignment> assignments);

  /**
   * Select an image.
   *
   * @param name The name
   *
   * @return The operation in progress
   */

  CompletableFuture<?> imageSelect(
    Optional<LImageID> name);

  /**
   * Assign the given captions to the given categories.
   *
   * @param categories The categories/captions
   *
   * @return The operation in progress
   */

  CompletableFuture<?> categoryCaptionsAssign(
    List<LCategoryCaptionsAssignment> categories);

  /**
   * Unassign the given captions from the given categories.
   *
   * @param categories The categories/captions
   *
   * @return The operation in progress
   */

  CompletableFuture<?> categoryCaptionsUnassign(
    List<LCategoryCaptionsAssignment> categories);

  /**
   * Select a category.
   *
   * @param id The id
   *
   * @return The operation in progress
   */

  CompletableFuture<?> categorySelect(
    Optional<LCategoryID> id);

  /**
   * Add or update metadata.
   *
   * @param metadata The metadata values
   *
   * @return The operation in progress
   */

  CompletableFuture<?> metadataPut(
    List<LMetadataValue> metadata);

  /**
   * Remove metadata.
   *
   * @param metadata The metadata values
   *
   * @return The operation in progress
   */

  CompletableFuture<?> metadataRemove(
    List<LMetadataValue> metadata);

  @Override
  void close()
    throws LException;

  /**
   * @return The current complete list of metadata
   */

  AttributeReadableType<List<LMetadataValue>> metadataList();

  /**
   * @return The currently selected category
   */

  AttributeReadableType<Optional<LCategory>> categorySelected();

  /**
   * @return The currently selected image
   */

  AttributeReadableType<Optional<LImageWithID>> imageSelected();

  /**
   * @return The current complete list of images
   */

  AttributeReadableType<List<LImageWithID>> imageList();

  /**
   * @return The current complete list of required caption categories
   */

  AttributeReadableType<List<LCategory>> categoriesRequired();

  /**
   * @return The current complete list of captions
   */

  AttributeReadableType<List<LCaption>> captionList();

  /**
   * @return The list of captions assigned to the current image
   */

  AttributeReadableType<List<LCaption>> imageCaptionsAssigned();

  /**
   * @return The list of captions not assigned to the current image
   */

  AttributeReadableType<List<LCaption>> imageCaptionsUnassigned();

  /**
   * @return Text describing the top of the undo stack, if any
   */

  AttributeReadableType<Optional<String>> undoText();

  /**
   * @return The list of captions assigned to the current category
   */

  AttributeReadableType<List<LCaption>> categoryCaptionsAssigned();

  /**
   * @return The list of captions not assigned to the current category
   */

  AttributeReadableType<List<LCaption>> categoryCaptionsUnassigned();

  /**
   * Undo the operation on the top of the undo stack
   *
   * @return The operation in progress
   */

  CompletableFuture<?> undo();

  /**
   * Redo the operation on the top of the redo stack
   *
   * @return The operation in progress
   */

  CompletableFuture<?> redo();

  /**
   * Compact the file, deleting the undo/redo log and cleaning up any
   * unused data in the file.
   *
   * @return The operation in progress
   */

  CompletableFuture<?> compact();

  /**
   * @return Text describing the top of the redo stack, if any
   */

  AttributeReadableType<Optional<String>> redoText();

  /**
   * @return The current complete list of categories
   */

  AttributeReadableType<List<LCategory>> categoryList();

  /**
   * @return The captions for every available category
   */

  AttributeReadableType<SortedMap<LCategoryID, List<LCaption>>> categoryCaptions();

  /**
   * @param id The image ID
   *
   * @return The image data stream
   */

  CompletableFuture<Optional<InputStream>> imageStream(LImageID id);
}
