package com.tenzens.charliefx.Repository.Entries.RemoteDataSource;

import com.tenzens.charliefx.Repository.Model.Change;
import com.tenzens.charliefx.Repository.Model.Commit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.annotations.*;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.List;

@WebSocket
public class DownstreamCommandsListener implements WebSocketListener {
    private final Gson gson;
    private final DownstreamCommandsHandler downstreamCommandsHandler;

    private boolean readInitCommandResponse;
    private Session session;

    public static class DownstreamConnectionException extends Exception {
        public DownstreamConnectionException(final String message) {
            super(message);
        }
    }

    public interface DownstreamCommandsHandler {
        void onDownstreamError(Exception exception);
        void onDownstreamFileRequest(String fileId, FileRequestResponseHandler fileRequestResponseHandler) throws Exception;
        void onDownstreamChanges(List<Change> changes) throws Exception;
    }

    public interface FileRequestResponseHandler {
        void onFileRequestResponse(ApiResponse apiResponse);
    }

    public static class ApiResponse {
        private final int code;
        private final String message;

        public ApiResponse(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public String toString() {
            return code + " " + message;
        }
    }

    public DownstreamCommandsListener(Gson gson, DownstreamCommandsHandler downstreamCommandsHandler) {
        this.downstreamCommandsHandler = downstreamCommandsHandler;
        this.gson = gson;
        this.readInitCommandResponse = false;
    }

    @OnWebSocketClose
    public void onWebSocketClose(int i, String s) {
        this.downstreamCommandsHandler.onDownstreamError(new DownstreamConnectionException("Connection issue, no longer connected"));
    }

    @OnWebSocketConnect
    public void onWebSocketConnect(Session session) {
        this.session = session;

        session.setIdleTimeout(Duration.ZERO);

        try {
            session.getRemote().sendString("PUSHABLE");
        } catch (IOException e) {
            e.printStackTrace();
            session.close(500, "INTERNAL CLIENT ERROR");
        }
    }

    @OnWebSocketError
    public void onWebSocketError(Throwable throwable) {
        this.downstreamCommandsHandler.onDownstreamError((Exception) throwable);
    }

    @OnWebSocketMessage
    public void onWebSocketText(String text) {
        if (!this.readInitCommandResponse) {
            if (!text.equals("200 OK")) {
                session.close(500, "unexpected response");
            }

            this.readInitCommandResponse = true;
            return;
        }

        System.out.println("got message " + text);
        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            // Read the first line
            String command = reader.readLine();
            var body = new char[text.length() - command.length() - 1];
            reader.read(body);

            if (command.equals("PUSH")) {
                handlePush(new String(body));
                return;
            }

            if (command.matches("^FILEREQUEST .*$")) {
                handleFileRequest(command.split(" ")[1]);
                return;
            }

            session.getRemote().sendString("400 Bad Request");
        } catch (IOException e) {
            e.printStackTrace();
            try {
                session.getRemote().sendString("500 Internal Server Error");
            }catch (Exception ex) {
                ex.printStackTrace();
                session.close(500, "INTERNAL CLIENT ERROR");
            }
        }
    }


    private void handleFileRequest(String fileId) {
        try {
            this.downstreamCommandsHandler.onDownstreamFileRequest(fileId, (ApiResponse apiResponse) -> {
                try {
                    session.getRemote().sendString(apiResponse.toString());
                } catch (IOException e) {
                    session.close(500, "INTERNAL CLIENT ERROR");
                }
            });
        }catch (Exception e) {
            e.printStackTrace();

            try {
                session.getRemote().sendString("500 INTERNAL CLIENT ERROR " + e.getMessage());
            }catch (Exception ex) {
                session.close(500, "INTERNAL CLIENT ERROR");
            }
        }
    }

    private void handlePush(String body) {
        Commit commit = gson.fromJson(body, Commit.class);

        try {
            this.downstreamCommandsHandler.onDownstreamChanges(commit.getChanges());
            session.getRemote().sendString("200 OK");
        }catch (Exception e) {
            try {
                session.getRemote().sendString("500 INTERNAL CLIENT ERROR");
            }catch (Exception ex) {
                ex.printStackTrace();
                session.close(500, "INTERNAL CLIENT ERROR");
            }
        }
    }
}
