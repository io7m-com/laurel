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


package com.io7m.laurel.model;

import java.util.List;

/**
 * The image set.
 */

public interface LImageSetType
  extends LImageSetReadableType, AutoCloseable
{
  @Override
  void close();

  /**
   * Create or update an image caption.
   *
   * @param caption The caption
   *
   * @return The operation
   */

  LImageSetCommandType captionUpdate(
    LImageCaption caption);

  /**
   * Remove a caption.
   *
   * @param caption The caption ID
   *
   * @return The operation
   */

  LImageSetCommandType captionRemove(
    LImageCaptionID caption);

  /**
   * Create or update an image.
   *
   * @param image The image
   *
   * @return The operation
   */

  LImageSetCommandType imageUpdate(
    LImage image);

  /**
   * Compose several commands into a single command.
   *
   * @param description The command description
   * @param commands    The commands
   *
   * @return A composite command
   */

  LImageSetCommandType compose(
    String description,
    List<LImageSetCommandType> commands);

  /**
   * Compose several commands into a single command.
   *
   * @param description The command description
   * @param commands    The commands
   *
   * @return A composite command
   */

  default LImageSetCommandType compose(
    final String description,
    final LImageSetCommandType... commands)
  {
    return this.compose(description, List.of(commands));
  }

  /**
   * Assign a caption to an image.
   *
   * @param image   The image
   * @param caption The caption
   *
   * @return The operation
   */

  LImageSetCommandType captionAssign(
    LImageID image,
    LImageCaptionID caption);

  /**
   * Unassign a caption from an image.
   *
   * @param image   The image
   * @param caption The caption
   *
   * @return The operation
   */

  LImageSetCommandType captionUnassign(
    LImageID image,
    LImageCaptionID caption);

  /**
   * Increase the priority of a caption on an image.
   *
   * @param image   The image
   * @param caption The caption
   *
   * @return The operation
   */

  LImageSetCommandType captionPriorityIncrease(
    LImageID image,
    LImageCaptionID caption);

  /**
   * Decrease the priority of a caption on an image.
   *
   * @param image   The image
   * @param caption The caption
   *
   * @return The operation
   */

  LImageSetCommandType captionPriorityDecrease(
    LImageID image,
    LImageCaptionID caption);
}
