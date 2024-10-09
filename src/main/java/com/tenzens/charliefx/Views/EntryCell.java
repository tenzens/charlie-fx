package com.tenzens.charliefx.Views;

import com.tenzens.charliefx.MainApplication;
import com.tenzens.charliefx.Repository.Entries.Model.ClientEntry;
import com.tenzens.charliefx.Repository.Entries.Model.FileEntry;
import com.tenzens.charliefx.Repository.Entries.Model.TextEntry;
import com.tenzens.charliefx.Repository.Model.FileContent;
import com.tenzens.charliefx.Repository.Model.TextContent;
import com.tenzens.charliefx.Utilities.Utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class EntryCell extends ListCell<ClientEntry> {

    private final ActionEventListener actionEventListener;
    private Node textEntry;
    private Node fileEntry;

    public enum ActionEvent {
        COPY_BUTTON_CLICK,
        DOWNLOAD_BUTTON_CLICK,
        REMOVE_BUTTON_CLICK,
        EDIT_CLICK,
    }

    public interface ActionEventListener {
        void onEntryCellAction(ActionEvent actionEvent, ClientEntry entry);
    }

    public EntryCell(ActionEventListener actionEventListener) {
        this.actionEventListener = actionEventListener;
        try {
            FXMLLoader textEntryLoader = new FXMLLoader(MainApplication.class.getResource("layouts/text-entry.fxml"));
            textEntry = textEntryLoader.load();

            FXMLLoader fileEntryLoader = new FXMLLoader(MainApplication.class.getResource("layouts/file-entry.fxml"));
            fileEntry = fileEntryLoader.load();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void updateItem(ClientEntry clientEntry, boolean empty) {
        super.updateItem(clientEntry, empty);

        if(empty || clientEntry == null) {
            setGraphic(null);
            setText(null);
            return;
        }

        Node entryNode;
        switch(clientEntry.getEntry().getBody().getContent().getType()) {
            case "TEXT":
                setTextEntry(textEntry, (TextEntry) clientEntry);
                entryNode = textEntry;
                break;

            case "FILE":
                setFileEntry(fileEntry, (FileEntry) clientEntry);
                entryNode = fileEntry;
                break;

            default:
                setTextEntry(textEntry, (TextEntry) clientEntry);
                entryNode = textEntry;
                break;
        }

        setGraphic(entryNode);
    }

    private void setEntry(Node guiEntry, ClientEntry clientEntry) {
        Label titleLabel = (Label) guiEntry.lookup("#titleLabel");
        titleLabel.setText(clientEntry.getEntry().getBody().getTitle());

        ((HBox) guiEntry.lookup("#titleHBox")).maxWidthProperty().bind(listViewProperty().get().widthProperty().subtract(50));

        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(clientEntry.getEntry().getCreatedAt()), ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E, dd. MMMM yyyy");
        String formattedDate = localDateTime.format(formatter);

        ((Label) guiEntry.lookup("#dateLabel")).setText(formattedDate);

        Button actionButton = (Button) guiEntry.lookup("#actionButton");
        actionButton.getStyleClass().clear();
        ((SVGPath) actionButton.getGraphic()).setFill(Color.web(clientEntry.getEntry().getBody().getHighlightsColor()));

        ColorAdjust actionButtonAdjust = new ColorAdjust();
        actionButtonAdjust.setBrightness(0); // Initial brightness

        actionButton.setOnMousePressed(event -> {
            actionButtonAdjust.setBrightness(-0.2); // Darken by 20%
            actionButton.setEffect(actionButtonAdjust);
        });

        // Reset the button's color on release
        actionButton.setOnMouseReleased(event -> {
            actionButtonAdjust.setBrightness(0); // Darken by 20%
            actionButton.setEffect(actionButtonAdjust);
        });

        Button removeButton = (Button) guiEntry.lookup("#removeButton");
        removeButton.getStyleClass().add("remove-button");
        ((SVGPath) removeButton.getGraphic()).setFill(Color.web(clientEntry.getEntry().getBody().getHighlightsColor()));
        ((SVGPath) removeButton.getGraphic()).setStroke(Color.TRANSPARENT);

        removeButton.setOnAction(actionEvent -> {
            actionEventListener.onEntryCellAction(ActionEvent.REMOVE_BUTTON_CLICK, clientEntry);
        });

        ColorAdjust removeButtonAdjust = new ColorAdjust();
        removeButtonAdjust.setBrightness(0); // Initial brightness

        removeButton.setOnMousePressed(event -> {
            removeButtonAdjust.setBrightness(-0.2); // Darken by 20%
            removeButton.setEffect(removeButtonAdjust);
        });

        // Reset the button's color on release
        removeButton.setOnMouseReleased(event -> {
            removeButtonAdjust.setBrightness(0); // Darken by 20%
            removeButton.setEffect(removeButtonAdjust);
        });

        var mainBoxStyle = """
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-border-width: 1px;
                -fx-border-radius: 10px;
                -fx-background-radius: 10px;
                """;

        guiEntry.setStyle(String.format(mainBoxStyle, clientEntry.getEntry().getBody().getColor(), clientEntry.getBody().getHighlightsColor()));
    }

    private void setTextEntry(Node textEntryNode, TextEntry textEntry) {
        setEntry(textEntryNode, textEntry);

        var textContent = textEntry.getContent().getText();

        Label mainTextLabel = (Label) textEntryNode.lookup("#mainTextLabel");
        if (textContent.isBlank() || textContent.isEmpty()) {
            mainTextLabel.setVisible(false);
            mainTextLabel.setManaged(false);
        }else {
            mainTextLabel.setVisible(true);
            mainTextLabel.setManaged(true);
            mainTextLabel.setText(textContent);
            mainTextLabel.maxWidthProperty().bind(listViewProperty().get().widthProperty().subtract(50));
        }

        textEntryNode.setOnMouseClicked(event -> {
            actionEventListener.onEntryCellAction(ActionEvent.EDIT_CLICK, textEntry);
        });

        Button copyButton = (Button) textEntryNode.lookup("#actionButton");
        copyButton.getStyleClass().add("copy-button");

        copyButton.setOnAction(actionEvent -> {
            actionEventListener.onEntryCellAction(ActionEvent.COPY_BUTTON_CLICK, textEntry);
        });
    }

    private void setFileEntry(Node fileEntry, FileEntry entry) {
        setEntry(fileEntry, entry);
        fileEntry.setOnMouseClicked((event) -> {});

        Button downloadButton = (Button) fileEntry.lookup("#actionButton");
        ProgressIndicator progressIndicator = (ProgressIndicator) fileEntry.lookup("#progressIndicator");

        if (entry.isLocal()) {
            progressIndicator.visibleProperty().unbind();
            progressIndicator.setVisible(false);
            downloadButton.visibleProperty().unbind();
            downloadButton.visibleProperty().set(false);
        }else {
            progressIndicator.setStyle("-fx-progress-color: " + entry.getBody().getHighlightsColor());
            progressIndicator.visibleProperty().bind(entry.getIsDownloadingProperty());
            progressIndicator.progressProperty().bind(entry.getDownloadProgressProperty());
            downloadButton.visibleProperty().bind(entry.getIsDownloadingProperty().not());
            downloadButton.getStyleClass().add("download-button");

            downloadButton.setOnAction(actionEvent -> {
                actionEventListener.onEntryCellAction(ActionEvent.DOWNLOAD_BUTTON_CLICK, entry);
            });
        }

        SVGPath filePreviewIcon = (SVGPath) fileEntry.lookup("#filePreviewIcon");
        filePreviewIcon.setStroke(Color.web(entry.getBody().getHighlightsColor()));

        Label fileSizeLabel = (Label) fileEntry.lookup("#fileSizeLabel");
        fileSizeLabel.setText(Utils.formatFileSize(((FileContent) entry.getBody().getContent()).getFileSize()));
    }

}
