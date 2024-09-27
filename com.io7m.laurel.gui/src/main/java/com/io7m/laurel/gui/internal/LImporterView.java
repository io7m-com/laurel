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

import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import com.io7m.jwheatsheaf.api.JWFileChooserAction;
import com.io7m.jwheatsheaf.api.JWFileChooserConfiguration;
import com.io7m.laurel.filemodel.LFileModelEvent;
import com.io7m.laurel.filemodel.LFileModelEventError;
import com.io7m.laurel.filemodel.LFileModelEventType;
import com.io7m.laurel.filemodel.LFileModels;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.laurel.gui.internal.LStringConstants.TITLE;

/**
 * The importer view.
 */

public final class LImporterView implements LViewType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LImporterView.class);

  private final LFileChoosersType fileChoosers;
  private final Stage stage;
  private final CloseableCollectionType<ClosingResourceFailedException> resources;
  private final AtomicBoolean running;
  private final RPServiceDirectoryType services;

  @FXML private Button select;
  @FXML private Button create;
  @FXML private Button importButton;
  @FXML private TextField directoryField;
  @FXML private TextField fileField;
  @FXML private TextArea exceptionArea;
  @FXML private ProgressBar progress;
  @FXML private TableView<Map.Entry<String, String>> attributeTable;
  @FXML private TableColumn<Map.Entry<String, String>, String> attributeName;
  @FXML private TableColumn<Map.Entry<String, String>, String> attributeValue;
  @FXML private ListView<LFileModelEventType> eventList;

  /**
   * The importer view.
   *
   * @param inServices The services
   * @param inStage    The stage
   */

  public LImporterView(
    final RPServiceDirectoryType inServices,
    final Stage inStage)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.fileChoosers =
      inServices.requireService(LFileChoosersType.class);
    this.stage =
      Objects.requireNonNull(inStage, "stage");
    this.resources =
      CloseableCollection.create();
    this.running =
      new AtomicBoolean(false);
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.importButton.setDisable(true);

    this.directoryField.textProperty()
      .addListener((_0, _1, _2) -> this.validate());
    this.fileField.textProperty()
      .addListener((_0, _1, _2) -> this.validate());

    this.attributeTable.setPlaceholder(new Label(""));
    this.attributeName.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(param.getValue().getKey());
    });
    this.attributeValue.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(param.getValue().getValue());
    });

    this.eventList.setCellFactory(v -> new LEventCell(this.services));
    this.eventList.getSelectionModel()
      .selectedItemProperty()
      .addListener((_0, _1, item) -> {
        this.onEventSelectionChanged(item);
      });

    this.stage.setOnHidden(windowEvent -> this.close());
  }

  private void onEventSelectionChanged(
    final LFileModelEventType item)
  {
    switch (item) {
      case null -> {
        this.attributeTable.setItems(FXCollections.emptyObservableList());
        this.exceptionArea.setText("");
      }
      case final LFileModelEvent ignored -> {
        this.attributeTable.setItems(FXCollections.emptyObservableList());
        this.exceptionArea.setText("");
      }
      case final LFileModelEventError error -> {
        this.attributeTable.setItems(
          FXCollections.observableList(
            List.copyOf(error.attributes().entrySet())
          )
        );
        this.exceptionArea.setText(exceptionTextOf(error.exception()));
      }
    }
  }

  private static String exceptionTextOf(
    final Optional<Throwable> exceptionOpt)
  {
    if (exceptionOpt.isEmpty()) {
      return "";
    }

    final var exception = exceptionOpt.get();
    try (var writer = new StringWriter()) {
      try (var printWriter = new PrintWriter(writer)) {
        exception.printStackTrace(printWriter);
        printWriter.flush();
      }
      return writer.toString();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void close()
  {
    try {
      this.resources.close();
    } catch (final ClosingResourceFailedException e) {
      // Nothing we can do!
    }
  }

  private void validate()
  {
    if (this.running.get()) {
      this.create.setDisable(true);
      this.directoryField.setDisable(true);
      this.fileField.setDisable(true);
      this.importButton.setDisable(true);
      this.select.setDisable(true);
      return;
    }

    var ok = !this.fileField.getText().isBlank();
    ok = ok && !this.directoryField.getText().isBlank();
    this.importButton.setDisable(!ok);
    this.create.setDisable(false);
    this.directoryField.setDisable(false);
    this.importButton.setDisable(false);
    this.select.setDisable(false);
  }

  @FXML
  private void onSelectDirectorySelected()
  {
    final var chooser =
      this.fileChoosers.create(
        JWFileChooserConfiguration.builder()
          .setAction(JWFileChooserAction.OPEN_EXISTING_SINGLE)
          .build()
      );

    final var r = chooser.showAndWait();
    if (!r.isEmpty()) {
      this.directoryField.setText(r.get(0).toAbsolutePath().toString());
    }
  }

  @FXML
  private void onCreateFileSelected()
  {
    final var chooser =
      this.fileChoosers.create(
        JWFileChooserConfiguration.builder()
          .setAction(JWFileChooserAction.CREATE)
          .setConfirmFileSelection(true)
          .build()
      );

    final var r = chooser.showAndWait();
    if (!r.isEmpty()) {
      this.fileField.setText(r.get(0).toAbsolutePath().toString());
    }
  }

  @FXML
  private void onImportSelected()
  {
    final var outputFile =
      Paths.get(this.fileField.getText());
    final var sourceDirectory =
      Paths.get(this.directoryField.getText());
    final var importer =
      this.resources.add(LFileModels.createImport(sourceDirectory, outputFile));

    importer.events().subscribe(new LPerpetualSubscriber<>(this::onFileModelEvent));

    final var future = importer.execute();
    future.whenComplete((unused, throwable) -> {
      Platform.runLater(() -> {
        if (throwable == null) {
          try {
            this.onImportCompleted(outputFile);
          } catch (final Exception e) {
            LOG.debug("Failed to open new file: ", e);
          }
        }
      });
    });

    this.running.set(true);
    this.validate();
  }

  private void onImportCompleted(
    final Path outputFile)
    throws Exception
  {
    final var newStage = new Stage();
    LFileView.openForStage(
      this.services,
      LFileModelScope.createNewScope(outputFile),
      newStage
    );
    newStage.show();

    this.stage.toFront();
  }

  private void onFileModelEvent(
    final LFileModelEventType event)
  {
    Platform.runLater(() -> {
      final var progressV = event.progress();
      if (progressV.isEmpty()) {
        this.progress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
      } else {
        this.progress.setProgress(progressV.getAsDouble());
      }
      this.eventList.getItems().add(event);
    });
  }

  @FXML
  private void onCancelSelected()
  {
    this.stage.close();
  }

  /**
   * Open a new view for the given stage.
   *
   * @param services The service directory
   * @param stage    The stage
   *
   * @return A view and stage
   *
   * @throws Exception On errors
   */

  public static LViewAndStage<LImporterView> openForStage(
    final RPServiceDirectoryType services,
    final Stage stage)
    throws Exception
  {
    final var strings =
      services.requireService(LStrings.class);

    final var xml =
      LFileView.class.getResource(
        "/com/io7m/laurel/gui/internal/importer.fxml"
      );
    final var resources =
      strings.resources();
    final var loader =
      new FXMLLoader(xml, resources);

    final LViewControllerFactoryType<LViewType> controllers =
      LViewControllerFactoryMapped.create(
        Map.entry(
          LImporterView.class,
          () -> {
            return new LImporterView(services, stage);
          }
        )
      );

    loader.setControllerFactory(param -> {
      return controllers.call((Class<? extends LViewType>) param);
    });

    final var pane = loader.<Pane>load();
    LCSS.setCSS(pane);
    stage.setScene(new Scene(pane));
    stage.setTitle(strings.format(TITLE));
    stage.setWidth(600.0);
    stage.setHeight(400.0);

    return new LViewAndStage<>(loader.getController(), stage);
  }
}
