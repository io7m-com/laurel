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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * An internal command.
 *
 * @param <P> The type of parameters
 */

public interface LCommandType<P>
{
  /**
   * Execute the command.
   *
   * @param model       The model
   * @param transaction The database transaction
   * @param parameters  The parameters
   *
   * @return A value indicating if the command can be undone
   *
   * @throws LException         On errors
   * @throws DDatabaseException On errors
   */

  LCommandUndoable execute(
    LFileModel model,
    LDatabaseTransactionType transaction,
    P parameters)
    throws LException, DDatabaseException;

  /**
   * Undo the command.
   *
   * @param model       The model
   * @param transaction The database transaction
   *
   * @throws LException         On errors
   * @throws DDatabaseException On errors
   */

  void undo(
    LFileModel model,
    LDatabaseTransactionType transaction)
    throws LException, DDatabaseException;

  /**
   * Redo the command.
   *
   * @param model       The model
   * @param transaction The database transaction
   *
   * @throws LException         On errors
   * @throws DDatabaseException On errors
   */

  void redo(
    LFileModel model,
    LDatabaseTransactionType transaction)
    throws LException, DDatabaseException;

  /**
   * @return The command as a set of properties
   */

  Properties toProperties();

  /**
   * @return A humanly-readable description of the operation
   */

  String describe();

  /**
   * Serialize this command as XML properties.
   *
   * @return The serialized bytes
   */

  default byte[] serialize()
  {
    try (var out = new ByteArrayOutputStream()) {
      final var p = this.toProperties();
      p.setProperty("@Type", this.getClass().getCanonicalName());
      p.storeToXML(out, "", StandardCharsets.UTF_8);
      out.flush();
      return out.toByteArray();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
