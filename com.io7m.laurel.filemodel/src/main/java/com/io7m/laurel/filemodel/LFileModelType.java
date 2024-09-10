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
import com.io7m.laurel.filemodel.internal.LCategoryAndTags;
import com.io7m.laurel.model.LCategory;
import com.io7m.laurel.model.LException;
import com.io7m.laurel.model.LImage;
import com.io7m.laurel.model.LTag;

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
    Set<LCategory> categories);

  /**
   * Set categories as not required.
   *
   * @param categories The categories
   *
   * @return The operation in progress
   */

  CompletableFuture<?> categorySetNotRequired(
    Set<LCategory> categories);

  /**
   * Add a category.
   *
   * @param text The category
   *
   * @return The operation in progress
   */

  CompletableFuture<?> categoryAdd(
    LCategory text);

  /**
   * Add a tag.
   *
   * @param text The tag
   *
   * @return The operation in progress
   */

  CompletableFuture<?> tagAdd(
    LTag text);

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
   * Select an image.
   *
   * @param name The name
   *
   * @return The operation in progress
   */

  CompletableFuture<?> imageSelect(
    Optional<String> name);

  /**
   * Assign the given tags to the given categories.
   *
   * @param categories The categories/tags
   *
   * @return The operation in progress
   */

  CompletableFuture<?> categoryTagsAssign(
    List<LCategoryAndTags> categories);

  /**
   * Unassign the given tags from the given categories.
   *
   * @param categories The categories/tags
   *
   * @return The operation in progress
   */

  CompletableFuture<?> categoryTagsUnassign(
    List<LCategoryAndTags> categories);

  @Override
  void close()
    throws LException;

  /**
   * @return The currently selected image
   */

  AttributeReadableType<Optional<LImage>> imageSelected();

  /**
   * @return The current complete list of images
   */

  AttributeReadableType<List<LImage>> imageList();

  /**
   * @return The current complete list of required tag categories
   */

  AttributeReadableType<List<LCategory>> categoriesRequired();

  /**
   * @return The current complete list of tags
   */

  AttributeReadableType<List<LTag>> tagList();

  /**
   * @return The list of tags assigned to the current image
   */

  AttributeReadableType<List<LTag>> tagsAssigned();

  /**
   * @return Text describing the top of the undo stack, if any
   */

  AttributeReadableType<Optional<String>> undoText();

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
   * @return The tags for every available category
   */

  AttributeReadableType<SortedMap<LCategory, List<LTag>>> categoryTags();
}
