package com.tenzens.charliefx.Repository.Entries.LocalDataSource;

import com.tenzens.charliefx.Repository.ApplicationResourceLocations;
import com.tenzens.charliefx.Repository.Entries.Model.ClientAddOperation;
import com.tenzens.charliefx.Repository.Entries.Model.ClientEntry;
import com.tenzens.charliefx.Repository.Entries.Model.FileEntry;
import com.tenzens.charliefx.Repository.Entries.Model.TextEntry;
import com.tenzens.charliefx.Repository.Model.*;

import com.google.gson.*;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class EntriesLocalDataSource {
    private final Gson gson;
    private final Connection connection;
    private final ReentrantLock lock = new ReentrantLock();
    private final String deviceId;

    public EntriesLocalDataSource(String deviceId, Gson gson) throws Exception{
        this.deviceId = deviceId;

        // Initialize Gson with custom serializer
        this.gson = gson.newBuilder()
                .registerTypeAdapter(PersistedEntryBody.class, new PersistedEntryBody.PersistedEntryBodyDeserializer())
                .create();

        String DB_URL = String.format("jdbc:sqlite:%s", ApplicationResourceLocations.getApplicationFolderPath().resolve("charlie.db"));

        // Create a single connection to the database
        this.connection = DriverManager.getConnection(DB_URL);

        Files.createDirectories(ApplicationResourceLocations.getApplicationFolderPath().resolve("entries"));

        // Initialize SQLite tables
        initDatabase();
    }

    // Only encrypt the entry content? Basically only encrypt sensitive data... maybe best bet

    private void initDatabase() throws Exception{
        String createEntriesTable = "CREATE TABLE IF NOT EXISTS cliententries (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "uuid TEXT UNIQUE NOT NULL, " +
                "deviceUUID TEXT NOT NULL, " +
                "createdAt INTEGER, " +
                "client_entry TEXT)";

        String createChangesTable = "CREATE TABLE IF NOT EXISTS changes (" +
                "id INTEGER PRIMARY KEY, " +
                "operation TEXT, " +
                "entryUUID TEXT, " +
                "client_entry TEXT)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createEntriesTable);
            stmt.execute(createChangesTable);
        }
    }

    public void storeEntries(List<ClientEntry> entries) throws Exception {
        lock.lock();
        for (ClientEntry clientEntry : entries) {
            PersistedClientEntry persistedClientEntry = new PersistedClientEntry(
                    clientEntry.getDeliveryStatus().get(),
                    clientEntry.isLocal(),
                    new PersistedEntryBody(
                            clientEntry.getEntry().getBody()
                    )
            );

            // If it's a text content, we will write to a file instead
            if (clientEntry.getEntry().getBody().getContent().getType().equals("TEXT")) {
                storeTextContentToFile(clientEntry.getEntry().getUUID(), ((TextContent) clientEntry.getEntry().getBody().getContent()));
            }

            String serializedBody = gson.toJson(persistedClientEntry, PersistedClientEntry.class);

            String insertEntrySql = "INSERT OR IGNORE INTO cliententries(uuid, deviceUUID, createdAt, client_entry) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertEntrySql)) {
                pstmt.setString(1, clientEntry.getEntry().getUUID());
                pstmt.setString(2, clientEntry.getEntry().getDeviceUUID());
                pstmt.setLong(3, clientEntry.getEntry().getCreatedAt());
                pstmt.setString(4, serializedBody);
                pstmt.executeUpdate();

                clientEntry.setId(pstmt.getGeneratedKeys().getLong(1));
            }
        }

        lock.unlock();
    }

    public void removeEntries(List<String> entriesToRemove) throws Exception {
        lock.lock();
            for (String entryUUID : entriesToRemove) {
                String deleteEntrySql = "DELETE FROM cliententries WHERE uuid = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(deleteEntrySql)) {
                    pstmt.setString(1, entryUUID);
                    pstmt.executeUpdate();
                }

                removeTextContentFile(entryUUID);
            }
        lock.unlock();
    }

    public List<ClientEntry> loadEntries() throws Exception {
        List<ClientEntry> entries = new ArrayList<>();
        String selectSql = "SELECT id, uuid, deviceUUID, createdAt, client_entry FROM cliententries";

        lock.lock();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {

            while (rs.next()) {
                long id = rs.getLong("id");
                String uuid = rs.getString("uuid");
                String deviceId = rs.getString("deviceUUID");
                long createdAt = rs.getLong("createdAt");
                String clientEntryJson = rs.getString("client_entry");

                PersistedClientEntry persistedClientEntry = gson.fromJson(clientEntryJson, PersistedClientEntry.class);

                // If it's a text entry, load the text content from file
                if (persistedClientEntry.getEntryBody().getContentType().equals("TEXT")) {
                    TextContent textContent = loadTextContentFromFile(uuid);
                    persistedClientEntry.getEntryBody().setContent(textContent);
                    entries.add(textEntryFromPersistedClientEntry(id, uuid, deviceId, createdAt, persistedClientEntry));
                }else if (persistedClientEntry.getEntryBody().getContentType().equals("FILE")) {
                    entries.add(fileEntryFromPersistedClientEntry(id, uuid, deviceId, createdAt, persistedClientEntry));
                }
            }
        }
        lock.unlock();

        return entries;
    }

    private FileEntry fileEntryFromPersistedClientEntry(long id, String uuid, String deviceId, long createdAt, PersistedClientEntry persistedClientEntry) {
        return new FileEntry(
                id,
                new Entry(
                        uuid,
                        deviceId,
                        createdAt,
                        new EntryBody(
                                persistedClientEntry.getEntryBody().getColor(),
                                persistedClientEntry.getEntryBody().getHighlightsColor(),
                                persistedClientEntry.getEntryBody().isPinned(),
                                persistedClientEntry.getEntryBody().getContent()
                        )
                ),
                this.deviceId.equals(deviceId),
                persistedClientEntry.getDeliveryStatus()
        );
    }

    private TextEntry textEntryFromPersistedClientEntry(long id, String uuid, String deviceId, long createdAt, PersistedClientEntry persistedClientEntry) {
        return new TextEntry(
                id,
                new Entry(
                        uuid,
                        deviceId,
                        createdAt,
                        new EntryBody(
                                persistedClientEntry.getEntryBody().getColor(),
                                persistedClientEntry.getEntryBody().getHighlightsColor(),
                                persistedClientEntry.getEntryBody().isPinned(),
                                persistedClientEntry.getEntryBody().getContent()
                        )
                ),
                this.deviceId.equals(deviceId),
                persistedClientEntry.getDeliveryStatus()
        );
    }

    public ClientEntry loadEntryByUUID(String uuid) throws Exception {
        lock.lock();
            try (PreparedStatement pstmt = connection.prepareStatement("SELECT * FROM cliententries WHERE uuid = ?")) {
                pstmt.setString(1, uuid);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        long id = rs.getLong("id");
                        String deviceId = rs.getString("deviceUUID");
                        long createdAt = rs.getLong("createdAt");
                        String bodyJson = rs.getString("client_entry");

                        PersistedClientEntry persistedClientEntry = gson.fromJson(bodyJson, PersistedClientEntry.class);

                        // If it's a text entry, load the text content from file
                        if (persistedClientEntry.getEntryBody().getContentType().equals("TEXT")) {
                            TextContent textContent = loadTextContentFromFile(uuid);
                            persistedClientEntry.getEntryBody().setContent(textContent);
                            lock.unlock();
                            return textEntryFromPersistedClientEntry(id, uuid, deviceId, createdAt, persistedClientEntry);
                        } else if (persistedClientEntry.getEntryBody().getContentType().equals("FILE")){
                            lock.unlock();
                            return fileEntryFromPersistedClientEntry(id, uuid, deviceId, createdAt, persistedClientEntry);
                        }

                        lock.unlock();
                        throw new Exception("unsupported entry type: " + persistedClientEntry.getEntryBody().getContentType());
                    }
                }
            }
        lock.unlock();
        return null;
    }

    public void storeAndApplyChanges(List<Change> changes) throws Exception {
        String insertChangeSql = "INSERT INTO changes(operation, entryUUID, client_entry) VALUES (?, ?, ?)";

        lock.lock();
        List<ClientEntry> entriesToStore = new ArrayList<>();
        List<String> entriesToRemove = new ArrayList<>();

        for (Change change : changes) {
            if (change.getOperation().equals("ADD")) {
                var addOp = ((ClientAddOperation) change.getOperationBody());

                if (addOp.getClientEntry().getContent().getType().equals("TEXT")) {
                    storeTextContentToFile(change.getOperationBody().getEntryUUID(), (TextContent) addOp.getClientEntry().getContent());
                }

                entriesToStore.add(addOp.getClientEntry());
            }else if (change.getOperation().equals("REMOVE")) {
                removeTextContentFile(change.getOperationBody().getEntryUUID());
                entriesToRemove.add(change.getOperationBody().getEntryUUID());
            }else {
                continue;
            }

            try (PreparedStatement pstmt = connection.prepareStatement(insertChangeSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, change.getOperation());
                pstmt.setString(2, change.getOperationBody().getEntryUUID());

                if (change.getOperation().equals("ADD")) {
                    ClientEntry clientEntry = ((ClientAddOperation) change.getOperationBody()).getClientEntry();

                    PersistedClientEntry persistedClientEntry = new PersistedClientEntry(
                        clientEntry.getDeliveryStatus().get(),
                        clientEntry.isLocal(),
                        new PersistedEntryBody(
                                clientEntry.getEntry().getBody()
                        )
                    );

                    pstmt.setString(3, gson.toJson(persistedClientEntry, PersistedClientEntry.class));
                }else if (change.getOperation().equals("REMOVE")) {
                    pstmt.setString(3, "");
                }

                pstmt.executeUpdate();

                change.setId(pstmt.getGeneratedKeys().getLong(1));
            }
        }

        storeEntries(entriesToStore);
        removeEntries(entriesToRemove);
        lock.unlock();
    }

    public List<Change> loadPendingChanges() throws Exception {
        List<Change> changes = new ArrayList<>();
        String selectSql = "SELECT changes.id AS changeId, operation, entryUUID, client_entry FROM changes";

        lock.lock();
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(selectSql)) {

                while (rs.next()) {
                    long changeId = rs.getLong("changeId");
                    String operation = rs.getString("operation");
                    String entryUUID = rs.getString("entryUUID");
                    String deviceUUID = rs.getString("deviceUUID");

                    long createdAt = rs.getLong("createdAt");
                    String serializedPersistedClientEntry = rs.getString("client_entry");

                    PersistedClientEntry persistedClientEntry = gson.fromJson(serializedPersistedClientEntry, PersistedClientEntry.class);
                    ClientEntry clientEntry;
                    switch (persistedClientEntry.getEntryBody().getContentType()) {
                        case "TEXT":
                            clientEntry = textEntryFromPersistedClientEntry(-1, entryUUID, deviceUUID, createdAt, persistedClientEntry);
                            break;

                        case "FILE":
                            clientEntry = fileEntryFromPersistedClientEntry(-1, entryUUID, deviceUUID, createdAt, persistedClientEntry);
                            break;
                        default:
                            continue;
                    }

                    if (operation.equals("ADD")) {
                        Change change = new Change(changeId, operation, new ClientAddOperation(clientEntry));
                        changes.add(change);
                    }else if (operation.equals("REMOVE")) {
                        Change change = new Change(changeId, operation, new RemoveOperation(clientEntry.getUUID()));
                        changes.add(change);
                    }
                }

            }
        lock.unlock();
        return changes;
    }

    public void removeChanges(List<Change> changes) throws Exception {
        lock.lock();
        // Prepare SQL query to delete changes with entryIds in the provided list
        String deleteChangesSql = "DELETE FROM changes WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(deleteChangesSql)) {
            for (Change change : changes) {
                pstmt.setLong(1, change.getId());
                pstmt.executeUpdate();  // Execute the delete for each uuid
            }
        }

        lock.unlock();
    }

    private void storeTextContentToFile(String uuid, TextContent textContent) throws IOException {
        Path file = ApplicationResourceLocations.getApplicationFolderPath().resolve("entries/" + uuid);

        String textContentString = textContent.getTitle() +
                                   System.lineSeparator() +
                                   textContent.getText();

        Files.writeString(file, textContentString);
    }

    private TextContent loadTextContentFromFile(String uuid) throws IOException {
        Path file = ApplicationResourceLocations.getApplicationFolderPath().resolve(Paths.get("entries/", uuid));
        List<String> lines = Files.readAllLines(file);
        return new TextContent(lines.get(0), String.join("", lines.subList(1, lines.size())));
    }

    private void removeTextContentFile(String uuid) throws IOException {
        Files.deleteIfExists(ApplicationResourceLocations.getApplicationFolderPath().resolve(Paths.get("entries/", uuid)));
    }
}
