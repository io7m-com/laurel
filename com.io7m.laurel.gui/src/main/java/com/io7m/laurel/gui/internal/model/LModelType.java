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


package com.io7m.laurel.gui.internal.model;

import com.io7m.laurel.model.LImageCaptionID;
import com.io7m.laurel.model.LImageID;
import com.io7m.laurel.model.LImageSet;
import javafx.beans.property.ReadOnlyProperty;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.SortedList;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The interface exposed by the model.
 */

public interface LModelType
{
  /**
   * @return The current file status
   */

  ReadOnlyProperty<LModelFileStatusType> fileStatus();

  /**
   * @return The current images
   */

  SortedList<LMImage> imagesView();

  /**
   * @return An immutable image set based on the current model state
   */

  LImageSet createImageSet();

  /**
   * Replace the state of the model with the given image set.
   *
   * @param path        The file
   * @param newImageSet The new image set
   *
   * @throws LModelOpException On errors
   */

  void replaceWith(
    Path path,
    LImageSet newImageSet)
    throws LModelOpException;

  /**
   * Set the image selection.
   *
   * @param imageOpt The image
   *
   * @return The file for the image
   */

  Optional<Path> imageSelect(
    Optional<LImageID> imageOpt);

  /**
   * @return The captions
   */

  ObservableMap<LImageCaptionID, LMCaption> captions();

  /**
   * @return The images
   */

  ObservableMap<LImageID, LMImage> images();

  /**
   * @return The selected image
   */

  ReadOnlyProperty<LMImage> imageSelected();

  /**
   * Clear the model.
   */

  void clear();

  /**
   * @return The currently assigned captions for the selected image
   */

  SortedList<LMCaption> captionsAssigned();

  /**
   * @return The currently unassigned captions for the selected image
   */

  SortedList<LMCaption> captionsUnassigned();

  /**
   * Set the unassigned caption filter.
   *
   * @param text The text
   */

  void captionsUnassignedSetFilter(
    String text);

  /**
   * Set the images caption filter.
   *
   * @param text The text
   */

  void imagesSetFilter(String text);
}
