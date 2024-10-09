package com.tenzens.charliefx.Repository.Entries.Model;

import com.tenzens.charliefx.Repository.Model.Entry;
import com.tenzens.charliefx.Repository.Model.EntryBody;
import com.tenzens.charliefx.Repository.Model.EntryContent;
import javafx.beans.property.SimpleStringProperty;

public class ClientEntry {

    public static class DeliveryStatus {
        public static String DELIVERED = "DELIVERED";
        public static String UNDELIVERED = "UNDELIVERED";
        public static String UNSUCCESSFUL_DELIVERY = "UNSUCCESSFUL";
    }

    protected long id; // used for sqlite

    protected final SimpleStringProperty deliveryStatus;

    protected final boolean isLocal;

    protected final Entry entry;

    public ClientEntry(long id, Entry entry, boolean isLocal, String deliveryStatus) {
        this.id = id;
        this.entry = entry;
        this.isLocal = isLocal;
        this.deliveryStatus = new SimpleStringProperty(deliveryStatus);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClientEntry)) {
            return false;
        }
        return entry.equals(((ClientEntry) obj).getEntry());
    }

    public Entry getEntry() {
        return entry;
    }

    public EntryBody getBody() {
        return entry.getBody();
    }

    public String getUUID() {
        return entry.getUUID();
    }

    public String getDeviceUUID() {
        return entry.getDeviceUUID();
    }

    public long getCreatedAt() {
        return entry.getCreatedAt();
    }

    public EntryContent getContent() {
        return entry.getBody().getContent();
    }

    public void setDeliveryStatus(String d) {
        deliveryStatus.set(d);
    }

    public SimpleStringProperty getDeliveryStatus() {
        return deliveryStatus;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
