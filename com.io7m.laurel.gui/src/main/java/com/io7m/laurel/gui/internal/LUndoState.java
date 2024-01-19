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

import com.io7m.laurel.model.LImageSetCommandType;

import java.util.LinkedList;
import java.util.List;

/**
 * The undo state.
 *
 * @param undoStack The undo stack
 * @param redoStack The redo stack
 */

public record LUndoState(
  List<LImageSetCommandType> undoStack,
  List<LImageSetCommandType> redoStack)
{
  /**
   * The undo state.
   *
   * @param undoStack The undo stack
   * @param redoStack The redo stack
   */

  public LUndoState
  {
    undoStack = List.copyOf(undoStack);
    redoStack = List.copyOf(redoStack);
  }

  /**
   * @return An empty undo state
   */

  public static LUndoState empty()
  {
    return new LUndoState(List.of(), List.of());
  }

  /**
   * Add a new command to the undo state.
   *
   * @param command The command
   *
   * @return The new state
   */

  public LUndoState add(
    final LImageSetCommandType command)
  {
    final var undoStackNew =
      new LinkedList<>(this.undoStack);

    undoStackNew.addFirst(command);
    return new LUndoState(
      undoStackNew,
      this.redoStack
    );
  }
}
