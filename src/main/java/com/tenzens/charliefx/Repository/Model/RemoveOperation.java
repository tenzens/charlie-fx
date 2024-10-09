package com.tenzens.charliefx.Repository.Model;

public class RemoveOperation extends OperationBody {
    private final String entryUUID;

    public RemoveOperation(String entryUUID) {
        this.entryUUID = entryUUID;
    }

    @Override
    public String getEntryUUID() {
        return entryUUID;
    }
}
