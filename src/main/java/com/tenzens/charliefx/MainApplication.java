package com.tenzens.charliefx;

import com.tenzens.charliefx.Views.RootViewController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        RootViewController.OnQuitListener onQuitListener = new RootViewController.OnQuitListener();

        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("layouts/root-view.fxml"));
        Parent rootView = fxmlLoader.load();
        RootViewController rootViewController = fxmlLoader.getController();
        rootViewController.configure(onQuitListener);

        Scene scene = new Scene(rootView, 400, 650);
        scene.getStylesheets().add(MainApplication.class.getResource("styles/styles.css").toExternalForm());

        stage.setTitle("charlie");
        stage.setScene(scene);

        stage.setOnCloseRequest(event -> {
            onQuitListener.onQuit();
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}