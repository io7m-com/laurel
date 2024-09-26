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

import com.io7m.jattribute.core.AttributeReadableType;
import com.io7m.jattribute.core.AttributeReceiverType;
import com.io7m.jattribute.core.AttributeSubscriptionType;
import com.io7m.jattribute.core.AttributeType;
import com.io7m.laurel.filemodel.LFileModelType;
import com.io7m.laurel.filemodel.LFileModels;
import com.io7m.laurel.model.LException;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

final class LFileModelScope
  implements AttributeType<Optional<LFileModelType>>
{
  private final AttributeType<Optional<LFileModelType>> fileModel;

  LFileModelScope()
  {
    this.fileModel =
      LAttributes.withValue(Optional.empty());
  }

  public static LFileModelScope createNewScope(
    final Path path)
    throws LException
  {
    final var file = LFileModels.open(path, false);
    final var scope = new LFileModelScope();
    scope.fileModel.set(Optional.of(file));
    return scope;
  }

  public static LFileModelScope createEmptyScope()
  {
    return new LFileModelScope();
  }

  public void reopen(
    final Path path)
    throws LException
  {
    final var file = LFileModels.open(path, false);
    this.fileModel.set(Optional.of(file));
  }

  @Override
  public <B> AttributeType<B> map(
    final Function<Optional<LFileModelType>, B> f)
  {
    return this.fileModel.map(f);
  }

  @Override
  public Optional<LFileModelType> set(
    final Optional<LFileModelType> y)
  {
    return this.fileModel.set(y);
  }

  @Override
  public Optional<LFileModelType> get()
  {
    return this.fileModel.get();
  }

  @Override
  public <B> AttributeReadableType<B> mapR(
    final Function<Optional<LFileModelType>, B> f)
  {
    return this.fileModel.mapR(f);
  }

  @Override
  public AttributeSubscriptionType subscribe(
    final AttributeReceiverType<Optional<LFileModelType>> receiver)
  {
    return this.fileModel.subscribe(receiver);
  }

  public void close()
    throws LException
  {
    final var existing = this.fileModel.get();
    if (existing.isPresent()) {
      existing.get().close();
      this.fileModel.set(Optional.empty());
    }
  }
}
