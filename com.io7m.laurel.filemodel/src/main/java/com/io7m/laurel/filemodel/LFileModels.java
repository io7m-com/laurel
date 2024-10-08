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

import com.io7m.laurel.filemodel.internal.LFileModel;
import com.io7m.laurel.filemodel.internal.LFileModelImport;
import com.io7m.laurel.model.LException;

import java.nio.file.Path;

/**
 * The file models.
 */

public final class LFileModels
{
  private LFileModels()
  {

  }

  /**
   * Open a file model.
   *
   * @param file     The file
   * @param readOnly {@code true} if the file should be read-only
   *
   * @return A file model
   *
   * @throws LException On errors
   */

  public static LFileModelType open(
    final Path file,
    final boolean readOnly)
    throws LException
  {
    return LFileModel.open(file, readOnly);
  }

  /**
   * Create a file model import operation.
   *
   * @param directory  The import directory
   * @param outputFile The output file
   *
   * @return An import operation
   */

  public static LFileModelImportType createImport(
    final Path directory,
    final Path outputFile)
  {
    return LFileModelImport.create(directory, outputFile);
  }
}
