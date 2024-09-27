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

import com.io7m.laurel.filemodel.LFileModelEvent;
import com.io7m.laurel.filemodel.LFileModelEventError;
import com.io7m.laurel.filemodel.LFileModelEventType;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * An event cell controller.
 */

public final class LEventCellController implements LViewType
{
  @FXML private ImageView eventErrorIcon;
  @FXML private Label eventText;

  /**
   * An event cell controller.
   */

  public LEventCellController()
  {

  }

  /**
   * Unset an item.
   */

  public void unsetItem()
  {
    this.eventErrorIcon.setVisible(false);
    this.eventText.setText("");
  }

  /**
   * Set an item.
   *
   * @param item The event
   */

  public void setItem(
    final LFileModelEventType item)
  {
    this.eventText.setText(item.message());

    switch (item) {
      case final LFileModelEvent ignored -> {
        this.eventErrorIcon.setVisible(false);
      }
      case final LFileModelEventError ignored -> {
        this.eventErrorIcon.setVisible(true);
      }
    }
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {

  }
}
