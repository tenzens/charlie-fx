package com.tenzens.charliefx.Repository.Model;

import com.google.gson.*;
import com.tenzens.charliefx.Repository.Entries.Model.ClientEntry;

import java.lang.reflect.Type;
import java.util.UUID;

public class Change {
    protected transient long id; // for sqlite
    protected final String operation;
    protected final OperationBody body;

    public Change(long id, String operation, OperationBody body) {
        this.id = id;
        this.operation = operation;
        this.body = body;
    }

    public Change(String operation, OperationBody body) {
        this(-1, operation, body);
    }

    public String getOperation() {
        return operation;
    }

    public OperationBody getOperationBody() {
        return body;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public static class ChangeSerializer implements JsonSerializer<Change>, JsonDeserializer<Change> {

        @Override
        public JsonElement serialize(Change change, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("operation", change.getOperation());

            switch (change.getOperation()) {
                case "ADD":
                    jsonObject.add("entry", jsonSerializationContext.serialize(((AddOperation) change.getOperationBody()).getEntry()));
                    break;

                case "REMOVE":
                    jsonObject.addProperty("entry_id", ((RemoveOperation) change.getOperationBody()).getEntryUUID());
                    break;
            }

            return jsonObject;
        }

        @Override
        public Change deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext)  throws JsonParseException {
            String operation = jsonElement.getAsJsonObject().get("operation").getAsString();

            switch (operation) {
                case "ADD":
                    Entry entry = jsonDeserializationContext.deserialize(jsonElement.getAsJsonObject().get("entry").getAsJsonObject(), Entry.class);
                    UUID.fromString(entry.getUUID());

                    return new Change(-1, "ADD", new AddOperation(entry));

                case "REMOVE":
                    String entryUUID = jsonElement.getAsJsonObject().get("entry_id").getAsString();
                    UUID.fromString(entryUUID);

                    return new Change(-1, "REMOVE", new RemoveOperation(entryUUID));

                default:
                    throw new JsonParseException("Unknown operation: " + operation);
            }
        }

    }

}
