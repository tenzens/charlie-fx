package com.tenzens.charliefx.Views;


import com.tenzens.charliefx.MainApplication;
import com.tenzens.charliefx.Quittable;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import org.eclipse.jetty.client.HttpClient;
import java.util.concurrent.Executors;

public class RootViewController implements ViewSwitcher {
    public static class OnQuitListener implements Quittable {
        private Quittable quittable;

        public void setQuittable(Quittable quittable) {
            this.quittable = quittable;
        }

        @Override
        public void onQuit() {
            this.quittable.onQuit();
        }
    }

    @FXML
    private StackPane rootView;

    private OnQuitListener onQuitListener;

    public void configure(OnQuitListener onQuitListener) throws Exception {
        this.onQuitListener = onQuitListener;

        var executorService = Executors.newCachedThreadPool();
        var gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();

        HttpClient client = new HttpClient();
        client.start();

        FXMLLoader loginViewLoader = new FXMLLoader(MainApplication.class.getResource("layouts/login-view.fxml"));
        Parent loginView = loginViewLoader.load();
        LoginViewController loginViewController = loginViewLoader.getController();
        loginViewController.configure(executorService, gson, client, this, onQuitListener);

        switchView(loginView);
    }

    public void switchView(Parent view) {
        if (!rootView.getChildren().isEmpty()) {
            rootView.getChildren().remove(0);
        }
        rootView.getChildren().add(view);
    }
}
