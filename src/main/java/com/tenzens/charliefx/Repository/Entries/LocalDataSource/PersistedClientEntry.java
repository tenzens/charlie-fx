package com.tenzens.charliefx.Repository.Entries.LocalDataSource;

public class PersistedClientEntry {
    private final String deliveryStatus;

    private final boolean isLocal;

    private final PersistedEntryBody entryBody;

    public PersistedClientEntry(String deliveryStatus, boolean isLocal, PersistedEntryBody entry) {
        this.deliveryStatus = deliveryStatus;
        this.isLocal = isLocal;
        this.entryBody = entry;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public PersistedEntryBody getEntryBody() {
        return entryBody;
    }
}
