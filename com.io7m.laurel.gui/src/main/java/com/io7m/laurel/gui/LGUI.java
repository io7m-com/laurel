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


package com.io7m.laurel.gui;

import com.io7m.laurel.gui.internal.LApplication;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * The UI.
 */

public final class LGUI
{
  private final LApplication app;

  private LGUI(
    final LApplication inApp)
  {
    this.app = Objects.requireNonNull(inApp, "app");
  }

  /**
   * Start a new UI.
   *
   * @param configuration The UI configuration
   *
   * @return A new UI
   *
   * @throws Exception On startup failures
   */

  public static LGUI start(
    final LConfiguration configuration)
    throws Exception
  {
    final var stage = new Stage();
    stage.setMinWidth(960.0);
    stage.setMinHeight(540.0);
    stage.setWidth(960.0);
    stage.setHeight(540.0);

    final var app = new LApplication(configuration);
    app.start(stage);
    return new LGUI(app);
  }
}
