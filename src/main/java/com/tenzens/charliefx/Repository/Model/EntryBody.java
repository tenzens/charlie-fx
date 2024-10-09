package com.tenzens.charliefx.Repository.Model;

import com.google.gson.annotations.JsonAdapter;
import com.tenzens.charliefx.Repository.Entries.EntryContentSerializer;

public class EntryBody {
    private final String color;
    private final String highlightsColor;

    private final boolean pinned;

    @JsonAdapter(EntryContentSerializer.class)
    private EntryContent content;

    public EntryBody(String color,
                     String highlightsColor,
                     boolean pinned,
                     EntryContent content) {

        this.color = color;
        this.highlightsColor = highlightsColor;
        this.pinned = pinned;
        this.content = content;
    }

    public String getTitle() {
        if (content instanceof TextContent) {
            return ((TextContent) content).getTitle();
        }else if (content instanceof FileContent) {
            return ((FileContent) content).getFileName();
        }else {
            return "";
        }
    }

    public String getColor() {
        return color;
    }

    public String getHighlightsColor() {
        return highlightsColor;
    }

    public boolean isPinned() {
        return pinned;
    }

    public EntryContent getContent() {
        return content;
    }

    public void setContent(EntryContent content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntryBody other)) {
            return false;
        }

        return  other.getContent().equals(this.getContent()) &&
                other.isPinned() == this.isPinned() &&
                other.getColor().equals(this.getColor()) &&
                other.getHighlightsColor().equals(this.getHighlightsColor());
    }
}
