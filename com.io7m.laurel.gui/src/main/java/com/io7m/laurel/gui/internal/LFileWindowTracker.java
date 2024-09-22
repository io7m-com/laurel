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

import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Objects;

/**
 * A window tracker.
 */

public final class LFileWindowTracker
  implements LFileWindowTrackerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LFileWindowTracker.class);

  private final HashSet<Stage> stages;

  private LFileWindowTracker()
  {
    this.stages = new HashSet<>();
  }

  /**
   * Create a new window tracker.
   *
   * @return A tracker
   */

  public static LFileWindowTrackerType create()
  {
    return new LFileWindowTracker();
  }

  @Override
  public String description()
  {
    return "File window tracking service.";
  }

  @Override
  public void register(
    final Stage stage)
  {
    this.stages.add(Objects.requireNonNull(stage, "stage"));
    LOG.debug("Register [{}] ({} open)", stage, this.fileWindowsOpen());
  }

  @Override
  public void closeWindow(
    final Stage stage)
  {
    Objects.requireNonNull(stage, "stage");
    stage.close();
    this.stages.remove(stage);
    LOG.debug("Deregister [{}] ({} open)", stage, this.fileWindowsOpen());
  }

  @Override
  public int fileWindowsOpen()
  {
    return this.stages.size();
  }

  @Override
  public String toString()
  {
    return String.format(
      "[%s 0x%08x]",
      this.getClass().getName(),
      Integer.valueOf(this.hashCode())
    );
  }
}
