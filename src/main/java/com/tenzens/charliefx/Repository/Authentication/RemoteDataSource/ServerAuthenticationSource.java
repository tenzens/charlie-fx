package com.tenzens.charliefx.Repository.Authentication.RemoteDataSource;

import com.tenzens.charliefx.Quittable;
import com.tenzens.charliefx.Repository.Authentication.Aloha;
import com.tenzens.charliefx.Repository.Authentication.Session;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.FormRequestContent;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.Fields;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ServerAuthenticationSource implements Quittable {
    public static class ServerAuthenticationException extends Exception {
        public ServerAuthenticationException(String message) {
            super(message);
        }
    }

    private final Gson gson;
    private final HttpClient client;

    public ServerAuthenticationSource(Gson gson, HttpClient httpClient) {
        this.gson = gson;
        this.client = httpClient;
    }

    public Aloha aloha(String serverUrl, String password) throws Exception {

        var mainApiEndpoint = String.format("%s/api/v1", serverUrl);
        var alohaEndpoint   = String.format("%s/auth/aloha", mainApiEndpoint);

        Fields userAuth = new Fields();
        userAuth.add("password", password);

        // Make the POST request
        Request request = client.newRequest(alohaEndpoint)
                .method(HttpMethod.POST)
                .body(new FormRequestContent(userAuth));

        ContentResponse response;

        response = request.send();

        if (!HttpStatus.isSuccess(response.getStatus())) {
            if (response.getStatus() == HttpStatus.FORBIDDEN_403) {
                throw new ServerAuthenticationException("The provided master-password is incorrect");
            }

            throw new ServerAuthenticationException("Unexpected response from your server: " + response.getStatus() + ": " + response.getContentAsString());
        }

        String bodyString = new String(response.getContent(), StandardCharsets.UTF_8);

        return gson.fromJson(bodyString, Aloha.class);
    }

    public Session refresh(String serverUrl, String deviceId, Session session) throws Exception {
        var mainApiEndpoint = String.format("%s/api/v1", serverUrl);
        var refreshEndpoint   = String.format("%s/auth/refresh", mainApiEndpoint);

        var bearerString = String.format("%s$%s", deviceId, session.getRefreshToken());
        var base64encodedBearerString = Base64.getEncoder().encode(bearerString.getBytes(StandardCharsets.UTF_8));
        var base64BearerString = new String(base64encodedBearerString, StandardCharsets.UTF_8);

        Request request = client.newRequest(refreshEndpoint)
                .method(HttpMethod.GET)
                .headers(headers -> headers
                        .put("Authorization", "Bearer " + base64BearerString));

        ContentResponse response;
        response = request.send();

        if (!HttpStatus.isSuccess(response.getStatus())) {
            throw new ServerAuthenticationException("Unexpected response from your server: " + response.getStatus() + ": " + response.getContentAsString());
        }

        String bodyString = new String(response.getContent(), StandardCharsets.UTF_8);

        System.out.println("Got refresh: " + bodyString);
        return gson.fromJson(bodyString, Session.class);
    }

    @Override
    public void onQuit() {
        try {
            client.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
