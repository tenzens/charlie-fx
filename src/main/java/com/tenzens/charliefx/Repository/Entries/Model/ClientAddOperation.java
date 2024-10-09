package com.tenzens.charliefx.Repository.Entries.Model;

import com.tenzens.charliefx.Repository.Model.AddOperation;

public class ClientAddOperation extends AddOperation {
    private final ClientEntry clientEntry;

    public ClientAddOperation(ClientEntry clientEntry) {
        super(clientEntry.getEntry());

        this.clientEntry = clientEntry;
    }

    public ClientEntry getClientEntry() {
        return clientEntry;
    }
}
