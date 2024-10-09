package com.tenzens.charliefx.Repository.Entries.LocalDataSource;

import com.google.gson.*;

import java.io.File;
import java.lang.reflect.Type;

import com.tenzens.charliefx.Repository.Model.EntryBody;
import com.tenzens.charliefx.Repository.Model.EntryContent;
import com.tenzens.charliefx.Repository.Model.FileContent;

public class PersistedEntryBody {

    private final String color;

    private final String highlightsColor;

    private final boolean pinned;

    private final String contentType;

    private EntryContent content;

    public PersistedEntryBody(EntryBody entryBody) {
        this.color = entryBody.getColor();
        this.highlightsColor = entryBody.getHighlightsColor();
        this.pinned = entryBody.isPinned();

        this.contentType = entryBody.getContent().getType();
        if (entryBody.getContent().getType().equals("TEXT")) {
            this.content = null;
        }else if (entryBody.getContent().getType().equals("FILE")){
            this.content = new PersistedFileContent((FileContent) entryBody.getContent());
        }
    }

    public void setContent(EntryContent content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public EntryContent getContent() {
        return content;
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

    public static class PersistedEntryBodyDeserializer implements JsonSerializer<PersistedEntryBody>, JsonDeserializer<PersistedEntryBody> {

        @Override
        public JsonElement serialize(PersistedEntryBody persistedEntryBody, Type type, JsonSerializationContext context) {
            EntryContent content = persistedEntryBody.getContent();
            persistedEntryBody.setContent(null);

            JsonObject jsonObject = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .setPrettyPrinting()
                    .create()
                    .toJsonTree(persistedEntryBody, PersistedEntryBody.class)
                    .getAsJsonObject();

            persistedEntryBody.setContent(content);

            switch (persistedEntryBody.getContentType()) {
                case "FILE":
                    jsonObject.add("content", context.serialize(persistedEntryBody.getContent(), PersistedFileContent.class));
                    break;

                case "TEXT":
                    break;
            }

            return jsonObject;
        }

        @Override
        public PersistedEntryBody deserialize (JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            String entryType = jsonObject.get("content_type").getAsString();

            EntryContent content = null;
            switch (entryType) {
                case "TEXT":
                    break;

                case "FILE":
                    PersistedFileContent persistedFileContent = context.deserialize(jsonObject.get("content"), PersistedFileContent.class);
                    if (persistedFileContent.getFilePath() != null) {
                        content = new FileContent(persistedFileContent.getFileName(), persistedFileContent.getFileSize(), new File(persistedFileContent.getFilePath()));
                    }else {
                        content = new FileContent(persistedFileContent.getFileName(), persistedFileContent.getFileSize(), null);
                    }
                    break;
                default:
                    throw new JsonParseException("Unknown entry type: " + entryType);
            }

            jsonObject.remove("content");

            PersistedEntryBody entryBody = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .setPrettyPrinting()
                    .create()
                    .fromJson(jsonObject, PersistedEntryBody.class);

            entryBody.setContent(content);

            return entryBody;
        }
    }
}
