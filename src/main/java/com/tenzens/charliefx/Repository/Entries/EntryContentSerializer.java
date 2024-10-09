package com.tenzens.charliefx.Repository.Entries;

import com.tenzens.charliefx.Repository.Model.EntryContent;
import com.tenzens.charliefx.Repository.Model.FileContent;
import com.tenzens.charliefx.Repository.Model.TextContent;

import com.google.gson.*;
import java.lang.reflect.Type;

public class EntryContentSerializer implements JsonSerializer<EntryContent>, JsonDeserializer<EntryContent> {

    @Override
    public JsonElement serialize(EntryContent entryContent, Type type, JsonSerializationContext jsonSerializationContext) {
        switch (entryContent.getType()) {
            case "TEXT":
                return jsonSerializationContext.serialize(entryContent, TextContent.class);

            case "FILE":
                JsonObject fileObjectJson = jsonSerializationContext.serialize(entryContent, FileContent.class).getAsJsonObject();
                fileObjectJson.remove("file_path");
                return fileObjectJson;
        }

        return null;
    }

    @Override
    public EntryContent deserialize (JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String entryType = jsonObject.get("type").getAsString();

        EntryContent content = null;
        switch (entryType) {
            case "TEXT":
                content = context.deserialize(jsonObject, TextContent.class);
                break;
            case "FILE":
                content = context.deserialize(jsonObject, FileContent.class);
                break;
            default:
                throw new JsonParseException("Unknown entry type: " + entryType);
        }

        return content;
    }
}