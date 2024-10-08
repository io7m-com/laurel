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

import com.io7m.darco.api.DDatabaseCreate;
import com.io7m.darco.api.DDatabaseTelemetryType;
import com.io7m.darco.api.DDatabaseUpgrade;
import com.io7m.darco.sqlite.DSDatabaseConfigurationType;

import java.nio.file.Path;
import java.util.Objects;

/**
 * The configuration information for the laurel SQLite database.
 *
 * @param telemetry The telemetry interface
 * @param create    The database creation option
 * @param upgrade   The database upgrade option
 * @param file      The database file
 * @param readOnly  If the database should be read-only
 */

public record LDatabaseConfiguration(
  DDatabaseTelemetryType telemetry,
  DDatabaseCreate create,
  DDatabaseUpgrade upgrade,
  Path file,
  boolean readOnly)
  implements DSDatabaseConfigurationType
{
  /**
   * The configuration information for the laurel SQLite database.
   *
   * @param telemetry The telemetry interface
   * @param create    The database creation option
   * @param upgrade   The database upgrade option
   * @param file      The database file
   * @param readOnly  If the database should be read-only
   */

  public LDatabaseConfiguration
  {
    Objects.requireNonNull(telemetry, "telemetry");
    Objects.requireNonNull(create, "create");
    Objects.requireNonNull(upgrade, "upgrade");
    Objects.requireNonNull(file, "file");
  }
}
