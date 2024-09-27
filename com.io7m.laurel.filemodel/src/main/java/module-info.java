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

import com.io7m.laurel.filemodel.internal.LCommandCaptionDelete;
import com.io7m.laurel.filemodel.internal.LCommandCaptionsAdd;
import com.io7m.laurel.filemodel.internal.LCommandCaptionsModify;
import com.io7m.laurel.filemodel.internal.LCommandCategoriesAdd;
import com.io7m.laurel.filemodel.internal.LCommandCategoriesSetRequired;
import com.io7m.laurel.filemodel.internal.LCommandCategoriesUnsetRequired;
import com.io7m.laurel.filemodel.internal.LCommandCategoryCaptionsAssign;
import com.io7m.laurel.filemodel.internal.LCommandCategoryCaptionsUnassign;
import com.io7m.laurel.filemodel.internal.LCommandFactoryType;
import com.io7m.laurel.filemodel.internal.LCommandGlobalCaptionModify;
import com.io7m.laurel.filemodel.internal.LCommandGlobalCaptionOrderLower;
import com.io7m.laurel.filemodel.internal.LCommandGlobalCaptionOrderUpper;
import com.io7m.laurel.filemodel.internal.LCommandGlobalCaptionsAdd;
import com.io7m.laurel.filemodel.internal.LCommandGlobalCaptionsRemove;
import com.io7m.laurel.filemodel.internal.LCommandImageCaptionsAssign;
import com.io7m.laurel.filemodel.internal.LCommandImageCaptionsUnassign;
import com.io7m.laurel.filemodel.internal.LCommandImageSelect;
import com.io7m.laurel.filemodel.internal.LCommandImagesAdd;
import com.io7m.laurel.filemodel.internal.LCommandImagesDelete;
import com.io7m.laurel.filemodel.internal.LCommandMetadataPut;
import com.io7m.laurel.filemodel.internal.LCommandMetadataRemove;

/**
 * Image caption management (File model)
 */

module com.io7m.laurel.filemodel
{
  uses com.io7m.laurel.filemodel.internal.LDatabaseQueryProviderType;
  uses com.io7m.laurel.filemodel.internal.LCommandFactoryType;

  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires com.io7m.darco.api;
  requires com.io7m.darco.sqlite;
  requires com.io7m.jattribute.core;
  requires com.io7m.jmulticlose.core;
  requires com.io7m.lanark.core;
  requires com.io7m.laurel.model;
  requires com.io7m.seltzer.api;
  requires io.opentelemetry.api;
  requires java.desktop;
  requires org.apache.commons.io;
  requires org.apache.tika.core;
  requires org.jooq;
  requires org.slf4j;
  requires org.xerial.sqlitejdbc;

  provides LCommandFactoryType with
    LCommandCaptionDelete,
    LCommandCaptionsAdd,
    LCommandCaptionsModify,
    LCommandCategoriesAdd,
    LCommandCategoriesSetRequired,
    LCommandCategoriesUnsetRequired,
    LCommandCategoryCaptionsAssign,
    LCommandCategoryCaptionsUnassign,
    LCommandGlobalCaptionModify,
    LCommandGlobalCaptionOrderLower,
    LCommandGlobalCaptionOrderUpper,
    LCommandGlobalCaptionsAdd,
    LCommandGlobalCaptionsRemove,
    LCommandImageCaptionsAssign,
    LCommandImageCaptionsUnassign,
    LCommandImageSelect,
    LCommandImagesAdd,
    LCommandImagesDelete,
    LCommandMetadataPut,
    LCommandMetadataRemove
    ;

  exports com.io7m.laurel.filemodel;
  exports com.io7m.laurel.filemodel.internal
    to com.io7m.laurel.tests;
}
