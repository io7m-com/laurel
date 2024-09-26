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


package com.io7m.laurel.filemodel.internal;

import com.io7m.darco.api.DDatabaseException;
import com.io7m.laurel.model.LException;

import java.util.Objects;

/**
 * The abstract base of commands.
 *
 * @param <T> The type of parameters
 */

public abstract class LCommandAbstract<T>
  implements LCommandType<T>
{
  private boolean executed;

  /**
   * @return {@code true} if this command has ever successfully executed
   */

  public final boolean isExecuted()
  {
    return this.executed;
  }

  /**
   * Set the command as executed.
   *
   * @param e The state
   */

  public final void setExecuted(
    final boolean e)
  {
    this.executed = e;
  }

  protected abstract LCommandUndoable onExecute(
    LFileModel model,
    LDatabaseTransactionType transaction,
    T parameters)
    throws LException, DDatabaseException;

  protected abstract void onRedo(
    LFileModel model,
    LDatabaseTransactionType transaction)
    throws LException, DDatabaseException;

  protected abstract void onUndo(
    LFileModel model,
    LDatabaseTransactionType transaction)
    throws LException, DDatabaseException;

  @Override
  public final LCommandUndoable execute(
    final LFileModel model,
    final LDatabaseTransactionType transaction,
    final T parameters)
    throws LException, DDatabaseException
  {
    Objects.requireNonNull(model, "model");
    Objects.requireNonNull(transaction, "transaction");
    Objects.requireNonNull(parameters, "parameters");

    final var r = this.onExecute(model, transaction, parameters);
    this.setExecuted(true);
    return r;
  }

  @Override
  public final void redo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
    throws LException, DDatabaseException
  {
    Objects.requireNonNull(model, "model");
    Objects.requireNonNull(transaction, "transaction");

    if (!this.isExecuted()) {
      throw new IllegalStateException(
        "Cannot redo a command that has not executed.");
    }

    this.onRedo(model, transaction);
  }

  @Override
  public final void undo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
    throws LException, DDatabaseException
  {
    Objects.requireNonNull(model, "model");
    Objects.requireNonNull(transaction, "transaction");

    if (!this.isExecuted()) {
      throw new IllegalStateException(
        "Cannot undo a command that has not executed.");
    }

    this.onUndo(model, transaction);
  }
}
