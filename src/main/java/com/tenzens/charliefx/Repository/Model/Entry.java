package com.tenzens.charliefx.Repository.Model;

import com.google.gson.annotations.SerializedName;
import com.tenzens.charliefx.Repository.Entries.Model.ClientEntry;

public class Entry {

    @SerializedName(value = "id")
    private final String uuid;

    @SerializedName(value = "device_id")
    private final String deviceUUID;

    private final long createdAt;

    private final EntryBody body;

    public Entry(String uuid, String deviceId, long createdAt, EntryBody body) {
        this.uuid = uuid;
        this.deviceUUID = deviceId;
        this.createdAt = createdAt;
        this.body = body;
    }

    public String getUUID() {
        return uuid;
    }

    public String getDeviceUUID() {
        return deviceUUID;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public EntryBody getBody() {
        return body;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Entry)) {
            return false;
        }
        return uuid.equals(((Entry) obj).getUUID());
    }
}
