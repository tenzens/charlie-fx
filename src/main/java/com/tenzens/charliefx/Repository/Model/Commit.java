package com.tenzens.charliefx.Repository.Model;

import java.util.ArrayList;
import java.util.List;

public class Commit {

    private final ArrayList<Change> changes;

    public Commit(ArrayList<Change> changes) {
        this.changes = changes;
    }

    public List<Change> getChanges() {
        return changes;
    }
}
