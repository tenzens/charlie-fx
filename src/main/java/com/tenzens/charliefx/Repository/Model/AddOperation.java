package com.tenzens.charliefx.Repository.Model;

import com.tenzens.charliefx.Repository.Entries.Model.ClientEntry;

public class AddOperation extends OperationBody {
    private final Entry entry;
    private transient long entryId;

    public AddOperation(Entry entry) {
        this.entry = entry;
    }

    @Override
    public String getEntryUUID() {
        return entry.getUUID();
    }

    public Entry getEntry() {
        return entry;
    }
}
