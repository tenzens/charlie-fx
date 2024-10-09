package com.tenzens.charliefx.Repository.Authentication;

import com.google.gson.annotations.SerializedName;

public class Aloha {
    private String deviceId;

    @SerializedName(value = "auth_token")
    private Session session;

    public Session getSession() {
        return session;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setSession(Session session) {
        this.session = session;
    }
}
