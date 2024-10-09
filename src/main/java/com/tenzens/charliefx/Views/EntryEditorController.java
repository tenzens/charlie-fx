package com.tenzens.charliefx.Views;

import com.tenzens.charliefx.Repository.Entries.Model.TextEntry;
import com.tenzens.charliefx.Repository.Preferences.Preferences;
import com.tenzens.charliefx.Repository.Entries.Model.ClientEntry;
import com.tenzens.charliefx.Repository.Preferences.Model.Color;
import com.tenzens.charliefx.Repository.Model.EntryBody;
import com.tenzens.charliefx.Repository.Model.TextContent;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.util.OptionalInt;
import java.util.stream.IntStream;

public class EntryEditorController {

    @FXML
    private VBox entryEditor;

    @FXML
    private Button colorButton;

    @FXML
    private TextArea textArea;

    @FXML
    private Button applyButton;

    @FXML
    private Button cancelButton;

    @FXML
    private TextField titleTextField;
    private String deviceId;

    public interface EntryEditorCallback {
        void apply(ClientEntry oldEntry, ClientEntry newEntry);
        void cancel();
    }

    private Preferences preferences;
    private EntryEditorCallback callback;
    private Color entryColor;
    private int nextColorIndex;
    private TextEntry clientEntry;

    public void configure(String deviceId, Preferences preferences, EntryEditorCallback entryEditorCallback) {
        this.preferences = preferences;
        this.callback = entryEditorCallback;
        this.deviceId = deviceId;
    }

    public void prepare(TextEntry clientEntry) {
        this.clientEntry = clientEntry;
        var colors = preferences.getColors();

        textArea.setWrapText(true);
        textArea.maxWidthProperty().bind(entryEditor.widthProperty().subtract(35));
        titleTextField.maxWidthProperty().bind(entryEditor.widthProperty());

        if (clientEntry != null) {
            titleTextField.setText(clientEntry.getEntry().getBody().getTitle());
            textArea.setText(((TextContent)clientEntry.getEntry().getBody().getContent()).getText());

            OptionalInt indexOpt = IntStream.range(0, colors.size())
                    .filter(i -> colors.get(i).getColor().equals(clientEntry.getEntry().getBody().getColor()) && colors.get(i).getAccentColor().equals(clientEntry.getEntry().getBody().getHighlightsColor()))  // Replace with your condition
                    .findFirst();

            if (indexOpt.isPresent()) {
                var colorIndex = indexOpt.getAsInt();
                entryColor = colors.get(colorIndex);

                if (colorIndex == colors.size() - 1) {
                    nextColorIndex = 0;
                }else {
                    nextColorIndex = colorIndex + 1;
                }
            } else {
                entryColor = new Color(clientEntry.getEntry().getBody().getColor(), clientEntry.getEntry().getBody().getHighlightsColor());
                nextColorIndex = 0;
            }
        }else {
            entryColor = colors.get(0);
            nextColorIndex = 1;

            titleTextField.setText("");
            textArea.setText("");
        }

        var backgroundColorStyle = """
                -fx-background-color: %s;
                """;

        entryEditor.setStyle(String.format(backgroundColorStyle, entryColor.getColor(), entryColor.getAccentColor()));

        var colorButtonStyle = """
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-border-width: 2px;
                -fx-border-radius: 30px;
                -fx-background-radius: 30px;
                """;

        colorButton.setStyle(String.format(colorButtonStyle, colors.get(nextColorIndex).getColor(), colors.get(nextColorIndex).getAccentColor()));

        ((SVGPath) applyButton.getGraphic()).setFill(javafx.scene.paint.Color.web(entryColor.getAccentColor()));
        ((SVGPath) cancelButton.getGraphic()).setStroke(javafx.scene.paint.Color.web(entryColor.getAccentColor()));

        colorButton.setOnAction(event -> {
            entryColor = colors.get(nextColorIndex);
            nextColorIndex = nextColorIndex < colors.size() - 1 ? nextColorIndex + 1 : 0;

            colorButton.setStyle(String.format(colorButtonStyle, colors.get(nextColorIndex).getColor(), colors.get(nextColorIndex).getAccentColor()));
            entryEditor.setStyle(String.format(backgroundColorStyle, entryColor.getColor()));
            ((SVGPath) applyButton.getGraphic()).setFill(javafx.scene.paint.Color.web(entryColor.getAccentColor()));
            ((SVGPath) cancelButton.getGraphic()).setStroke(javafx.scene.paint.Color.web(entryColor.getAccentColor()));
        });

        entryEditor.setOnKeyTyped(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                apply(null);
            }
        });
    }

    public void apply(ActionEvent actionEvent) {
        var newTextEntry = TextEntry.createNewTextEntry(
                deviceId,
                titleTextField.getText(),
                textArea.getText(),
                entryColor.getColor(),
                entryColor.getAccentColor(),
                false
        );

        this.callback.apply(this.clientEntry, newTextEntry);
    }

    public void cancel(ActionEvent actionEvent) {
        callback.cancel();
    }

}
