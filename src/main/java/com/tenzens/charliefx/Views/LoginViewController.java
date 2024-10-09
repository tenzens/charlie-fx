package com.tenzens.charliefx.Views;

import com.tenzens.charliefx.Quittable;
import com.tenzens.charliefx.Repository.Authentication.AuthenticationRepository;
import com.tenzens.charliefx.Repository.Authentication.LocalDataSource.DiskProfile;
import com.tenzens.charliefx.MainApplication;

import com.google.gson.Gson;
import com.tenzens.charliefx.Repository.Authentication.RemoteDataSource.ServerAuthenticationSource;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.eclipse.jetty.client.HttpClient;
import javax.net.ssl.SSLHandshakeException;
import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import java.util.concurrent.ExecutorService;

public class LoginViewController implements Quittable {

    @FXML
    private VBox root;

    @FXML
    private Label serverUrlLabel;

    @FXML
    public TextField serverUrlField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private ViewSwitcher viewSwitcher;
    private RootViewController.OnQuitListener onQuitListener;
    private AuthenticationRepository authenticationRepository;
    private Gson gson;
    private ExecutorService executorService;
    private HttpClient client;

    public void configure(ExecutorService executorService, Gson gson, HttpClient client, ViewSwitcher viewSwitcher, RootViewController.OnQuitListener onQuitListener) {
        this.viewSwitcher = viewSwitcher;
        this.gson = gson;
        this.executorService = executorService;
        this.client = client;

        this.onQuitListener = onQuitListener;
        this.onQuitListener.setQuittable(this);
        this.authenticationRepository = new AuthenticationRepository(executorService, gson, client);

        Platform.runLater(() -> {
            passwordField.getParent().requestFocus(); // Or request focus on another node
        });

        this.authenticationRepository.loadDiskSession(new AuthenticationRepository.DiskSessionCallback() {
            @Override
            public void onSuccess(DiskProfile diskSession) {
                LoginViewController.this.root.getChildren().remove(serverUrlField);
                LoginViewController.this.serverUrlLabel.setText(diskSession.getServerUrl());
            }

            @Override
            public void onFailure(Exception e) {
                Exception thrownException = null;

                if (!(e instanceof NoSuchFileException || e instanceof FileNotFoundException)) {
                    thrownException = e;
                }

                if (thrownException != null) {
                    showError(thrownException.getMessage());
                }
            }
        });
    }

    public void onSignInButtonClicked() {
        if (authenticationRepository.getDiskProfile() != null) {
            String password = passwordField.getText();

            try {
                authenticationRepository.decryptDiskProfile(password);
                switchToMainView();
            }catch (Exception e) {
                showError("Error decrypting stored session: " + e.getMessage());
            }
        }else {
            var serverUrlString = serverUrlField.getText();
            String password = passwordField.getText();
            authenticationRepository.newProfile(serverUrlString, password);
            authenticationRepository.authenticate(new AuthenticationRepository.AuthenticationCallback() {

                @Override
                public void onSuccess() {
                    switchToMainView();
                }

                @Override
                public void onFailure(Exception e) {
                    if (e instanceof ServerAuthenticationSource.ServerAuthenticationException) {
                        showError(e.getMessage());
                    }else {
                        e.printStackTrace();
                        showError("Unknown error occurred: " + e.getMessage());
                    }
                }
            });
        }
    }

    public void switchToMainView() {
        try {
            FXMLLoader mainViewLoader = new FXMLLoader(MainApplication.class.getResource("layouts/main-view.fxml"));
            Parent mainView = mainViewLoader.load();
            MainViewController mainViewController = mainViewLoader.getController();
            mainViewController.configure(executorService, gson, client, authenticationRepository, viewSwitcher, onQuitListener);
            viewSwitcher.switchView(mainView);
        }catch (Exception e) {
            showError(e.getMessage());
        }
    }

    public void showError(String message) {
        errorLabel.visibleProperty().set(true);
        errorLabel.setText(message);
    }

    @Override
    public void onQuit() {
        authenticationRepository.onQuit();
    }
}