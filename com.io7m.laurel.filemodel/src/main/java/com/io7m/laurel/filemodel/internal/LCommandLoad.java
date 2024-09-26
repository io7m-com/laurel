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

import com.io7m.darco.api.DDatabaseUnit;
import org.jooq.DSLContext;

import java.util.Properties;

/**
 * Load everything.
 */

public final class LCommandLoad
  extends LCommandAbstract<DDatabaseUnit>
{
  /**
   * Load everything.
   */

  public LCommandLoad()
  {

  }

  /**
   * @return A command factory
   */

  public static LCommandFactoryType<DDatabaseUnit> provider()
  {
    return new LCommandFactory<>(
      LCommandLoad.class.getCanonicalName(),
      LCommandLoad::fromProperties
    );
  }

  private static LCommandLoad fromProperties(
    final Properties p)
  {
    final var c = new LCommandLoad();
    c.setExecuted(true);
    return c;
  }

  @Override
  protected LCommandUndoable onExecute(
    final LFileModel model,
    final LDatabaseTransactionType transaction,
    final DDatabaseUnit request)
  {
    final var context = transaction.get(DSLContext.class);

    model.eventWithProgress(0.0, "Loading images…");
    model.setImagesAll(LCommandModelUpdates.listImages(context));

    model.eventWithProgress(0.25, "Loading captions…");
    model.setCategoriesAndCaptions(
      context,
      LCommandModelUpdates.listCaptionsAll(context),
      LCommandModelUpdates.listCategoriesAll(context),
      LCommandModelUpdates.listCategoriesRequired(context),
      LCommandModelUpdates.listCategoriesCaptions(context)
    );

    model.eventWithProgress(0.5, "Loading metadata…");
    model.setMetadata(LCommandModelUpdates.listMetadata(context));

    model.eventWithProgress(0.75, "Loading global captions…");
    model.setGlobalCaptions(LCommandModelUpdates.listGlobalCaptions(context));

    model.eventWithProgress(0.8, "Loading undo stack…");
    model.loadUndo(transaction);

    model.eventWithProgress(0.9, "Loading redo stack…");
    model.loadRedo(transaction);

    model.eventWithProgress(1.0, "Loaded file.");
    return LCommandUndoable.COMMAND_NOT_UNDOABLE;
  }

  @Override
  protected void onUndo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void onRedo(
    final LFileModel model,
    final LDatabaseTransactionType transaction)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Properties toProperties()
  {
    return new Properties();
  }

  @Override
  public String describe()
  {
    return "Load data.";
  }
}
