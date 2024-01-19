/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
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


import com.io7m.jade.api.ApplicationDirectoriesType;
import com.io7m.jattribute.core.Attributes;
import com.io7m.repetoir.core.RPServiceDirectoryWritableType;
import com.io7m.repetoir.core.RPServiceType;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.util.Objects;

/**
 * The factory of screen controllers.
 */

public final class LScreenViewFactory
  implements Callback<Class<?>, Object>, RPServiceType
{
  private final RPServiceDirectoryWritableType services;
  private final Attributes attributes;
  private final ApplicationDirectoriesType directories;
  private final Stage stage;

  /**
   * The factory of screen controllers.
   *
   * @param inServices    The service directory
   * @param inAttributes  The attributes
   * @param inDirectories The directories
   * @param inStage       The stage
   */

  public LScreenViewFactory(
    final RPServiceDirectoryWritableType inServices,
    final Attributes inAttributes,
    final ApplicationDirectoriesType inDirectories,
    final Stage inStage)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.attributes =
      Objects.requireNonNull(inAttributes, "inAttributes");
    this.directories =
      Objects.requireNonNull(inDirectories, "inDirectories");
    this.stage =
      Objects.requireNonNull(inStage, "stage");
  }

  @Override
  public Object call(
    final Class<?> param)
  {
    if (Objects.equals(param, LMainView.class)) {
      return new LMainView(this.services, this.stage);
    }
    if (Objects.equals(param, LCaptionsView.class)) {
      return new LCaptionsView(this.services);
    }

    throw new IllegalStateException(
      "Unrecognized screen controller: %s".formatted(param)
    );
  }

  @Override
  public String toString()
  {
    return String.format("[LScreenControllerFactory 0x%08x]", this.hashCode());
  }

  @Override
  public String description()
  {
    return "Screen controller factory service.";
  }
}
