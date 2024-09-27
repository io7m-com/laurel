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
import com.io7m.laurel.model.LCaption;
import com.io7m.laurel.model.LCaptionID;
import com.io7m.laurel.model.LCaptionName;
import com.io7m.laurel.model.LCategory;
import com.io7m.laurel.model.LCategoryID;
import com.io7m.laurel.model.LCategoryName;
import com.io7m.laurel.model.LCommandRecord;
import com.io7m.laurel.model.LException;
import com.io7m.laurel.model.LGlobalCaption;
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

  Flow.Publisher<LFileModelEventType> events();

  /**
   * Add a global caption.
   *
   * @param text The caption
   *
   * @return The operation in progress
   */

  CompletableFuture<?> globalCaptionAdd(
    LCaptionName text);

  /**
   * Remove a global caption.
   *
   * @param id The caption
   *
   * @return The operation in progress
   */

  CompletableFuture<?> globalCaptionRemove(
    LCaptionID id);

  /**
   * Lower the order of the given caption (making it higher priority).
   *
   * @param id The caption
   *
   * @return The operation in progress
   */

  CompletableFuture<?> globalCaptionOrderLower(
    LCaptionID id);

  /**
   * Increase the order of the given caption (making it lower priority).
   *
   * @param id The caption
   *
   * @return The operation in progress
   */

  CompletableFuture<?> globalCaptionOrderUpper(
    LCaptionID id);

  /**
   * Change the text for a global caption.
   *
   * @param id      The caption
   * @param newName The new text
   *
   * @return The operation in progress
   */

  CompletableFuture<?> globalCaptionModify(
    LCaptionID id,
    LCaptionName newName);

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
   * Modify a caption.
   *
   * @param id   The caption ID
   * @param name The caption text
   *
   * @return The operation in progress
   */

  CompletableFuture<?> captionModify(
    LCaptionID id,
    LCaptionName name);

  /**
   * Delete captions.
   *
   * @param captions The captions
   *
   * @return The operation in progress
   */

  CompletableFuture<?> captionRemove(
    Set<LCaptionID> captions);

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
   * Compare captions on images.
   *
   * @param imageA The left image
   * @param imageB The right image
   *
   * @return The operation in progress
   */

  CompletableFuture<?> imagesCompare(
    LImageID imageA,
    LImageID imageB);

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
   * Set the filter for the image list.
   *
   * @param filter The filter
   */

  void imageListFilterSet(String filter);

  /**
   * @return The current complete list of images with a search filter applied
   */

  AttributeReadableType<List<LImageWithID>> imageListFiltered();

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
   * @return The filtered list of captions not assigned to the current image
   */

  AttributeReadableType<List<LCaption>> imageCaptionsUnassignedFiltered();

  /**
   * Set the filter for the caption list.
   *
   * @param filter The filter
   */

  void captionsUnassignedListFilterSet(String filter);

  /**
   * @return Text describing the top of the undo stack, if any
   */

  AttributeReadableType<Optional<String>> undoText();

  /**
   * @return The undo stack
   */

  AttributeReadableType<List<LCommandRecord>> undoStack();

  /**
   * @return The redo stack
   */

  AttributeReadableType<List<LCommandRecord>> redoStack();

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

  /**
   * @return The current complete list of global captions
   */

  AttributeReadableType<List<LGlobalCaption>> globalCaptionList();

  /**
   * @return The list of captions that are on image A, but not on image B
   */

  AttributeReadableType<List<LCaption>> imageComparisonA();

  /**
   * @return The list of captions that are on image B, but not on image A
   */

  AttributeReadableType<List<LCaption>> imageComparisonB();

  /**
   * @return The current image comparison
   */

  AttributeReadableType<Optional<LImageComparison>> imageComparison();

  /**
   * @return The current caption clipboard
   */

  AttributeReadableType<Set<LCaptionID>> captionClipboard();

  /**
   * Move a set of captions to the clipboard.
   *
   * @param captions The captions
   */

  void captionsCopy(Set<LCaptionID> captions);

  /**
   * Assign the captions in the clipboard to each of the given images,
   * and then clear the clipboard.
   *
   * @param images The images
   *
   * @return The operation in progress
   */

  CompletableFuture<?> captionsPaste(
    Set<LImageID> images);
}
