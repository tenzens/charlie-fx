package com.tenzens.charliefx.Repository.Entries;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class ConnectionStatus {
    private final BooleanProperty connected;
    private String exceptionMessage;

    public ConnectionStatus(boolean connected, String exceptionMessage) {
        this.connected = new SimpleBooleanProperty(connected);
        this.exceptionMessage = exceptionMessage;
    }

    public boolean isConnected() {
        return connected.get();
    }

    public void setConnected(boolean connected) {
        this.connected.set(connected);
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public BooleanProperty getConnectionProperty() {
        return connected;
    }
}
