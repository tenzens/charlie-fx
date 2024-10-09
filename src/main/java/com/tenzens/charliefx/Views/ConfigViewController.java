package com.tenzens.charliefx.Views;

import com.tenzens.charliefx.Quittable;
import com.tenzens.charliefx.Repository.Authentication.RemoteDataSource.ServerAuthenticationSource;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.TransferMode;

public class ConfigViewController implements Quittable {

    private ViewSwitcher viewSwitcher;
    private RootViewController.OnQuitListener onQuitListener;

    @FXML private Label errorLabel;

    @FXML private Parent configView;

    public void configure(ServerAuthenticationSource authenticator, ViewSwitcher viewSwitcher, RootViewController.OnQuitListener onQuitListener) {
        this.viewSwitcher = viewSwitcher;
        this.onQuitListener = onQuitListener;
        this.onQuitListener.setQuittable(this);

        configView.setOnDragOver(dragEvent -> {
            if (dragEvent.getDragboard().hasFiles()) {
                dragEvent.acceptTransferModes(TransferMode.MOVE);
            }
            dragEvent.consume();
        });

        configView.setOnDragDropped(event -> {
            var dragBoard = event.getDragboard();
            var configFile = dragBoard.getFiles().get(0);

            if (!configFile.isFile()) {
                showError("The element you dropped is not a file.");
            }else {
                hideError();
            }
        });
    }

    public void showError(String message) {
        errorLabel.visibleProperty().set(true);
        errorLabel.setText("There was an error: " + message);
    }

    public void hideError() {
        errorLabel.visibleProperty().set(false);
    }

    @Override
    public void onQuit() {

    }
}