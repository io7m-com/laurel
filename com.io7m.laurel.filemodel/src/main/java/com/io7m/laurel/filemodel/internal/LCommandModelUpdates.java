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

import com.io7m.laurel.model.LCategory;
import com.io7m.laurel.model.LTag;
import org.jooq.DSLContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.io7m.laurel.filemodel.internal.Tables.CATEGORIES;
import static com.io7m.laurel.filemodel.internal.Tables.TAGS;
import static com.io7m.laurel.filemodel.internal.Tables.TAG_CATEGORIES;

/**
 * Functions to perform model updates on database changes.
 */

public final class LCommandModelUpdates
{
  private LCommandModelUpdates()
  {

  }

  private static List<LTag> listTags(
    final DSLContext context)
  {
    return context.select(TAGS.TAG_TEXT)
      .from(TAGS)
      .orderBy(TAGS.TAG_TEXT.asc())
      .stream()
      .map(r -> new LTag(r.get(TAGS.TAG_TEXT)))
      .toList();
  }

  private static SortedMap<LCategory, List<LTag>> listCategoriesTags(
    final DSLContext context)
  {
    final var map =
      new TreeMap<LCategory, ArrayList<LTag>>();

    final var results =
      context.select(CATEGORIES.CATEGORY_TEXT, TAGS.TAG_TEXT)
        .from(TAG_CATEGORIES)
        .join(TAGS)
        .on(TAG_CATEGORIES.TAG_TAG_ID.eq(TAGS.TAG_ID))
        .join(CATEGORIES)
        .on(TAG_CATEGORIES.TAG_CATEGORY_ID.eq(CATEGORIES.CATEGORY_ID))
        .orderBy(CATEGORIES.CATEGORY_TEXT, TAGS.TAG_TEXT)
        .fetch();

    for (final var rec : results) {
      final var category =
        new LCategory(rec.get(CATEGORIES.CATEGORY_TEXT));
      final var tag =
        new LTag(rec.get(TAGS.TAG_TEXT));

      var existing = map.get(category);
      if (existing == null) {
        existing = new ArrayList<>();
      }
      existing.add(tag);
      map.put(category, existing);
    }

    return Collections.unmodifiableSortedMap(map);
  }

  private static List<LCategory> listCategoriesAll(
    final DSLContext context)
  {
    return context.select(CATEGORIES.CATEGORY_TEXT)
      .from(CATEGORIES)
      .orderBy(CATEGORIES.CATEGORY_TEXT.asc())
      .stream()
      .map(r -> new LCategory(r.get(CATEGORIES.CATEGORY_TEXT)))
      .toList();
  }

  private static List<LCategory> listCategoriesRequired(
    final DSLContext context)
  {
    return context.select(CATEGORIES.CATEGORY_TEXT)
      .from(CATEGORIES)
      .where(CATEGORIES.CATEGORY_REQUIRED.eq(1L))
      .orderBy(CATEGORIES.CATEGORY_TEXT.asc())
      .stream()
      .map(r -> new LCategory(r.get(CATEGORIES.CATEGORY_TEXT)))
      .toList();
  }

  static void updateTagsAndCategories(
    final DSLContext context,
    final LFileModel model)
  {
    model.setCategoriesAndTags(
      listTags(context),
      listCategoriesAll(context),
      listCategoriesRequired(context),
      listCategoriesTags(context)
    );
  }
}
