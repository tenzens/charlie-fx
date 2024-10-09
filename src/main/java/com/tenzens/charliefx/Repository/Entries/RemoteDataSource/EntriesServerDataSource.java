package com.tenzens.charliefx.Repository.Entries.RemoteDataSource;

import com.tenzens.charliefx.Quittable;
import com.tenzens.charliefx.Repository.Authentication.AuthenticationRepository;
import com.tenzens.charliefx.Repository.Authentication.RemoteDataSource.ServerAuthenticationSource;
import com.tenzens.charliefx.Repository.Model.*;

import com.google.gson.Gson;
import org.eclipse.jetty.client.*;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamRequestContent;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.client.util.StringRequestContent;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EntriesServerDataSource implements Quittable {

    public static class ServerException extends Exception {
        public ServerException(String message) {
            super(message);
        }
    }

    private final String mainApiEndpoint;
    private final String websocketApiEndpoint;
    private final Gson gson;
    private final HttpClient client;
    private final AuthenticationRepository authenticationRepository;
    private final WebSocketClient webSocketClient;

    public EntriesServerDataSource(Gson gson, HttpClient httpClient, AuthenticationRepository authenticationRepository) {
        this.authenticationRepository = authenticationRepository;
        this.gson = gson;
        this.client = httpClient;
        this.mainApiEndpoint = String.format("%s/api/v1", authenticationRepository.getProfile().getServerUrl());
        this.websocketApiEndpoint = String.format("%s/api/v1/live", authenticationRepository.getProfile().getServerUrl());

        webSocketClient = new WebSocketClient(client);

        try {
            webSocketClient.start();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void live(DownstreamCommandsListener.DownstreamCommandsHandler downstreamCommandsHandler) throws Exception {

        URI websocketApiEndpoint = generateWebsocketApiEndpoint();

        var bearerString = String.format("%s$%s", authenticationRepository.getAloha().getDeviceId(), authenticationRepository.getAloha().getSession().getAccessToken());
        var base64encodedBearerString = Base64.getEncoder().encode(bearerString.getBytes(StandardCharsets.UTF_8));
        var base64BearerString = new String(base64encodedBearerString, StandardCharsets.UTF_8);

        ClientUpgradeRequest customRequest = new ClientUpgradeRequest();
        customRequest.setHeader("Authorization", "Bearer " + base64BearerString);

        webSocketClient.connect(new DownstreamCommandsListener(gson, downstreamCommandsHandler), websocketApiEndpoint, customRequest);
    }

    private URI generateWebsocketApiEndpoint() throws URISyntaxException {
        URI websocketHttpUri = new URI(this.websocketApiEndpoint);

        // Change the scheme to ws or wss depending on the original scheme
        String newScheme = websocketHttpUri.getScheme().equals("https") ? "wss" : "ws";

        // Create a new URI with the updated scheme
        return new URI(newScheme, websocketHttpUri.getUserInfo(), websocketHttpUri.getHost(), websocketHttpUri.getPort(), websocketHttpUri.getPath(), websocketHttpUri.getQuery(), websocketHttpUri.getFragment());
    }

    public void post(List<Change> changes) throws Exception {
        String entriesEndpoint = String.format("%s/entries", mainApiEndpoint);

        var bearerString = String.format("%s$%s", authenticationRepository.getAloha().getDeviceId(), authenticationRepository.getAloha().getSession().getAccessToken());
        var base64encodedBearerString = Base64.getEncoder().encode(bearerString.getBytes(StandardCharsets.UTF_8));
        var base64BearerString = new String(base64encodedBearerString, StandardCharsets.UTF_8);

        String postBody = gson.toJson(new Commit((ArrayList<Change>) changes));

        Request request = client.newRequest(entriesEndpoint)
                .method(HttpMethod.POST)
                .body(new StringRequestContent(postBody, StandardCharsets.UTF_8))
                .headers(headers -> headers
                        .put("Authorization", "Bearer " + base64BearerString));

        ContentResponse response = request.send();

        if (!HttpStatus.isSuccess(response.getStatus())) {
            throw new ServerAuthenticationSource.ServerAuthenticationException("Unexpected code " + response.getStatus() + ": " + response.getContentAsString());
        }
    }

    public interface ProgressCallback {
        void callback(int progress);
    }

    public static class ProgressInputStream extends InputStream {
        private final InputStream in;
        private final ProgressCallback progressCallback;
        private int progress;

        public ProgressInputStream(InputStream in, ProgressCallback callback) {
            this.in = in;
            this.progressCallback = callback;
            this.progress = 0;
        }

        @Override
        public int read() throws IOException {
            return in.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            var bytesRead = in.read(b, off, len);
            progress += bytesRead;
            progressCallback.callback(progress);
            return bytesRead;
        }
    }

    public void getFile(String fileId, String fileName, ProgressCallback progressCallback) throws Exception {
        var filesEndpoint = String.format("%s/files/%s", mainApiEndpoint, fileId);

        var bearerString = String.format("%s$%s", authenticationRepository.getAloha().getDeviceId(), authenticationRepository.getAloha().getSession().getAccessToken());
        var base64encodedBearerString = Base64.getEncoder().encode(bearerString.getBytes(StandardCharsets.UTF_8));
        var base64BearerString = new String(base64encodedBearerString, StandardCharsets.UTF_8);

        Path downloadFilePath = Paths.get(System.getProperty("user.home")).resolve(String.format("Downloads/%s", fileName));

        InputStreamResponseListener listener = new InputStreamResponseListener();

        Request request = client.newRequest(filesEndpoint)
                .method(HttpMethod.GET)
                .headers(headers -> headers
                        .put("Authorization", "Bearer " + base64BearerString));

        request.send(listener);
        Response response = listener.get(20, TimeUnit.SECONDS);

        if (response.getStatus() == HttpStatus.OK_200)
        {
            // Use try-with-resources to close input stream.
            try (InputStream responseContent = new ProgressInputStream(listener.getInputStream(), progressCallback))
            {
                Files.copy(responseContent, downloadFilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
        else
        {
            response.abort(new IOException("Unexpected HTTP response"));
            throw new ServerAuthenticationSource.ServerAuthenticationException("Unexpected code " + response.getStatus() + ": " + response.getReason());
        }
    }

    public void postFile(String fileId, File file) throws Exception {
        var filesEndpoint = String.format("%s/files/%s", mainApiEndpoint, fileId);

        var bearerString = String.format("%s$%s", authenticationRepository.getAloha().getDeviceId(), authenticationRepository.getAloha().getSession().getAccessToken());
        var base64encodedBearerString = Base64.getEncoder().encode(bearerString.getBytes(StandardCharsets.UTF_8));
        var base64BearerString = new String(base64encodedBearerString, StandardCharsets.UTF_8);

        Request request = client.newRequest(filesEndpoint)
                .method(HttpMethod.POST)
                .idleTimeout(30, TimeUnit.SECONDS)
                .body(new InputStreamRequestContent(new FileInputStream(file)))
                .headers(headers -> headers
                        .put("Authorization", "Bearer " + base64BearerString)
                        .put("Content-Type", "application/octet-stream")
                        .put("Content-Length", String.valueOf(file.length())));

        ContentResponse response = request.send();

        if (!HttpStatus.isSuccess(response.getStatus())) {
            throw new ServerAuthenticationSource.ServerAuthenticationException("Unexpected code " + response.getStatus() + ": " + response.getContentAsString());
        }
    }

    public void onQuit() {
        try {
            client.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
