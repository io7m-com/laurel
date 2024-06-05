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


package com.io7m.laurel.io;

import com.io7m.laurel.model.LImage;
import com.io7m.laurel.model.LImageCaption;
import com.io7m.laurel.model.LImageCaptionID;
import com.io7m.laurel.model.LImageID;
import com.io7m.laurel.model.LImageSet;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Functions over image sets.
 */

public final class LImageSets
{
  private LImageSets()
  {

  }

  private static final String CREATE_TABLE_CAPTIONS = """
    CREATE TABLE captions (
      cap_id   TEXT UNIQUE NOT NULL,
      cap_x    TEXT UNIQUE,
      cap_y    TEXT UNIQUE,
      cap_text TEXT UNIQUE NOT NULL
    )
    """;

  private static final String CAPTION_INSERT = """
    INSERT INTO captions (cap_id, cap_x, cap_y, cap_text) VALUES (?, ?, ?, ?)
    """;

  private static final String CAPTION_FIND_WITH_TEXT = """
    SELECT * FROM captions AS c WHERE c.cap_text = ?
    """;

  private static final String CAPTION_UPDATE_Y = """
    UPDATE captions
      SET cap_y = ?
        WHERE captions.cap_id = ?
    """;

  private static final String CAPTIONS_ALL = """
    SELECT * FROM captions AS c
    """;

  private static final String CREATE_TABLE_IMAGES = """
    CREATE TABLE images (
      image_id   TEXT UNIQUE NOT NULL,
      image_x    TEXT UNIQUE,
      image_y    TEXT UNIQUE,
      image_file TEXT UNIQUE NOT NULL
    )
    """;

  private static final String IMAGE_INSERT = """
    INSERT INTO images (image_id, image_x, image_y, image_file) VALUES (?, ?, ?, ?)
    """;

  private static final String IMAGE_FIND_WITH_FILE = """
    SELECT * FROM images AS i WHERE i.image_file = ?
    """;

  private static final String IMAGES_ALL = """
    SELECT * FROM images AS i
    """;

  private static final String IMAGE_UPDATE_Y = """
    UPDATE images
      SET image_y = ?
        WHERE images.image_id = ?
    """;

  private static final String CREATE_TABLE_IMAGE_CAPTIONS = """
    CREATE TABLE image_captions (
      ic_image   TEXT NOT NULL,
      ic_caption TEXT NOT NULL,
      
      CONSTRAINT image_caption_unique
        UNIQUE (ic_image, ic_caption),

      FOREIGN KEY (ic_image)
        REFERENCES images (image_id),

      FOREIGN KEY (ic_caption)
        REFERENCES captions (cap_id)
    )
    """;

  private static final String IMAGE_CAPTION_INSERT_FROM_X = """
    INSERT INTO image_captions (ic_image, ic_caption)
      VALUES (
        (SELECT images.image_id FROM images
          WHERE images.image_x = ?),
        (SELECT captions.cap_id FROM captions
          WHERE captions.cap_x = ?)
      )
    """;

  private static final String IMAGE_CAPTION_INSERT_FROM_Y = """
    INSERT INTO image_captions (ic_image, ic_caption)
      VALUES (
        (SELECT images.image_id FROM images
          WHERE images.image_y = ?),
        (SELECT captions.cap_id FROM captions
          WHERE captions.cap_y = ?)
      )
    """;

  private static final String CAPTIONS_FOR_IMAGE = """
    SELECT
      captions.cap_id,
      captions.cap_text
    FROM image_captions AS ic
    JOIN captions ON captions.cap_id = ic.ic_caption
      WHERE ic_image = ?
    """;

  /**
   * Merge the two image sets.
   *
   * @param x The first image set
   * @param y The second image set
   *
   * @return The merge of {@code x} and {@code y}
   */

  public static LImageSet merge(
    final LImageSet x,
    final LImageSet y)
  {
    Objects.requireNonNull(x, "x");
    Objects.requireNonNull(y, "y");

    final var source = new SQLiteDataSource(new SQLiteConfig());
    source.setUrl("jdbc:sqlite::memory:");

    try (var c = source.getConnection()) {
      try (var st = c.prepareStatement(CREATE_TABLE_CAPTIONS)) {
        st.execute();
      }
      try (var st = c.prepareStatement(CREATE_TABLE_IMAGES)) {
        st.execute();
      }
      try (var st = c.prepareStatement(CREATE_TABLE_IMAGE_CAPTIONS)) {
        st.execute();
      }

      insertXImages(x, c);
      insertYImages(y, c);

      insertXCaptions(x, c);
      insertYCaptions(y, c);

      insertXImageCaptions(x, c);
      insertYImageCaptions(y, c);

      final var captions =
        collectCaptions(c);
      final var images =
        collectImages(c);

      final var globals = new HashSet<String>();
      globals.addAll(x.globalPrefixCaptions());
      globals.addAll(y.globalPrefixCaptions());

      return new LImageSet(List.copyOf(globals), captions, images);
    } catch (final SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Merge all the given image sets.
   *
   * @param images The image sets
   *
   * @return The merge of all the given sets
   */

  public static LImageSet mergeAll(
    final List<LImageSet> images)
  {
    if (images.isEmpty()) {
      return new LImageSet(
        List.of(),
        new TreeMap<>(),
        new TreeMap<>()
      );
    }

    if (images.size() == 1) {
      return images.get(0);
    }

    final var iter = images.iterator();
    var merged = iter.next();
    while (iter.hasNext()) {
      final var current = iter.next();
      merged = merge(merged, current);
    }
    return merged;
  }

  private static SortedMap<LImageID, LImage> collectImages(
    final Connection c)
    throws SQLException
  {
    final var images = new TreeMap<LImageID, LImage>();

    try (var st0 = c.prepareStatement(IMAGES_ALL)) {
      try (var rs0 = st0.executeQuery()) {
        while (rs0.next()) {
          final var id =
            new LImageID(UUID.fromString(rs0.getString("image_id")));
          final var file =
            rs0.getString("image_file");

          try (var st1 = c.prepareStatement(CAPTIONS_FOR_IMAGE)) {
            try (var rs1 = st1.executeQuery()) {
              final var captions = new TreeSet<LImageCaptionID>();
              while (rs1.next()) {
                captions.add(new LImageCaptionID(
                  UUID.fromString(rs1.getString("caption_id"))
                ));
              }

              final var image = new LImage(id, file, captions);
              images.put(id, image);
            }
          }
        }
      }
    }

    return images;
  }

  private static void insertXImageCaptions(
    final LImageSet i,
    final Connection c)
    throws SQLException
  {
    for (final var image : i.images().keySet()) {
      final var captions = i.captionsForImage(image);
      for (final var cap : captions) {
        try (var st = c.prepareStatement(IMAGE_CAPTION_INSERT_FROM_X)) {
          st.setString(1, image.toString());
          st.setString(2, cap.id().toString());
          st.executeUpdate();
        }
      }
    }
  }

  private static void insertYImageCaptions(
    final LImageSet i,
    final Connection c)
    throws SQLException
  {
    for (final var image : i.images().keySet()) {
      final var captions = i.captionsForImage(image);
      for (final var cap : captions) {
        try (var st = c.prepareStatement(IMAGE_CAPTION_INSERT_FROM_Y)) {
          st.setString(1, image.toString());
          st.setString(2, cap.id().toString());
          st.executeUpdate();
        }
      }
    }
  }

  /**
   * Insert all the y images. If a image exists already with the
   * given text, update the Y value. Otherwise, record a new image with
   * a fresh image ID.
   */

  private static void insertYImages(
    final LImageSet y,
    final Connection conn)
    throws SQLException
  {
    for (final var cap : y.images().values()) {
      try (var st0 = conn.prepareStatement(IMAGE_FIND_WITH_FILE)) {
        st0.setString(1, cap.fileName());
        try (var rs = st0.executeQuery()) {
          if (rs.next()) {
            try (var st1 = conn.prepareStatement(IMAGE_UPDATE_Y)) {
              st1.setString(1, cap.imageID().toString());
              st1.setString(2, rs.getString("image_id"));
              st1.executeUpdate();
            }
          } else {
            try (var st1 = conn.prepareStatement(IMAGE_INSERT)) {
              st1.setString(1, UUID.randomUUID().toString());
              st1.setString(2, null);
              st1.setString(3, cap.imageID().toString());
              st1.setString(4, cap.fileName());
              st1.executeUpdate();
            }
          }
        }
      }
    }
  }

  /**
   * Insert all the x images. Record the old image IDs and generate
   * fresh IDs.
   */

  private static void insertXImages(
    final LImageSet x,
    final Connection conn)
    throws SQLException
  {
    for (final var i : x.images().values()) {
      try (var st = conn.prepareStatement(IMAGE_INSERT)) {
        st.setString(1, UUID.randomUUID().toString());
        st.setString(2, i.imageID().toString());
        st.setString(3, null);
        st.setString(4, i.fileName());
        st.executeUpdate();
      }
    }
  }

  /**
   * We now have the complete set of unique captions in the database.
   */

  private static TreeMap<LImageCaptionID, LImageCaption>
  collectCaptions(
    final Connection conn)
    throws SQLException
  {
    final var captions = new TreeMap<LImageCaptionID, LImageCaption>();
    try (var st = conn.prepareStatement(CAPTIONS_ALL)) {
      try (var rs = st.executeQuery()) {
        while (rs.next()) {
          final var cap =
            new LImageCaption(
              new LImageCaptionID(UUID.fromString(rs.getString("cap_id"))),
              rs.getString("cap_text")
            );
          captions.put(cap.id(), cap);
        }
      }
    }
    return captions;
  }

  /**
   * Insert all the y captions. If a caption exists already with the
   * given text, update the Y value. Otherwise, record a new caption with
   * a fresh caption ID.
   */

  private static void insertYCaptions(
    final LImageSet y,
    final Connection conn)
    throws SQLException
  {
    for (final var cap : y.captions().values()) {
      try (var st0 = conn.prepareStatement(CAPTION_FIND_WITH_TEXT)) {
        st0.setString(1, cap.text());
        try (var rs = st0.executeQuery()) {
          if (rs.next()) {
            try (var st1 = conn.prepareStatement(CAPTION_UPDATE_Y)) {
              st1.setString(1, cap.id().toString());
              st1.setString(2, rs.getString("cap_id"));
              st1.executeUpdate();
            }
          } else {
            try (var st1 = conn.prepareStatement(CAPTION_INSERT)) {
              st1.setString(1, UUID.randomUUID().toString());
              st1.setString(2, null);
              st1.setString(3, cap.id().toString());
              st1.setString(4, cap.text());
              st1.executeUpdate();
            }
          }
        }
      }
    }
  }

  /**
   * Insert all the x captions. Record the old caption IDs and generate
   * fresh IDs.
   */

  private static void insertXCaptions(
    final LImageSet x,
    final Connection conn)
    throws SQLException
  {
    for (final var cap : x.captions().values()) {
      try (var st = conn.prepareStatement(CAPTION_INSERT)) {
        st.setString(1, UUID.randomUUID().toString());
        st.setString(2, cap.id().toString());
        st.setString(3, null);
        st.setString(4, cap.text());
        st.executeUpdate();
      }
    }
  }
}
