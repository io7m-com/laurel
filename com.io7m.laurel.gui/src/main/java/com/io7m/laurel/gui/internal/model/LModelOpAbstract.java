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

import java.util.Objects;

abstract class LModelOpAbstract implements LModelOpType
{
  private boolean executed;

  LModelOpAbstract()
  {
    this.executed = false;
  }

  protected abstract void onExecute()
    throws LModelOpException;

  protected abstract void onUndo()
    throws LModelOpException;

  @Override
  public final void execute()
    throws LModelOpException
  {
    try {
      this.onExecute();
      this.executed = true;
    } catch (final LModelOpException e) {
      this.executed = false;
      throw e;
    } catch (final Exception e) {
      this.executed = false;
      throw new LModelOpException(
        Objects.requireNonNullElse(
          e.getMessage(), e.getClass().getCanonicalName()
        ),
        e
      );
    }
  }

  @Override
  public final void undo()
    throws LModelOpException
  {
    if (!this.executed) {
      throw new LModelOpException(
        "Cannot undo a command that hasn't executed.");
    }

    this.onUndo();
    this.executed = false;
  }
}
