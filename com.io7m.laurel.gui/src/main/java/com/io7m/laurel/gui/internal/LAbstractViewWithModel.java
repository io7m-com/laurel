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

import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import com.io7m.laurel.filemodel.LFileModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

abstract class LAbstractViewWithModel implements LViewType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LAbstractViewWithModel.class);

  private final LFileModelScope fileModel;
  private CloseableCollectionType<ClosingResourceFailedException> fileModelSubscriptions;

  protected LAbstractViewWithModel(
    final LFileModelScope inFileModel)
  {
    this.fileModel =
      Objects.requireNonNull(inFileModel, "fileModel");
    this.fileModelSubscriptions =
      CloseableCollection.create();
  }

  protected abstract void onInitialize();

  @Override
  public final void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.onInitialize();

    this.fileModel.subscribe((oldValue, newValue) -> {
      if (oldValue.isPresent() && newValue.isEmpty()) {
        this.onFileBecameUnavailable();
        try {
          this.fileModelSubscriptions.close();
        } catch (final ClosingResourceFailedException e) {
          LOG.debug("Failed to close subscriptions: ", e);
        }
        return;
      }

      if (oldValue.isEmpty() && newValue.isPresent()) {
        this.fileModelSubscriptions = CloseableCollection.create();
      }

      if (newValue.isPresent()) {
        this.onFileBecameAvailable(
          this.fileModelSubscriptions,
          newValue.get()
        );
      }
    });
  }

  protected abstract void onFileBecameUnavailable();

  protected abstract void onFileBecameAvailable(
    CloseableCollectionType<?> subscriptions,
    LFileModelType model);

  public final LFileModelScope fileModelScope()
  {
    return this.fileModel;
  }

  public final LFileModelType fileModelNow()
  {
    return this.fileModelScope()
      .get()
      .orElseThrow(() -> {
        return new IllegalStateException("No file is open!");
      });
  }
}
