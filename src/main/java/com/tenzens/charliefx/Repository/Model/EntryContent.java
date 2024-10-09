package com.tenzens.charliefx.Repository.Model;

public abstract class EntryContent {
    private final String type;

    public EntryContent(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntryContent other)) {
            return false;
        }

        return this.type.equals(other.type);
    }

    public abstract String toSearchableString();
}
