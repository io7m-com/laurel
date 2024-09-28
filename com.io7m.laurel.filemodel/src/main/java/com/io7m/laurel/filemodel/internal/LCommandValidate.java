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
import com.io7m.laurel.filemodel.LValidationProblemType;
import com.io7m.laurel.filemodel.LValidationProblemType.ImageMissingRequiredCaption;
import com.io7m.laurel.model.LCaption;
import com.io7m.laurel.model.LCaptionID;
import com.io7m.laurel.model.LCategory;
import com.io7m.laurel.model.LImageWithID;
import org.jooq.DSLContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Validate.
 */

public final class LCommandValidate
  extends LCommandAbstract<DDatabaseUnit>
{
  private final ArrayList<LValidationProblemType> problems;

  /**
   * Validate.
   */

  public LCommandValidate()
  {
    this.problems = new ArrayList<LValidationProblemType>();
  }

  /**
   * Validate.
   *
   * @return A command factory
   */

  public static LCommandFactoryType<DDatabaseUnit> provider()
  {
    return new LCommandFactory<>(
      LCommandValidate.class.getCanonicalName(),
      LCommandValidate::fromProperties
    );
  }

  private static LCommandValidate fromProperties(
    final Properties p)
  {
    final var c = new LCommandValidate();
    c.setExecuted(true);
    return c;
  }

  @Override
  protected LCommandUndoable onExecute(
    final LFileModel model,
    final LDatabaseTransactionType transaction,
    final DDatabaseUnit unit)
  {
    final var context = transaction.get(DSLContext.class);

    this.problems.clear();

    for (final var image : model.imageList().get()) {
      this.checkImage(context, model, image);
    }

    model.setValidationProblems(List.copyOf(this.problems));
    return LCommandUndoable.COMMAND_NOT_UNDOABLE;
  }

  private void checkImage(
    final DSLContext context,
    final LFileModel model,
    final LImageWithID image)
  {
    final var captionsAssigned =
      LCommandModelUpdates.listImageCaptionsAssigned(context, image.id())
        .stream()
        .collect(Collectors.toMap(LCaption::id, c -> c));

    for (final var category : model.categoriesRequired().get()) {
      this.checkImageWithCategory(
        context,
        image,
        captionsAssigned,
        category
      );
    }
  }

  private void checkImageWithCategory(
    final DSLContext context,
    final LImageWithID image,
    final Map<LCaptionID, LCaption> captionsAssigned,
    final LCategory category)
  {
    final var categoryCaptions =
      LCommandModelUpdates.listCategoryCaptionsAssigned(context, category.id())
        .stream()
        .collect(Collectors.toMap(LCaption::id, c -> c));

    final var intersection = new HashSet<>(captionsAssigned.keySet());
    intersection.retainAll(categoryCaptions.keySet());

    if (intersection.isEmpty()) {
      this.problems.add(
        new ImageMissingRequiredCaption(
          image.id(),
          category.id(),
          "Image '%s' does not contain any captions from the required category '%s'."
            .formatted(image.image().name(), category.name())
        )
      );
    }
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
    return "Validate";
  }
}
