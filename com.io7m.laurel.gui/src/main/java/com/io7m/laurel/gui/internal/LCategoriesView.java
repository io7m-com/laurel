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


package com.io7m.laurel.gui.internal;

import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.laurel.filemodel.LCategoryCaptionsAssignment;
import com.io7m.laurel.filemodel.LFileModelType;
import com.io7m.laurel.model.LCaption;
import com.io7m.laurel.model.LCategory;
import com.io7m.laurel.model.LCategoryName;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The categories view.
 */

public final class LCategoriesView extends LAbstractViewWithModel
{
  private final LCategoryEditors editors;

  @FXML private TableView<LCategory> categoryList;
  @FXML private TableView<LCaption> captionsAssigned;
  @FXML private TableView<LCaption> captionsUnassigned;
  @FXML private Button categoryAdd;
  @FXML private Button categoryRemove;
  @FXML private Button captionAssign;
  @FXML private Button captionUnassign;
  @FXML private Button categoryRequire;
  @FXML private Button categoryUnrequire;

  LCategoriesView(
    final RPServiceDirectoryType services,
    final LFileModelScope fileModel)
  {
    super(fileModel);

    this.editors =
      services.requireService(LCategoryEditors.class);
  }

  @FXML
  private void onCategoryAddSelected()
  {
    final var editor =
      this.editors.open("");
    final var result =
      editor.result();

    if (result.isPresent()) {
      final var text = result.get();
      this.fileModelNow().categoryAdd(new LCategoryName(text));
    }
  }

  @FXML
  private void onCategoryRemoveSelected()
  {

  }

  @FXML
  private void onCategoryRequireSelected()
  {
    final var category =
      this.categoryList.getSelectionModel()
        .getSelectedItem();

    this.fileModelNow()
      .categorySetRequired(Set.of(category.id()));
  }

  @FXML
  private void onCategoryUnrequireSelected()
  {
    final var category =
      this.categoryList.getSelectionModel()
        .getSelectedItem();

    this.fileModelNow()
      .categorySetNotRequired(Set.of(category.id()));
  }

  @FXML
  private void onCategoryAssignSelected()
  {
    final var category =
      this.categoryList.getSelectionModel()
        .getSelectedItem();

    final var captions =
      this.captionsUnassigned.getSelectionModel()
        .getSelectedItems();

    this.fileModelNow()
      .categoryCaptionsAssign(
        List.of(
          new LCategoryCaptionsAssignment(
            category.id(),
            captions.stream()
              .map(LCaption::id)
              .toList()
          )
        )
      );
  }

  @FXML
  private void onCategoryUnassignSelected()
  {
    final var category =
      this.categoryList.getSelectionModel()
        .getSelectedItem();

    final var captions =
      this.captionsAssigned.getSelectionModel()
        .getSelectedItems();

    this.fileModelNow()
      .categoryCaptionsUnassign(
        List.of(
          new LCategoryCaptionsAssignment(
            category.id(),
            captions.stream()
              .map(LCaption::id)
              .toList()
          )
        )
      );
  }

  @Override
  protected void onInitialize()
  {
    this.initializeCategoriesTable();
    this.initializeCaptionsAssignedTable();
    this.initializeCaptionsUnassignedTable();
  }

  private void initializeCaptionsUnassignedTable()
  {
    this.captionsUnassigned.setEditable(false);

    final var columns =
      this.captionsUnassigned.getColumns();

    final var colText = (TableColumn<LCaption, String>) columns.get(0);
    colText.setSortable(true);
    colText.setReorderable(false);
    colText.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(param.getValue().name().text());
    });

    this.captionsUnassigned.getSelectionModel()
      .setSelectionMode(SelectionMode.MULTIPLE);
    this.captionsUnassigned.getSelectionModel()
      .selectedItemProperty()
      .subscribe(this::onCaptionsUnassignedSelectionChanged);

    this.captionsUnassigned.setPlaceholder(new Label(""));
    this.captionsUnassigned.getSortOrder().add(colText);
    this.captionsUnassigned.sort();
  }

  private void initializeCaptionsAssignedTable()
  {
    this.captionsAssigned.setEditable(false);

    final var columns =
      this.captionsAssigned.getColumns();

    final var colText = (TableColumn<LCaption, String>) columns.get(0);
    colText.setSortable(true);
    colText.setReorderable(false);
    colText.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(param.getValue().name().text());
    });

    this.captionsAssigned.getSelectionModel()
      .setSelectionMode(SelectionMode.MULTIPLE);
    this.captionsAssigned.getSelectionModel()
      .selectedItemProperty()
      .subscribe(this::onCaptionsAssignedSelectionChanged);

    this.captionsAssigned.setPlaceholder(new Label(""));
    this.captionsAssigned.getSortOrder().add(colText);
    this.captionsAssigned.sort();
  }

  private void initializeCategoriesTable()
  {
    this.categoryList.setEditable(false);

    final var columns =
      this.categoryList.getColumns();

    final var colName = (TableColumn<LCategory, String>) columns.get(0);
    colName.setSortable(true);
    colName.setReorderable(false);
    colName.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(param.getValue().name().text());
    });

    final var colReq = (TableColumn<LCategory, Boolean>) columns.get(1);
    colReq.setSortable(true);
    colReq.setReorderable(false);
    colReq.setCellValueFactory(param -> {
      return new ReadOnlyObjectWrapper<>(param.getValue().required());
    });

    this.categoryList.getSelectionModel()
      .setSelectionMode(SelectionMode.SINGLE);
    this.categoryList.getSelectionModel()
      .selectedItemProperty()
      .subscribe(this::onCategorySelectionChanged);

    this.categoryList.setPlaceholder(new Label(""));
    this.categoryList.getSortOrder().add(colName);
    this.categoryList.sort();
  }

  private void onCategorySelectionChanged()
  {
    final var selected =
      Optional.ofNullable(
        this.categoryList.getSelectionModel()
          .getSelectedItem()
      ).map(LCategory::id);

    this.fileModelNow().categorySelect(selected);
    this.categoryRemove.setDisable(selected.isEmpty());
    this.categoryRequire.setDisable(selected.isEmpty());
    this.categoryUnrequire.setDisable(selected.isEmpty());
  }

  private void onCaptionsUnassignedSelectionChanged()
  {
    final var selected =
      this.captionsUnassigned.getSelectionModel()
        .getSelectedItems();

    if (selected.isEmpty()) {
      this.captionAssign.setDisable(true);
      return;
    }

    this.captionAssign.setDisable(false);
  }

  private void onCaptionsAssignedSelectionChanged()
  {
    final var selected =
      this.captionsAssigned.getSelectionModel()
        .getSelectedItems();

    if (selected.isEmpty()) {
      this.captionUnassign.setDisable(true);
      return;
    }

    this.captionUnassign.setDisable(false);
  }

  @Override
  protected void onFileBecameUnavailable()
  {

  }

  @Override
  protected void onFileBecameAvailable(
    final CloseableCollectionType<?> subscriptions,
    final LFileModelType model)
  {
    subscriptions.add(
      model.categoryList()
        .subscribe((oldValue, newValue) -> {
          this.categoryList.setItems(FXCollections.observableList(newValue));
        })
    );

    subscriptions.add(
      model.categoryCaptionsAssigned()
        .subscribe((oldValue, newValue) -> {
          this.captionsAssigned.setItems(FXCollections.observableList(newValue));
        })
    );

    subscriptions.add(
      model.categoryCaptionsUnassigned()
        .subscribe((oldValue, newValue) -> {
          this.captionsUnassigned.setItems(FXCollections.observableList(newValue));
        })
    );
  }
}
