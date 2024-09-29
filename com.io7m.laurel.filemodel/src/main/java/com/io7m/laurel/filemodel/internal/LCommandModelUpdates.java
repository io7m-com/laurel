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

import com.io7m.laurel.model.LCaption;
import com.io7m.laurel.model.LCaptionID;
import com.io7m.laurel.model.LCaptionName;
import com.io7m.laurel.model.LCategory;
import com.io7m.laurel.model.LCategoryID;
import com.io7m.laurel.model.LCategoryName;
import com.io7m.laurel.model.LGlobalCaption;
import com.io7m.laurel.model.LHashSHA256;
import com.io7m.laurel.model.LImage;
import com.io7m.laurel.model.LImageID;
import com.io7m.laurel.model.LImageWithID;
import com.io7m.laurel.model.LMetadataValue;
import com.io7m.mime2045.parser.MimeParsers;
import com.io7m.mime2045.parser.api.MimeParseException;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.io7m.laurel.filemodel.internal.Tables.CAPTIONS;
import static com.io7m.laurel.filemodel.internal.Tables.CAPTION_CATEGORIES;
import static com.io7m.laurel.filemodel.internal.Tables.CATEGORIES;
import static com.io7m.laurel.filemodel.internal.Tables.GLOBAL_CAPTIONS;
import static com.io7m.laurel.filemodel.internal.Tables.IMAGES;
import static com.io7m.laurel.filemodel.internal.Tables.IMAGE_BLOBS;
import static com.io7m.laurel.filemodel.internal.Tables.IMAGE_CAPTIONS;
import static com.io7m.laurel.filemodel.internal.Tables.IMAGE_CAPTIONS_COUNTS;
import static com.io7m.laurel.filemodel.internal.Tables.METADATA;

/**
 * Functions to perform model updates on database changes.
 */

public final class LCommandModelUpdates
{
  static final MimeParsers MIME_PARSERS =
    new MimeParsers();

  static final Field<Long> COUNT_FIELD =
    DSL.coalesce(IMAGE_CAPTIONS_COUNTS.COUNT_CAPTION_COUNT, 0L)
      .as(IMAGE_CAPTIONS_COUNTS.COUNT_CAPTION_COUNT);

  private LCommandModelUpdates()
  {

  }

  static List<LImageWithID> listImages(
    final DSLContext context)
  {
    return context.select(
        IMAGES.IMAGE_ID,
        IMAGES.IMAGE_SOURCE,
        IMAGES.IMAGE_NAME,
        IMAGES.IMAGE_FILE,
        IMAGE_BLOBS.IMAGE_BLOB_SHA256,
        IMAGE_BLOBS.IMAGE_BLOB_TYPE
      )
      .from(IMAGES)
      .join(IMAGE_BLOBS)
      .on(IMAGE_BLOBS.IMAGE_BLOB_ID.eq(IMAGES.IMAGE_BLOB))
      .orderBy(IMAGES.IMAGE_NAME)
      .stream()
      .map(LCommandModelUpdates::mapImageRecord)
      .toList();
  }

  private static LImageWithID mapImageRecord(
    final org.jooq.Record r)
  {
    try {
      return new LImageWithID(
        new LImageID(r.<Long>get(IMAGES.IMAGE_ID).longValue()),
        new LImage(
          r.get(IMAGES.IMAGE_NAME),
          Optional.ofNullable(r.get(IMAGES.IMAGE_FILE)).map(Paths::get),
          Optional.ofNullable(r.get(IMAGES.IMAGE_SOURCE)).map(URI::create),
          MIME_PARSERS.parse(r.get(IMAGE_BLOBS.IMAGE_BLOB_TYPE)),
          new LHashSHA256(r.get(IMAGE_BLOBS.IMAGE_BLOB_SHA256))
        )
      );
    } catch (final MimeParseException e) {
      throw new IllegalStateException(e);
    }
  }

  static List<LCaption> listCaptionsAll(
    final DSLContext context)
  {
    return context.select(
        CAPTIONS.CAPTION_ID,
        CAPTIONS.CAPTION_TEXT,
        COUNT_FIELD)
      .from(CAPTIONS)
      .leftJoin(IMAGE_CAPTIONS_COUNTS)
      .on(IMAGE_CAPTIONS_COUNTS.COUNT_CAPTION_ID.eq(CAPTIONS.CAPTION_ID))
      .orderBy(CAPTIONS.CAPTION_TEXT.asc())
      .stream()
      .map(r -> {
        return new LCaption(
          new LCaptionID(r.<Long>get(CAPTIONS.CAPTION_ID).longValue()),
          new LCaptionName(r.get(CAPTIONS.CAPTION_TEXT)),
          r.<Long>get(COUNT_FIELD).longValue()
        );
      })
      .toList();
  }


  static List<LCaption> listCategoryCaptionsAssigned(
    final DSLContext context,
    final LCategoryID id)
  {
    return context.select(
        CAPTIONS.CAPTION_ID,
        CAPTIONS.CAPTION_TEXT,
        COUNT_FIELD)
      .from(CAPTIONS)
      .join(CAPTION_CATEGORIES)
      .on(CAPTION_CATEGORIES.CAPTION_CAPTION_ID.eq(CAPTIONS.CAPTION_ID))
      .leftJoin(IMAGE_CAPTIONS_COUNTS)
      .on(IMAGE_CAPTIONS_COUNTS.COUNT_CAPTION_ID.eq(CAPTIONS.CAPTION_ID))
      .where(CAPTION_CATEGORIES.CAPTION_CATEGORY_ID.eq(id.value()))
      .orderBy(CAPTIONS.CAPTION_TEXT.asc())
      .stream()
      .map(r -> {
        return new LCaption(
          new LCaptionID(r.<Long>get(CAPTIONS.CAPTION_ID).longValue()),
          new LCaptionName(r.get(CAPTIONS.CAPTION_TEXT)),
          r.<Long>get(COUNT_FIELD).longValue()
        );
      })
      .toList();
  }

  static List<LCaption> listImageCaptionsAssigned(
    final DSLContext context,
    final LImageID id)
  {
    return context.select(
        CAPTIONS.CAPTION_ID,
        CAPTIONS.CAPTION_TEXT,
        COUNT_FIELD)
      .from(CAPTIONS)
      .join(IMAGE_CAPTIONS)
      .on(IMAGE_CAPTIONS.IMAGE_CAPTION_CAPTION.eq(CAPTIONS.CAPTION_ID))
      .join(IMAGE_CAPTIONS_COUNTS)
      .on(IMAGE_CAPTIONS_COUNTS.COUNT_CAPTION_ID.eq(CAPTIONS.CAPTION_ID))
      .where(IMAGE_CAPTIONS.IMAGE_CAPTION_IMAGE.eq(id.value()))
      .orderBy(CAPTIONS.CAPTION_TEXT.asc())
      .stream()
      .map(r -> {
        return new LCaption(
          new LCaptionID(r.<Long>get(CAPTIONS.CAPTION_ID).longValue()),
          new LCaptionName(r.get(CAPTIONS.CAPTION_TEXT)),
          r.<Long>get(COUNT_FIELD).longValue()
        );
      })
      .toList();
  }

  static SortedMap<LCategoryID, List<LCaption>> listCategoriesCaptions(
    final DSLContext context)
  {
    final var map =
      new TreeMap<LCategoryID, ArrayList<LCaption>>();

    final var results =
      context.select(
          CATEGORIES.CATEGORY_ID,
          CATEGORIES.CATEGORY_TEXT,
          CAPTIONS.CAPTION_ID,
          CAPTIONS.CAPTION_TEXT,
          COUNT_FIELD)
        .from(CAPTION_CATEGORIES)
        .join(CAPTIONS)
        .on(CAPTION_CATEGORIES.CAPTION_CAPTION_ID.eq(CAPTIONS.CAPTION_ID))
        .join(CATEGORIES)
        .on(CAPTION_CATEGORIES.CAPTION_CATEGORY_ID.eq(CATEGORIES.CATEGORY_ID))
        .leftJoin(IMAGE_CAPTIONS_COUNTS)
        .on(IMAGE_CAPTIONS_COUNTS.COUNT_CAPTION_ID.eq(CAPTIONS.CAPTION_ID))
        .orderBy(CATEGORIES.CATEGORY_TEXT, CAPTIONS.CAPTION_TEXT)
        .fetch();

    for (final var rec : results) {
      final var category =
        new LCategoryID(rec.get(CATEGORIES.CATEGORY_ID));
      final var caption =
        new LCaptionName(rec.get(CAPTIONS.CAPTION_TEXT));
      final var tagWithId =
        new LCaption(
          new LCaptionID(rec.<Long>get(CAPTIONS.CAPTION_ID).longValue()),
          caption,
          rec.<Long>get(COUNT_FIELD).longValue()
        );

      var existing = map.get(category);
      if (existing == null) {
        existing = new ArrayList<>();
      }
      existing.add(tagWithId);
      map.put(category, existing);
    }

    return Collections.unmodifiableSortedMap(map);
  }

  static List<LCategory> listCategoriesAll(
    final DSLContext context)
  {
    return context.select(
        CATEGORIES.CATEGORY_ID,
        CATEGORIES.CATEGORY_REQUIRED,
        CATEGORIES.CATEGORY_TEXT)
      .from(CATEGORIES)
      .orderBy(CATEGORIES.CATEGORY_TEXT.asc())
      .stream()
      .map(r -> {
        return new LCategory(
          new LCategoryID(r.<Long>get(CATEGORIES.CATEGORY_ID).longValue()),
          new LCategoryName(r.get(CATEGORIES.CATEGORY_TEXT)),
          booleanOf(r.get(CATEGORIES.CATEGORY_REQUIRED))
        );
      })
      .toList();
  }

  private static boolean booleanOf(
    final Long x)
  {
    return x.longValue() != 0L;
  }

  static List<LCategory> listCategoriesRequired(
    final DSLContext context)
  {
    return context.select(
        CATEGORIES.CATEGORY_TEXT,
        CATEGORIES.CATEGORY_ID,
        CATEGORIES.CATEGORY_REQUIRED)
      .from(CATEGORIES)
      .where(CATEGORIES.CATEGORY_REQUIRED.eq(1L))
      .orderBy(CATEGORIES.CATEGORY_TEXT.asc())
      .stream()
      .map(r -> {
        return new LCategory(
          new LCategoryID(r.<Long>get(CATEGORIES.CATEGORY_ID).longValue()),
          new LCategoryName(r.get(CATEGORIES.CATEGORY_TEXT)),
          booleanOf(r.get(CATEGORIES.CATEGORY_REQUIRED))
        );
      })
      .toList();
  }

  static void updateCaptionsAndCategories(
    final DSLContext context,
    final LFileModel model)
  {
    final var imageSelectedOpt =
      model.imageSelected().get();
    if (imageSelectedOpt.isPresent()) {
      model.setImageCaptionsAssigned(
        listImageCaptionsAssigned(context, imageSelectedOpt.get().id())
      );
    } else {
      model.setImageCaptionsAssigned(List.of());
    }

    final var categorySelectedOpt =
      model.categorySelected().get();
    if (categorySelectedOpt.isPresent()) {
      model.setCategoryCaptionsAssigned(
        listCategoryCaptionsAssigned(context, categorySelectedOpt.get().id())
      );
    } else {
      model.setCategoryCaptionsAssigned(List.of());
    }

    model.setCategoriesAndCaptions(
      context,
      listCaptionsAll(context),
      listCategoriesAll(context),
      listCategoriesRequired(context),
      listCategoriesCaptions(context)
    );
  }

  static List<LCaptionID> listImageCaptions(
    final DSLContext context,
    final LImageID id)
  {
    return context.select(IMAGE_CAPTIONS.IMAGE_CAPTION_CAPTION)
      .from(IMAGE_CAPTIONS)
      .where(IMAGE_CAPTIONS.IMAGE_CAPTION_IMAGE.eq(id.value()))
      .stream()
      .map(x -> new LCaptionID(x.value1().longValue()))
      .toList();
  }

  static List<LMetadataValue> listMetadata(
    final DSLContext context)
  {
    return context.select(METADATA.META_NAME, METADATA.META_VALUE)
      .from(METADATA)
      .orderBy(METADATA.META_NAME.asc(), METADATA.META_VALUE.asc())
      .stream()
      .map(x -> {
        return new LMetadataValue(
          x.get(METADATA.META_NAME),
          x.get(METADATA.META_VALUE)
        );
      }).toList();
  }

  static List<LGlobalCaption> listGlobalCaptions(
    final DSLContext context)
  {
    return context.select(
        GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID,
        GLOBAL_CAPTIONS.GLOBAL_CAPTION_TEXT,
        GLOBAL_CAPTIONS.GLOBAL_CAPTION_ORDER)
      .from(GLOBAL_CAPTIONS)
      .orderBy(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ORDER.asc())
      .stream()
      .map(r -> {
        return new LGlobalCaption(
          new LCaption(
            new LCaptionID(r.get(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ID)),
            new LCaptionName(r.get(GLOBAL_CAPTIONS.GLOBAL_CAPTION_TEXT)),
            1L
          ),
          r.get(GLOBAL_CAPTIONS.GLOBAL_CAPTION_ORDER).longValue()
        );
      })
      .toList();
  }
}
