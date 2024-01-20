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


package com.io7m.laurel.gui.internal.model;

import com.io7m.laurel.gui.internal.model.LModelFileStatusType.None;
import com.io7m.laurel.model.LImage;
import com.io7m.laurel.model.LImageCaption;
import com.io7m.laurel.model.LImageCaptionID;
import com.io7m.laurel.model.LImageID;
import com.io7m.laurel.model.LImageSet;
import javafx.beans.Observable;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.util.Subscription;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The model.
 */

public final class LModel implements LModelType
{
  private final FilteredList<LMCaption> imageCaptionsUnassignedFilteredView;
  private final HashMap<String, LImageCaptionID> captionText;
  private final ObservableList<LMCaption> imageCaptionsAssignedView;
  private final ObservableList<LMCaption> imageCaptionsUnassignedView;
  private final ObservableList<LMImage> imagesView;
  private final ObservableMap<LImageCaptionID, LMCaption> captions;
  private final ObservableMap<LImageID, LMImage> images;
  private final SimpleDirectedGraph<LMImageOrCaptionType, LMImageCaption> imageCaptionGraph;
  private final SimpleObjectProperty<LMImage> imageSelected;
  private final SimpleObjectProperty<LModelFileStatusType> fileStatus;
  private final SortedList<LMCaption> imageCaptionsAssignedSortedView;
  private final SortedList<LMCaption> imageCaptionsUnassignedSortedView;
  private final SortedList<LMImage> imagesViewSorted;
  private Subscription imageCaptionSubscription;

  /**
   * The model.
   */

  public LModel()
  {
    this.fileStatus =
      new SimpleObjectProperty<>(None.NONE);
    this.captions =
      FXCollections.observableHashMap();
    this.images =
      FXCollections.observableHashMap();
    this.imageSelected =
      new SimpleObjectProperty<>();
    this.captionText =
      new HashMap<>();
    this.imageCaptionGraph =
      new SimpleDirectedGraph<>(LMImageCaption.class);

    this.imageCaptionsAssignedView =
      FXCollections.observableArrayList(
        param -> {
          return new Observable[]{
            new SimpleObjectProperty<>(param),
          };
        }
      );

    this.imageCaptionsAssignedSortedView =
      new SortedList<>(this.imageCaptionsAssignedView);

    this.imageCaptionsUnassignedView =
      FXCollections.observableArrayList(
        param -> {
          return new Observable[]{
            new SimpleObjectProperty<>(param),
          };
        }
      );

    this.imageCaptionsUnassignedFilteredView =
      new FilteredList<>(this.imageCaptionsUnassignedView);
    this.imageCaptionsUnassignedSortedView =
      new SortedList<>(this.imageCaptionsUnassignedFilteredView);

    this.imageSelected.addListener((observable, oldValue, newValue) -> {
      this.onImageSelectionChanged(oldValue, newValue);
    });

    this.imagesView =
      FXCollections.observableArrayList(
        param -> {
          return new Observable[]{
            new SimpleObjectProperty<>(param),
          };
        }
      );

    this.imagesViewSorted =
      new SortedList<>(this.imagesView);

    this.images.addListener(
      (MapChangeListener<? super LImageID, ? super LMImage>) change -> {
        this.imagesView.setAll(this.images.values());
      });
  }

  /**
   * @return The image/caption graph
   */

  public SimpleDirectedGraph<LMImageOrCaptionType, LMImageCaption> imageCaptionGraph()
  {
    return this.imageCaptionGraph;
  }

  private void onImageSelectionChanged(
    final LMImage imageThen,
    final LMImage imageNow)
  {
    if (this.imageCaptionSubscription != null) {
      this.imageCaptionSubscription.unsubscribe();
    }

    if (imageNow == null) {
      this.imageCaptionsUnassignedView.setAll(this.captions.values());
      this.imageCaptionsAssignedView.clear();
      return;
    }

    this.imageCaptionSubscription =
      imageNow.captions().subscribe(() -> {
        final var unassigned = new HashSet<>(this.captions.values());
        unassigned.removeAll(imageNow.captions());
        this.imageCaptionsAssignedView.setAll(imageNow.captions());
        this.imageCaptionsUnassignedView.setAll(unassigned);
      });

    final var unassigned = new HashSet<>(this.captions.values());
    unassigned.removeAll(imageNow.captions());
    this.imageCaptionsAssignedView.setAll(imageNow.captions());
    this.imageCaptionsUnassignedView.setAll(unassigned);
  }

  @Override
  public SimpleObjectProperty<LModelFileStatusType> fileStatus()
  {
    return this.fileStatus;
  }

  @Override
  public SortedList<LMImage> imagesView()
  {
    return this.imagesViewSorted;
  }

  @Override
  public LImageSet createImageSet()
  {
    final var file =
      switch (this.fileStatus.get()) {
        case final LModelFileStatusType.None none -> {
          throw new IllegalStateException();
        }
        case final LModelFileStatusType.Unsaved unsaved -> {
          yield unsaved.file();
        }
        case final LModelFileStatusType.Saved saved -> {
          yield saved.file();
        }
      };

    final var outCaptions =
      new TreeMap<LImageCaptionID, LImageCaption>();
    final var outImages =
      new TreeMap<LImageID, LImage>();

    for (final var entry : this.captions.entrySet()) {
      final var caption =
        new LImageCaption(
          entry.getKey(),
          entry.getValue().text().getValue()
        );
      outCaptions.put(caption.id(), caption);
    }

    for (final var entry : this.images.entrySet()) {
      final var image = new LImage(
        entry.getKey(),
        file.relativize(entry.getValue().fileName().getValue()).toString(),
        new TreeSet<>(
          entry.getValue().captions()
            .stream()
            .map(LMCaption::id)
            .collect(Collectors.toSet())
        )
      );
      outImages.put(image.imageID(), image);
    }

    return new LImageSet(outCaptions, outImages);
  }

  @Override
  public void replaceWith(
    final Path path,
    final LImageSet newImageSet)
    throws LModelOpException
  {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(newImageSet, "newImageSet");

    this.clear();

    try {
      final var commands = new ArrayList<LModelOpType>();
      for (final var entry : newImageSet.captions().entrySet()) {
        commands.add(
          new LModelOpCaptionCreate(
            this,
            entry.getKey(),
            entry.getValue().text())
        );
      }

      final var directory = path.getParent();

      commands.add(
        new LModelOpImagesCreate(
          this,
          newImageSet.images()
            .values()
            .stream()
            .map(i -> {
              return new LMImageCreate(
                i.imageID(),
                directory.resolve(i.fileName())
              );
            })
            .collect(Collectors.toList())
        )
      );

      for (final var entry : newImageSet.images().entrySet()) {
        final var image = entry.getValue();
        commands.add(
          new LModelOpCaptionsAssign(
            this,
            List.of(image.imageID()),
            List.copyOf(image.captions())
          )
        );
      }

      for (final var command : commands) {
        command.execute();
      }
    } catch (final LModelOpException e) {
      this.clear();
      throw e;
    }

    this.fileStatus.set(new LModelFileStatusType.Saved(path));
    this.onImageSelectionChanged(null, null);
  }

  @Override
  public Optional<Path> imageSelect(
    final Optional<LImageID> imageOpt)
  {
    if (imageOpt.isEmpty()) {
      this.imageSelected.set(null);
      return Optional.empty();
    }

    final var image = this.images.get(imageOpt.get());
    if (image == null) {
      this.imageSelected.set(null);
      return Optional.empty();
    }

    this.imageSelected.set(image);
    return Optional.of(image.fileName().getValue());
  }

  @Override
  public ObservableMap<LImageCaptionID, LMCaption> captions()
  {
    return this.captions;
  }

  @Override
  public ObservableMap<LImageID, LMImage> images()
  {
    return this.images;
  }

  @Override
  public SimpleObjectProperty<LMImage> imageSelected()
  {
    return this.imageSelected;
  }

  @Override
  public void clear()
  {
    final var nodes = Set.copyOf(this.imageCaptionGraph.vertexSet());
    for (final var node : nodes) {
      this.imageCaptionGraph.removeVertex(node);
    }

    this.captions.clear();
    this.captionText.clear();
    this.images.clear();
  }

  @Override
  public SortedList<LMCaption> captionsAssigned()
  {
    return this.imageCaptionsAssignedSortedView;
  }

  @Override
  public SortedList<LMCaption> captionsUnassigned()
  {
    return this.imageCaptionsUnassignedSortedView;
  }

  @Override
  public void captionsUnassignedSetFilter(
    final String text)
  {
    this.imageCaptionsUnassignedFilteredView.setPredicate(caption -> {
      if (text.isEmpty()) {
        return true;
      }

      final var captionUpper =
        caption.text().getValue().toUpperCase(Locale.ROOT);
      final var searchUpper =
        text.toUpperCase(Locale.ROOT);

      return captionUpper.contains(searchUpper);
    });
  }

  /**
   * @return The caption texts
   */

  public HashMap<String, LImageCaptionID> captionTexts()
  {
    return this.captionText;
  }
}
