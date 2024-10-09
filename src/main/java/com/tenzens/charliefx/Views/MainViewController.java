package com.tenzens.charliefx.Views;

import com.tenzens.charliefx.MainApplication;
import com.tenzens.charliefx.Quittable;
import com.tenzens.charliefx.Repository.Authentication.AuthenticationRepository;
import com.tenzens.charliefx.Repository.Entries.EntriesRepository;
import com.tenzens.charliefx.Repository.Entries.Model.ClientEntry;
import com.tenzens.charliefx.Repository.Entries.Model.FileEntry;
import com.tenzens.charliefx.Repository.Entries.Model.TextEntry;
import com.tenzens.charliefx.Repository.Entries.SearchEntriesRepository;
import com.tenzens.charliefx.Repository.Model.TextContent;
import com.tenzens.charliefx.Repository.Preferences.LocalPreferencesRepository;

import com.google.gson.Gson;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import org.eclipse.jetty.client.HttpClient;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainViewController implements EntryCell.ActionEventListener, Quittable {
    @FXML
    private Button signoutButton;

    @FXML
    private HBox smallErrorBox;

    @FXML
    private Label errorLabel;

    @FXML
    private StackPane mainStackPane;

    @FXML
    private Label numberOfEntriesLabel;

    @FXML
    private Label visibleEntriesInfoLabel;

    @FXML
    private TextField searchField;

    @FXML
    private ListView<ClientEntry> entryListView;

    @FXML
    public ListView<ClientEntry> searchResultListView;

    private Parent entryEditorView;

    private ViewSwitcher viewSwitcher;

    private AuthenticationRepository authenticationRepository;

    private EntriesRepository entriesRepository;

    private SearchEntriesRepository searchEntriesRepository;

    private LocalPreferencesRepository localPreferencesRepository;

    private EntryEditorController entryEditorController;

    private Clipboard clipboard;

    public void configure(ExecutorService executorService, Gson gson, HttpClient client, AuthenticationRepository authenticationRepository, ViewSwitcher viewSwitcher, RootViewController.OnQuitListener onQuitListener) {
        this.authenticationRepository = authenticationRepository;
        this.localPreferencesRepository = new LocalPreferencesRepository();
        this.viewSwitcher = viewSwitcher;
        onQuitListener.setQuittable(this);

        this.entriesRepository = new EntriesRepository(executorService, gson, client, authenticationRepository);
        this.searchEntriesRepository = new SearchEntriesRepository(entriesRepository.getSortedEntries());

        try {
            FXMLLoader entryEditorLoader = new FXMLLoader(MainApplication.class.getResource("layouts/entry-editor.fxml"));
            this.entryEditorView = entryEditorLoader.load();
            this.entryEditorController = entryEditorLoader.getController();
            this.entryEditorController.configure(authenticationRepository.getAloha().getDeviceId(), localPreferencesRepository.loadPreferences(), new EntryEditorController.EntryEditorCallback() {
                @Override
                public void apply(ClientEntry oldClientEntry, ClientEntry newEntry) {
                    if (oldClientEntry == null) {
                        entriesRepository.addEntry(newEntry);
                    }else if (!newEntry.getBody().equals(oldClientEntry.getBody())) {

                        entriesRepository.addEntry(newEntry);
                        entriesRepository.removeEntry(oldClientEntry);
                    }

                    mainStackPane.getChildren().remove(mainStackPane.getChildren().size() - 1);
                }

                @Override
                public void cancel() {
                    mainStackPane.getChildren().remove(mainStackPane.getChildren().size() - 1);
                }
            });

        }catch (IOException e) {
            e.printStackTrace();
        }

        clipboard = Clipboard.getSystemClipboard();

        smallErrorBox.visibleProperty().bind(this.entriesRepository.isConnected().not());
        errorLabel.textProperty().bind(Bindings.createStringBinding(() -> this.entriesRepository.getConnectionStatus().getExceptionMessage(), this.entriesRepository.isConnected()));

        this.entriesRepository.run();

        Callback<ListView<ClientEntry>, ListCell<ClientEntry>> cellFactory = (clientEntryListView) -> new EntryCell(MainViewController.this);

        this.entryListView.setCellFactory(cellFactory);
        this.entryListView.setItems(this.entriesRepository.getSortedEntries());

        this.searchResultListView.setCellFactory(cellFactory);
        this.searchResultListView.setItems(this.searchEntriesRepository.getSearchResult());
        this.searchResultListView.setVisible(false);

        this.numberOfEntriesLabel.textProperty().bind(this.entriesRepository.numberOfEntries().asString("%d Entries"));

        this.searchField.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.ESCAPE && searchField.getText().isEmpty()) {
                entryListView.requestFocus();
                return;
            }else if(event.getCode() == KeyCode.ESCAPE && !searchField.getText().isEmpty()) {
                searchResultListView.requestFocus();
                return;
            }

            if (searchField.getText().isEmpty()) {
                this.numberOfEntriesLabel.textProperty().bind(this.entriesRepository.numberOfEntries().asString("%d Entries"));
                this.visibleEntriesInfoLabel.setText("All entries");

                this.entryListView.setVisible(true);
                this.searchResultListView.setVisible(false);
                return;
            }

            this.numberOfEntriesLabel.textProperty().bind(this.searchEntriesRepository.getNumberOfSearchResultEntries().asString("Found %d entries"));
            this.visibleEntriesInfoLabel.setText("Search result");

            this.entryListView.setVisible(false);
            this.searchResultListView.setVisible(true);

            this.searchEntriesRepository.search(searchField.getText());
        });

        this.entryListView.setOnDragOver(dragEvent -> {
            if (dragEvent.getDragboard().hasFiles()) {
                dragEvent.acceptTransferModes(TransferMode.MOVE);
            }
            dragEvent.consume();
        });

        this.entryListView.setOnDragDropped(event -> {
            var dragBoard = event.getDragboard();

            for (File file : dragBoard.getFiles()) {
                if (file.isFile()) {
                    var randomColor = this.localPreferencesRepository.loadPreferences().getRandomColor();

                    var newFileEntry = FileEntry.createNewFileEntry(
                            this.authenticationRepository.getAloha().getDeviceId(),
                            file.getName(),
                            file.length(),
                            file,
                            randomColor.getColor(),
                            randomColor.getAccentColor(),
                            false
                    );

                    this.entriesRepository.addEntry(
                            newFileEntry
                    );
                }
            }
        });

        this.signoutButton.setOnAction((event) -> {
            this.entriesRepository.onQuit();

            try {
                FXMLLoader loginViewLoader = new FXMLLoader(MainApplication.class.getResource("layouts/login-view.fxml"));
                Parent loginView = loginViewLoader.load();
                LoginViewController loginViewController = loginViewLoader.getController();
                loginViewController.configure(Executors.newCachedThreadPool(), gson, new HttpClient(), viewSwitcher, onQuitListener);

                viewSwitcher.switchView(loginView);
            }catch(Exception e) {
                e.printStackTrace();
            }
        });

        try {
            FXMLLoader emptyEntriesPlaceholderLoader = new FXMLLoader(MainApplication.class.getResource("layouts/empty-placeholder.fxml"));
            Parent emptyEntriesPlaceholder = emptyEntriesPlaceholderLoader.load();

            FXMLLoader emptySearchResultPlaceholderLoader = new FXMLLoader(MainApplication.class.getResource("layouts/empty-search-result-placeholder.fxml"));
            Parent emptySearchResultPlaceholder = emptySearchResultPlaceholderLoader.load();

            this.entryListView.setPlaceholder(emptyEntriesPlaceholder);
            this.searchResultListView.setPlaceholder(emptySearchResultPlaceholder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onAddEntryClicked(ActionEvent actionEvent) {
        entryEditorController.prepare(null);
        mainStackPane.getChildren().add(entryEditorView);
    }

    @Override
    public void onEntryCellAction(EntryCell.ActionEvent actionEvent, ClientEntry entry) {
        switch (actionEvent) {
            case COPY_BUTTON_CLICK:
                ClipboardContent content = new ClipboardContent();
                content.putString(((TextContent)entry.getContent()).getText());
                clipboard.setContent(content);
                break;
            case REMOVE_BUTTON_CLICK:
                entriesRepository.removeEntry(entry);
                break;
            case DOWNLOAD_BUTTON_CLICK:
                entriesRepository.getFile((FileEntry) entry);
                break;
            case EDIT_CLICK:
                entryEditorController.prepare((TextEntry) entry);
                mainStackPane.getChildren().add(entryEditorView);
                break;
        }
    }

    public void onRefreshClicked(ActionEvent actionEvent) {
        this.entriesRepository.reconnectDownstream();
    }

    @Override
    public void onQuit() {
        entriesRepository.onQuit();
    }
}
