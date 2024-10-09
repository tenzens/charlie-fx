package com.tenzens.charliefx.Repository.Model;

public class TextContent extends EntryContent {
    private final String title;
    private final String text;

    public TextContent(String title, String text) {
        super("TEXT");
        this.title = title;
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            TextContent other = (TextContent) obj;

            if (other.getTitle().equals(this.getTitle()) && other.getText().equals(this.getText())) {
                return true;
            }

            return false;
        }

        return false;
    }

    @Override
    public String toSearchableString() {
        return title + "\n" + text;
    }
}
