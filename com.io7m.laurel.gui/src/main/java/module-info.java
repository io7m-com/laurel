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

/**
 * Image caption management (GUI)
 */

open module com.io7m.laurel.gui
{
  requires static org.osgi.annotation.versioning;
  requires static org.osgi.annotation.bundle;

  requires com.io7m.laurel.filemodel;
  requires com.io7m.laurel.model;

  requires com.io7m.anethum.api;
  requires com.io7m.darco.api;
  requires com.io7m.jade.api;
  requires com.io7m.jaffirm.core;
  requires com.io7m.jattribute.core;
  requires com.io7m.jmulticlose.core;
  requires com.io7m.junreachable.core;
  requires com.io7m.jwheatsheaf.api;
  requires com.io7m.jwheatsheaf.oxygen;
  requires com.io7m.jwheatsheaf.ui;
  requires com.io7m.jxtrand.api;
  requires com.io7m.miscue.fx.seltzer;
  requires com.io7m.repetoir.core;
  requires com.io7m.seltzer.api;

  requires java.desktop;
  requires javafx.base;
  requires javafx.controls;
  requires javafx.fxml;
  requires org.slf4j;

  exports com.io7m.laurel.gui;

  exports com.io7m.laurel.gui.internal;
}