package com.tenzens.charliefx.Repository.Authentication;

import com.tenzens.charliefx.Quittable;
import com.tenzens.charliefx.Repository.ApplicationResourceLocations;
import com.tenzens.charliefx.Repository.Authentication.LocalDataSource.DiskProfile;
import com.tenzens.charliefx.Repository.Authentication.LocalDataSource.LocalAuthenticationSource;
import com.tenzens.charliefx.Repository.Authentication.RemoteDataSource.ServerAuthenticationSource;
import com.tenzens.charliefx.Repository.CryptoManager;

import com.google.gson.Gson;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;
import org.eclipse.jetty.client.HttpClient;

import javax.crypto.BadPaddingException;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

public class AuthenticationRepository implements Quittable {

    public interface DiskSessionCallback {
        void onSuccess(DiskProfile session);
        void onFailure(Exception e);
    }

    public interface AuthenticationCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface RefreshCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static class DecryptionException extends Exception {
        public DecryptionException(String message) {
            super(message);
        }
    }

    public static class RepositoryException extends Exception {
        public RepositoryException(String s) {
            super(s);
        }
    }

    private final Gson gson;
    private final LocalAuthenticationSource localAuthenticationSource;
    private final ServerAuthenticationSource serverAuthenticationSource;

    private DiskProfile diskProfile;
    private Profile profile;
    private final CryptoManager cryptoManager;
    private final ExecutorService executorService;

    public AuthenticationRepository(ExecutorService executorService, Gson gson, HttpClient client) {
        this.gson = gson;
        Path profilePath = ApplicationResourceLocations.getApplicationFolderPath().resolve("profile.json");

        this.localAuthenticationSource = new LocalAuthenticationSource(profilePath, gson);
        this.serverAuthenticationSource = new ServerAuthenticationSource(gson, client);
        this.executorService = executorService;
        this.cryptoManager = new CryptoManager();
    }

    public void loadDiskSession(DiskSessionCallback callback) {
        try {
            diskProfile = localAuthenticationSource.loadDiskSession();
            callback.onSuccess(diskProfile);
        }catch(IOException e) {
            callback.onFailure(e);
        }
    }

    public DiskProfile getDiskProfile() {
        return diskProfile;
    }

    public void authenticate(AuthenticationCallback authenticationCallback) {
        Task<Aloha> authenticationTask = new Task<>() {

            @Override
            protected Aloha call() throws Exception {
                Aloha aloha = serverAuthenticationSource.aloha(profile.getServerUrl(), profile.getPassword());
                cryptoManager.generateKeyFromPassword(profile.getPassword());
                String encryptedAloha = cryptoManager.encrypt(gson.toJson(aloha));
                localAuthenticationSource.storeDiskProfile(new DiskProfile(profile.getServerUrl(), encryptedAloha));

                return aloha;
            }
        };

        authenticationTask.setOnFailed((event) -> {
            if (event.getSource().getException() instanceof IllegalArgumentException) {
                authenticationCallback.onFailure(new AuthenticationRepository.RepositoryException("Could not connect to your server, check the provided URL."));
            }else if (event.getSource().getException() instanceof ConnectException) {
                authenticationCallback.onFailure(new AuthenticationRepository.RepositoryException("Could not authenticate the server, check the provided URL or your server's health."));
            }else if (event.getSource().getException() instanceof ServerAuthenticationSource.ServerAuthenticationException) {
                authenticationCallback.onFailure((Exception) event.getSource().getException());
            }else {
                event.getSource().getException().printStackTrace();
                authenticationCallback.onFailure(new Exception("Unknown exception occurred: " + event.getSource().getException().getMessage()));
            }
        });

        authenticationTask.setOnSucceeded((event) -> {
            this.profile.setAloha((Aloha) event.getSource().getValue());

            authenticationCallback.onSuccess();
        });

        executorService.submit(authenticationTask);
    }

    public void decryptDiskProfile(String password) throws Exception{
        cryptoManager.generateKeyFromPassword(password);
        profile = new Profile(diskProfile.getServerUrl(), password);

        String decryptedProfile = "";
        try {
            decryptedProfile = cryptoManager.decrypt(diskProfile.getEncryptedAloha());
        }catch (BadPaddingException e) {
            throw new DecryptionException("Unable to decrypt your session information, check your master password");
        }catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Unable to decrypt your session information: " + e.getMessage());
        }

        profile.setAloha(gson.fromJson(decryptedProfile, Aloha.class));
    }

    public Aloha getAloha() {
        return profile.getAloha();
    }

    public Profile getProfile() {
        return profile;
    }

    public void newProfile(String serverUrl, String password) {
        this.profile = new Profile(serverUrl, password);
    }

    public void scheduleRefresh(RefreshCallback refreshCallback) {
        ScheduledService<Session> scheduledService = new ScheduledService<>() {
            @Override
            protected Task<Session> createTask() {
                return new Task<>() {
                    @Override
                    protected Session call() throws Exception {
                        return serverAuthenticationSource.refresh(profile.getServerUrl(), profile.getAloha().getDeviceId(), profile.getAloha().getSession());
                    }
                };
            }
        };

        scheduledService.setOnSucceeded((event) -> {
            Session session = (Session) event.getSource().getValue();
            profile.getAloha().setSession(session);

            refreshCallback.onSuccess();
        });

        scheduledService.setOnFailed((event) -> {
            if (event.getSource().getException() instanceof ConnectException) {
                refreshCallback.onFailure(new AuthenticationRepository.RepositoryException("Could not connect to your server, check the server's health."));
            }else if (event.getSource().getException() instanceof ServerAuthenticationSource.ServerAuthenticationException){
                refreshCallback.onFailure((Exception) event.getSource().getException());
            }else {
                event.getSource().getException().printStackTrace();
                refreshCallback.onFailure(new Exception("Unknown exception: " + event.getSource().getException().getMessage()));
            }
        });

        double delayMillis = 0.0;
        double millisToExpiration = (profile.getAloha().getSession().getAccessTokenExpiration() - System.currentTimeMillis());

        if (millisToExpiration > 5 * 60 * 1000) {
            delayMillis = millisToExpiration - 5 * 60 * 1000;
        }

        scheduledService.setExecutor(executorService);
        scheduledService.setDelay(Duration.millis(delayMillis));
        scheduledService.setPeriod(Duration.millis(23 * 60 * 1000));
        scheduledService.start();
    }

    @Override
    public void onQuit() {
        executorService.shutdownNow();
        serverAuthenticationSource.onQuit();
    }
}
