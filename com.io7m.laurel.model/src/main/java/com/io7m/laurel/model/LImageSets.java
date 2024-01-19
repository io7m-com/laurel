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


package com.io7m.laurel.model;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.Collectors;

/**
 * Functions to create image sets.
 */

public final class LImageSets
{
  private LImageSets()
  {

  }

  /**
   * @return An empty image set
   */

  public static LImageSetType empty()
  {
    return new LImageSet();
  }

  private static final class LImageSet implements LImageSetType
  {
    private final SortedMap<LImageCaptionID, LImageCaption> captions;
    private final SortedMap<LImageID, LImage> images;
    private final SubmissionPublisher<LEventType> events;
    private final HashMap<LImageCaptionID, Long> captionCounts;
    private final BidiMap<String, LImageCaptionID> captionTexts;

    /**
     * A mutable image set.
     */

    LImageSet()
    {
      this.captions = new TreeMap<>();
      this.images = new TreeMap<>();
      this.events = new SubmissionPublisher<>();
      this.captionCounts = new HashMap<>();
      this.captionTexts = new DualHashBidiMap<>();
    }

    void captionCountIncrement(
      final LImageCaptionID id)
    {
      this.captionCounts.compute(id, (captionID, existing) -> {
        if (existing == null) {
          return Long.valueOf(1L);
        } else {
          return Long.valueOf(existing.longValue() + 1L);
        }
      });
    }

    void captionCountDecrement(
      final LImageCaptionID id)
    {
      this.captionCounts.compute(id, (captionID, existing) -> {
        if (existing == null) {
          return Long.valueOf(0L);
        } else {
          return Long.valueOf(Math.max(existing.longValue() - 1L, 0L));
        }
      });
    }

    @Override
    public String toString()
    {
      final StringBuilder sb = new StringBuilder("LImageSet{");
      sb.append("captions=").append(this.captions);
      sb.append(", images=").append(this.images);
      sb.append('}');
      return sb.toString();
    }

    @Override
    public boolean equals(final Object o)
    {
      if (this == o) {
        return true;
      }
      if (o == null || !this.getClass().equals(o.getClass())) {
        return false;
      }
      final LImageSet imageSet = (LImageSet) o;
      return Objects.equals(this.captions, imageSet.captions)
             && Objects.equals(this.images, imageSet.images);
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(this.captions, this.images);
    }

    @Override
    public SortedMap<LImageCaptionID, LImageCaption> captions()
    {
      return Collections.unmodifiableSortedMap(this.captions);
    }

    @Override
    public SortedMap<LImageID, LImage> images()
    {
      return Collections.unmodifiableSortedMap(this.images);
    }

    @Override
    public long captionAssignmentCount(
      final LImageCaptionID caption)
    {
      return this.captionCounts.getOrDefault(caption, Long.valueOf(0L))
        .longValue();
    }

    @Override
    public Optional<LImageCaptionID> captionForText(
      final String text)
    {
      return Optional.ofNullable(this.captionTexts.get(text));
    }

    @Override
    public Flow.Publisher<LEventType> events()
    {
      return this.events;
    }

    @Override
    public LImageSetCommandType captionUpdate(
      final LImageCaption caption)
    {
      Objects.requireNonNull(caption, "caption");
      return new LOpCaptionUpdate(this, caption);
    }

    @Override
    public LImageSetCommandType captionRemove(
      final LImageCaptionID caption)
    {
      Objects.requireNonNull(caption, "caption");
      return new LOpCaptionRemove(this, caption);
    }

    @Override
    public LImageSetCommandType imageUpdate(
      final LImage image)
    {
      Objects.requireNonNull(image, "image");
      return new LOpImageUpdate(this, image);
    }

    @Override
    public LImageSetCommandType compose(
      final String description,
      final List<LImageSetCommandType> commands)
    {
      return new LOpComposed(description, commands);
    }

    @Override
    public LImageSetCommandType captionAssign(
      final LImageID image,
      final LImageCaptionID caption)
    {
      Objects.requireNonNull(image, "image");
      Objects.requireNonNull(caption, "caption");
      return new LOpCaptionAssign(this, image, caption);
    }

    @Override
    public LImageSetCommandType captionUnassign(
      final LImageID image,
      final LImageCaptionID caption)
    {
      Objects.requireNonNull(image, "image");
      Objects.requireNonNull(caption, "caption");
      return new LOpCaptionUnassign(this, image, caption);
    }

    @Override
    public LImageSetCommandType captionPriorityIncrease(
      final LImageID image,
      final LImageCaptionID caption)
    {
      Objects.requireNonNull(image, "image");
      Objects.requireNonNull(caption, "caption");
      return new LOpCaptionPriorityIncrease(this, image, caption);
    }

    @Override
    public LImageSetCommandType captionPriorityDecrease(
      final LImageID image,
      final LImageCaptionID caption)
    {
      Objects.requireNonNull(image, "image");
      Objects.requireNonNull(caption, "caption");
      return new LOpCaptionPriorityDecrease(this, image, caption);
    }

    @Override
    public void close()
    {
      this.events.close();
    }
  }

  private static abstract class LOp implements LImageSetCommandType
  {
    private boolean executed;

    LOp()
    {
      this.executed = false;
    }

    protected abstract void onExecute()
      throws LImageSetCommandException;

    protected abstract void onUndo()
      throws LImageSetCommandException;

    @Override
    public final void execute()
      throws LImageSetCommandException
    {
      try {
        this.onExecute();
        this.executed = true;
      } catch (final LImageSetCommandException e) {
        this.executed = false;
        throw e;
      }
    }

    @Override
    public final void undo()
      throws LImageSetCommandException
    {
      if (!this.executed) {
        throw new LImageSetCommandException(
          "Cannot undo a command that hasn't executed.");
      }

      this.onUndo();
      this.executed = false;
    }
  }

  private static final class LOpComposed
    extends LOp
  {
    private final List<LImageSetCommandType> commands;
    private final String description;
    private final ArrayList<LImageSetCommandType> commandsUndo;

    LOpComposed(
      final String inDescription,
      final List<LImageSetCommandType> inCommands)
    {
      this.description =
        Objects.requireNonNull(inDescription, "description");
      this.commands =
        List.copyOf(inCommands);
      this.commandsUndo =
        new ArrayList<>(this.commands.size());
    }

    @Override
    public String description()
    {
      return this.description;
    }

    @Override
    protected void onExecute()
      throws LImageSetCommandException
    {
      this.commandsUndo.clear();

      for (final var c : this.commands) {
        c.execute();
        this.commandsUndo.addFirst(c);
      }
    }

    @Override
    protected void onUndo()
      throws LImageSetCommandException
    {
      for (final var c : this.commandsUndo) {
        c.execute();
      }
    }
  }

  private static final class LOpCaptionUpdate
    extends LOp
  {
    private final LImageCaption caption;
    private final LImageSet imageSet;
    private LImageCaption existing;

    LOpCaptionUpdate(
      final LImageSet inImageSet,
      final LImageCaption inCaption)
    {
      this.imageSet =
        Objects.requireNonNull(inImageSet, "imageSet");
      this.caption =
        Objects.requireNonNull(inCaption, "caption");
    }

    @Override
    public String description()
    {
      return "Update caption";
    }

    @Override
    protected void onExecute()
      throws LImageSetCommandException
    {
      final var id = this.imageSet.captionTexts.get(this.caption.text());
      if (!Objects.equals(id, this.caption.id()) && (id != null)) {
        throw new LImageSetCommandException(
          "Duplicate caption text: Caption %s already has text '%s'"
            .formatted(id, this.caption.text())
        );
      }

      this.existing = this.imageSet.captions.get(this.caption.id());
      this.imageSet.captions.put(this.caption.id(), this.caption);
      this.imageSet.captionTexts.put(this.caption.text(), this.caption.id());
      this.imageSet.events.submit(new LCaptionUpdated(this.caption));
    }

    @Override
    protected void onUndo()
    {
      this.imageSet.captions.remove(this.caption.id());
      this.imageSet.captionTexts.removeValue(this.caption.id());

      final var e = this.existing;
      if (e != null) {
        this.imageSet.captions.put(e.id(), e);
        this.imageSet.captionTexts.put(e.text(), e.id());
        this.imageSet.events.submit(new LCaptionUpdated(e));
      } else {
        this.imageSet.events.submit(new LCaptionDeleted(this.caption.id()));
      }
    }
  }

  private static final class LOpCaptionRemove
    extends LOp
  {
    private final LImageCaptionID caption;
    private final LImageSet imageSet;
    private LImageCaption existingCaption;
    private List<LImage> existingImages;
    private long existingCount;

    LOpCaptionRemove(
      final LImageSet inImageSet,
      final LImageCaptionID inCaption)
    {
      this.imageSet =
        Objects.requireNonNull(inImageSet, "imageSet");
      this.caption =
        Objects.requireNonNull(inCaption, "caption");
      this.existingImages =
        List.of();
    }

    @Override
    public String description()
    {
      return "Remove caption";
    }

    @Override
    protected void onExecute()
    {
      this.existingImages =
        this.imageSet.images.values()
          .stream()
          .filter(i -> i.captions().contains(this.caption))
          .collect(Collectors.toList());

      this.existingCaption =
        this.imageSet.captions.remove(this.caption);
      this.existingCount =
        this.imageSet.captionAssignmentCount(this.caption);

      for (final var image : this.existingImages) {
        final var newImage = new LImage(
          image.imageID(),
          image.fileName(),
          image.captions()
            .stream()
            .filter(i -> !Objects.equals(i, this.caption))
            .collect(Collectors.toList())
        );
        this.imageSet.images.put(newImage.imageID(), newImage);
        this.imageSet.events.submit(new LImageUpdated(newImage));
      }

      this.imageSet.captionCounts.remove(this.caption);
      this.imageSet.events.submit(new LCaptionDeleted(this.caption));
    }

    @Override
    protected void onUndo()
    {
      final var e = this.existingCaption;
      if (e != null) {
        this.imageSet.captions.put(e.id(), e);
        this.imageSet.captionTexts.put(e.text(), e.id());
        this.imageSet.captionCounts.put(e.id(), this.existingCount);
        this.imageSet.events.submit(new LCaptionUpdated(e));
      }

      for (final var image : this.existingImages) {
        this.imageSet.images.put(image.imageID(), image);
        this.imageSet.events.submit(new LImageUpdated(image));
      }
    }
  }

  private static final class LOpImageUpdate
    extends LOp
  {
    private final LImage image;
    private final LImageSet imageSet;
    private LImage existing;

    LOpImageUpdate(
      final LImageSet inImageSet,
      final LImage inImage)
    {
      this.imageSet =
        Objects.requireNonNull(inImageSet, "imageSet");
      this.image =
        Objects.requireNonNull(inImage, "caption");
    }

    @Override
    public String description()
    {
      return "Update image";
    }

    @Override
    protected void onExecute()
      throws LImageSetCommandException
    {
      for (final var caption : this.image.captions()) {
        if (!this.imageSet.captions.containsKey(caption)) {
          throw new LImageSetCommandException(
            "No such caption: %s".formatted(caption)
          );
        }
      }

      this.existing = this.imageSet.images.get(this.image.imageID());
      if (this.existing != null) {
        for (final var cap : this.existing.captions()) {
          this.imageSet.captionCountDecrement(cap);
        }
      }

      this.imageSet.images.put(this.image.imageID(), this.image);
      for (final var cap : this.image.captions()) {
        this.imageSet.captionCountIncrement(cap);
      }
      this.imageSet.events.submit(new LImageUpdated(this.image));
    }

    @Override
    protected void onUndo()
    {
      this.imageSet.images.remove(this.image.imageID());
      for (final var cap : this.image.captions()) {
        this.imageSet.captionCountDecrement(cap);
      }

      final var e = this.existing;
      if (e != null) {
        this.imageSet.images.put(e.imageID(), e);
        for (final var cap : e.captions()) {
          this.imageSet.captionCountIncrement(cap);
        }
        this.imageSet.events.submit(new LImageUpdated(e));
      } else {
        this.imageSet.events.submit(new LImageRemoved(this.image.imageID()));
      }
    }
  }

  private static final class LOpCaptionAssign
    extends LOp
  {
    private final LImageCaptionID caption;
    private final LImageSet imageSet;
    private final LImageID image;
    private LImage existingImage;

    LOpCaptionAssign(
      final LImageSet inImageSet,
      final LImageID inImage,
      final LImageCaptionID inCaption)
    {
      this.imageSet =
        Objects.requireNonNull(inImageSet, "imageSet");
      this.image =
        Objects.requireNonNull(inImage, "image");
      this.caption =
        Objects.requireNonNull(inCaption, "caption");
    }

    @Override
    public String description()
    {
      return "Assign caption";
    }

    @Override
    protected void onExecute()
    {
      this.existingImage = this.imageSet.images.get(this.image);
      if (this.existingImage.captions().contains(this.caption)) {
        return;
      }

      final var newCaptions =
        new LinkedList<>(this.existingImage.captions());

      newCaptions.addFirst(this.caption);

      final var newImage =
        new LImage(
          this.existingImage.imageID(),
          this.existingImage.fileName(),
          newCaptions
      );

      this.imageSet.images.put(newImage.imageID(), newImage);
      this.imageSet.captionCountIncrement(this.caption);
      this.imageSet.events.submit(new LImageUpdated(newImage));
    }

    @Override
    protected void onUndo()
    {
      final var i = this.existingImage;
      this.imageSet.images.put(i.imageID(), i);
      this.imageSet.captionCountDecrement(this.caption);
      this.imageSet.events.submit(new LImageUpdated(i));
    }
  }

  private static final class LOpCaptionUnassign
    extends LOp
  {
    private final LImageCaptionID caption;
    private final LImageSet imageSet;
    private final LImageID image;
    private LImage existingImage;

    LOpCaptionUnassign(
      final LImageSet inImageSet,
      final LImageID inImage,
      final LImageCaptionID inCaption)
    {
      this.imageSet =
        Objects.requireNonNull(inImageSet, "imageSet");
      this.image =
        Objects.requireNonNull(inImage, "image");
      this.caption =
        Objects.requireNonNull(inCaption, "caption");
    }

    @Override
    public String description()
    {
      return "Unassign caption";
    }

    @Override
    protected void onExecute()
    {
      this.existingImage = this.imageSet.images.get(this.image);
      if (!this.existingImage.captions().contains(this.caption)) {
        return;
      }

      final var newCaptions =
        new LinkedList<>(this.existingImage.captions());

      newCaptions.removeIf(captionID -> {
        return Objects.equals(captionID, this.caption);
      });

      final var newImage =
        new LImage(
          this.existingImage.imageID(),
          this.existingImage.fileName(),
          newCaptions
        );

      this.imageSet.images.put(newImage.imageID(), newImage);
      this.imageSet.captionCountDecrement(this.caption);
      this.imageSet.events.submit(new LImageUpdated(newImage));
    }

    @Override
    protected void onUndo()
    {
      final var i = this.existingImage;
      this.imageSet.images.put(i.imageID(), i);
      this.imageSet.captionCountIncrement(this.caption);
      this.imageSet.events.submit(new LImageUpdated(i));
    }
  }

  private static final class LOpCaptionPriorityIncrease
    extends LOp
  {
    private final LImageCaptionID caption;
    private final LImageSet imageSet;
    private final LImageID image;
    private LImage existingImage;

    LOpCaptionPriorityIncrease(
      final LImageSet inImageSet,
      final LImageID inImage,
      final LImageCaptionID inCaption)
    {
      this.imageSet =
        Objects.requireNonNull(inImageSet, "imageSet");
      this.image =
        Objects.requireNonNull(inImage, "image");
      this.caption =
        Objects.requireNonNull(inCaption, "caption");
    }

    @Override
    public String description()
    {
      return "Increase caption priority";
    }

    @Override
    protected void onExecute()
    {
      this.existingImage = this.imageSet.images.get(this.image);
      if (!this.existingImage.captions().contains(this.caption)) {
        return;
      }

      final var newCaptions =
        new LinkedList<>(this.existingImage.captions());

      var existingIndex = -1;
      for (int index = 0; index < newCaptions.size(); ++index) {
        final var newCaption = newCaptions.get(index);
        if (Objects.equals(newCaption, this.caption)) {
          existingIndex = index;
          break;
        }
      }

      if (existingIndex == -1) {
        return;
      }

      newCaptions.remove(existingIndex);
      newCaptions.add(Math.max(0, existingIndex - 1), this.caption);

      final var newImage =
        new LImage(
          this.existingImage.imageID(),
          this.existingImage.fileName(),
          newCaptions
        );

      this.imageSet.images.put(newImage.imageID(), newImage);
      this.imageSet.events.submit(new LImageUpdated(newImage));
    }

    @Override
    protected void onUndo()
    {
      final var i = this.existingImage;
      this.imageSet.images.put(i.imageID(), i);
      this.imageSet.events.submit(new LImageUpdated(i));
    }
  }

  private static final class LOpCaptionPriorityDecrease
    extends LOp
  {
    private final LImageCaptionID caption;
    private final LImageSet imageSet;
    private final LImageID image;
    private LImage existingImage;

    LOpCaptionPriorityDecrease(
      final LImageSet inImageSet,
      final LImageID inImage,
      final LImageCaptionID inCaption)
    {
      this.imageSet =
        Objects.requireNonNull(inImageSet, "imageSet");
      this.image =
        Objects.requireNonNull(inImage, "image");
      this.caption =
        Objects.requireNonNull(inCaption, "caption");
    }

    @Override
    public String description()
    {
      return "Decrease caption priority";
    }

    @Override
    protected void onExecute()
    {
      this.existingImage = this.imageSet.images.get(this.image);
      if (!this.existingImage.captions().contains(this.caption)) {
        return;
      }

      final var newCaptions =
        new LinkedList<>(this.existingImage.captions());

      var existingIndex = -1;
      for (int index = 0; index < newCaptions.size(); ++index) {
        final var newCaption = newCaptions.get(index);
        if (Objects.equals(newCaption, this.caption)) {
          existingIndex = index;
          break;
        }
      }

      if (existingIndex == -1) {
        return;
      }

      newCaptions.remove(existingIndex);

      final int newIndex = existingIndex + 1;
      if (newIndex >= newCaptions.size()) {
        newCaptions.add(this.caption);
      } else {
        newCaptions.add(newIndex, this.caption);
      }

      final var newImage =
        new LImage(
          this.existingImage.imageID(),
          this.existingImage.fileName(),
          newCaptions
        );

      this.imageSet.images.put(newImage.imageID(), newImage);
      this.imageSet.events.submit(new LImageUpdated(newImage));
    }

    @Override
    protected void onUndo()
    {
      final var i = this.existingImage;
      this.imageSet.images.put(i.imageID(), i);
      this.imageSet.events.submit(new LImageUpdated(i));
    }
  }
}
