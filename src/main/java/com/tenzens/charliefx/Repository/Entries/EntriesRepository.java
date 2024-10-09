package com.tenzens.charliefx.Repository.Entries;

import com.tenzens.charliefx.Quittable;
import com.tenzens.charliefx.Repository.Authentication.AuthenticationRepository;
import com.tenzens.charliefx.Repository.Authentication.RemoteDataSource.ServerAuthenticationSource;
import com.tenzens.charliefx.Repository.Entries.LocalDataSource.EntriesLocalDataSource;
import com.tenzens.charliefx.Repository.Entries.Model.*;
import com.tenzens.charliefx.Repository.Entries.RemoteDataSource.DownstreamCommandsListener;
import com.tenzens.charliefx.Repository.Entries.RemoteDataSource.EntriesServerDataSource;
import com.tenzens.charliefx.Repository.Model.*;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import org.eclipse.jetty.client.HttpClient;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntriesRepository implements Quittable, DownstreamCommandsListener.DownstreamCommandsHandler {
    private final AuthenticationRepository authenticationRepository;

    private final EntriesServerDataSource entriesServerDataSource;

    private EntriesLocalDataSource entriesLocalDataSource;

    private final SortedList<ClientEntry> sortedEntries;

    private final BlockingQueue<UpstreamCommand> upstreamCommandsQueue;

    private final ExecutorService executorService;

    private final ObservableList<ClientEntry> entries;

    private final IntegerProperty numberOfEntries;

    private final ConnectionStatus connected;

    public EntriesRepository(ExecutorService executorService,
                             Gson gson,
                             HttpClient httpClient,
                             AuthenticationRepository authenticationRepository) {

        Gson newGson = gson.newBuilder()
                .registerTypeAdapter(Change.class, new Change.ChangeSerializer())
                .create();

        this.executorService = executorService;
        this.authenticationRepository = authenticationRepository;
        this.entriesServerDataSource = new EntriesServerDataSource(newGson, httpClient, authenticationRepository);

        try {
            this.entriesLocalDataSource = new EntriesLocalDataSource(authenticationRepository.getAloha().getDeviceId(), newGson);
        }catch (Exception e) {
            e.printStackTrace();
        }

        this.upstreamCommandsQueue = new ArrayBlockingQueue<>(10);

        Comparator<ClientEntry> entryComparator = (o1, o2) -> {
            if (o1.getCreatedAt() < o2.getCreatedAt()) {
                return 1;
            }else if (o1.getCreatedAt() > o2.getCreatedAt()) {
                return -1;
            }

            return -1*o1.getUUID().compareTo(o2.getUUID());
        };

        ObservableList<ClientEntry> entriesList;
        try {
            entriesList = FXCollections.observableArrayList(entriesLocalDataSource.loadEntries());
        }catch (Exception e) {
            entriesList = FXCollections.observableArrayList();
            e.printStackTrace();
        }

        this.entries = entriesList;
        this.sortedEntries = new SortedList<>(this.entries, entryComparator);
        this.connected = new ConnectionStatus(false, null);
        this.numberOfEntries = new SimpleIntegerProperty(entriesList.size());
    }

    public void run() {
        var authenticationCallback = new AuthenticationRepository.AuthenticationCallback() {
            @Override
            public void onSuccess() {
                reconnectDownstream();
                handleUpstreamChanges();
            }

            @Override
            public void onFailure(Exception e) {
                connected.setExceptionMessage(e.getMessage());
                connected.setConnected(false);
            }
        };

        var refreshCallback = new AuthenticationRepository.RefreshCallback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(Exception e) {
                connected.setExceptionMessage(e.getMessage());
                connected.setConnected(false);
            }
        };

        if (authenticationRepository.getAloha().getSession().getRefreshTokenExpiration() <= System.currentTimeMillis()) {
            authenticationRepository.authenticate(authenticationCallback);
        }else {
            authenticationRepository.scheduleRefresh(refreshCallback);
            reconnectDownstream();
            handleUpstreamChanges();
        }
    }

    public void reconnectDownstream() {
        if (connected.isConnected()) {
            return;
        }

        connected.setConnected(true);

        Task<Void> downstreamConnectionTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                entriesServerDataSource.live(EntriesRepository.this);
                upstreamCommandsQueue.put(new UpstreamCommand(UpstreamCommand.Command.FLUSH, null));
                return null;
            }
        };

        this.executorService.submit(downstreamConnectionTask);
    }

    @Override
    public void onDownstreamError(Exception e) {
        Platform.runLater(() -> {
            connected.setExceptionMessage(e.getMessage());
            connected.setConnected(false);
        });
    }

    @Override
    public void onDownstreamFileRequest(String fileId, DownstreamCommandsListener.FileRequestResponseHandler fileRequestResponseHandler) throws Exception {
        ClientEntry entry = entriesLocalDataSource.loadEntryByUUID(fileId);

        if (entry == null) {
            fileRequestResponseHandler.onFileRequestResponse(new DownstreamCommandsListener.ApiResponse(500, "ENTRY NOT FOUND"));
            return;
        }

        if (entry.getDeviceUUID().equals(authenticationRepository.getAloha().getDeviceId()) &&
            entry.getContent().getType().equals("FILE")) {

            File file = ((FileEntry) entry).getContent().getFile();
            if (file == null || !file.exists()) {
                fileRequestResponseHandler.onFileRequestResponse(new DownstreamCommandsListener.ApiResponse(500, "FILE NOT FOUND"));
                return;
            }

            fileRequestResponseHandler.onFileRequestResponse(new DownstreamCommandsListener.ApiResponse(200, "OK"));

            var task = new Task<>() {
                protected Void call() throws Exception {
                    try {
                        entriesServerDataSource.postFile(fileId, file);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    }
                    return null;
                }
            };

            executorService.submit(task);
        }else {
            fileRequestResponseHandler.onFileRequestResponse(new DownstreamCommandsListener.ApiResponse(500, "ENTRY NOT FOUND"));
        }
    }

    @Override
    public void onDownstreamChanges(List<Change> changes) throws Exception {
        List<ClientEntry> entriesToAdd = new ArrayList<>();
        List<String> entriesToRemove = new ArrayList<>();

        for (Change change : changes) {
            switch (change.getOperation()) {
                case "ADD":
                    var entry = ((AddOperation) change.getOperationBody()).getEntry();
                    ClientEntry clientEntry;

                    switch (entry.getBody().getContent().getType()) {
                        case "TEXT":
                            clientEntry = new TextEntry(-1, entry, false, ClientEntry.DeliveryStatus.DELIVERED);
                            break;

                        case "FILE":
                            clientEntry = new FileEntry(-1, entry,false, ClientEntry.DeliveryStatus.DELIVERED);
                            break;

                        default:
                            continue;
                    }

                    entriesToAdd.add(clientEntry);
                    break;

                case "REMOVE":
                    entriesToRemove.add(change.getOperationBody().getEntryUUID());
                    break;
            }
        }

        try {
            entriesLocalDataSource.storeEntries(entriesToAdd);
            entriesLocalDataSource.removeEntries(entriesToRemove);
        }catch (Exception e) {
            e.printStackTrace();
        }

        Platform.runLater(() -> {
            for (ClientEntry entry : entriesToAdd) {
                if (entries.contains(entry)) {
                    continue;
                }

                entries.add(entry);
            }

            entries.removeIf(entry -> entriesToRemove.contains(entry.getUUID()));
            numberOfEntries.set(entries.size());
        });
    }

    public void handleUpstreamChanges() {
        Task<Void> handleUpstreamChangesTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<Change> unsentChanges;

                try {
                   unsentChanges = entriesLocalDataSource.loadPendingChanges();
                }catch (Exception e) {
                    e.printStackTrace();
                    unsentChanges = new ArrayList<Change>();
                }

                List<Change> changesQueue = new ArrayList<>();

                while(true) {
                    UpstreamCommand upstreamCommand;

                    try {
                        upstreamCommand = upstreamCommandsQueue.take();
                    } catch (InterruptedException e) {
                        break;
                    }

                    if (upstreamCommand.getCommand() == UpstreamCommand.Command.PUSH) {
                        changesQueue.add(upstreamCommand.getChange());

                        while (true) {
                            try {
                                var newQueuedCommand = upstreamCommandsQueue.poll(300, TimeUnit.MILLISECONDS);

                                if (newQueuedCommand == null) {
                                    break;
                                }else if (newQueuedCommand.getCommand() == UpstreamCommand.Command.FLUSH) {
                                    continue;
                                }

                                changesQueue.add(newQueuedCommand.getChange());
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }else if(upstreamCommand.getCommand() == UpstreamCommand.Command.FLUSH && changesQueue.isEmpty()) {
                        continue;
                    }

                    try {
                        entriesLocalDataSource.storeAndApplyChanges(changesQueue);
                    }catch (Exception e) {
                        e.printStackTrace();
                    }

                    var changesToPost = Stream.concat(unsentChanges.stream(), changesQueue.stream()).collect(Collectors.toList());
                    try {
                        entriesServerDataSource.post(changesToPost);
                    } catch (Exception e) {
                        e.printStackTrace();
                        var queuedChangesEntryIds = changesToPost.stream().map((change) -> change.getOperationBody().getEntryUUID()).collect(Collectors.toSet());
                        Platform.runLater(() -> {
                            entries.stream().filter(entry -> queuedChangesEntryIds.contains(entry.getUUID())).forEach(entry -> {
                                entry.setDeliveryStatus(ClientEntry.DeliveryStatus.UNSUCCESSFUL_DELIVERY);
                            });
                        });
                        continue;
                    }

                    // remove persisted changes
                    try {
                        entriesLocalDataSource.removeChanges(changesToPost);
                    }catch (Exception e) {
                        e.printStackTrace();
                    }

                    var queuedChangesEntryIds = changesToPost.stream().map((change) -> change.getOperationBody().getEntryUUID()).collect(Collectors.toSet());
                    Platform.runLater(() -> {
                        entries.stream().filter(entry -> queuedChangesEntryIds.contains(entry.getUUID())).forEach(entry -> {
                            entry.setDeliveryStatus(ClientEntry.DeliveryStatus.DELIVERED);
                        });
                    });

                    changesQueue.clear();
                    unsentChanges.clear();
                }
                return null;
            }
        };
        this.executorService.submit(handleUpstreamChangesTask);
    }

    public void addEntry(final ClientEntry clientEntry) {
        this.entries.add(clientEntry);
        numberOfEntries.set(entries.size());

        var addEntryTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                upstreamCommandsQueue.put(new UpstreamCommand(UpstreamCommand.Command.PUSH, new Change("ADD", new ClientAddOperation(clientEntry))));
                return null;
            }
        };

        this.executorService.submit(addEntryTask);
    }

    public void removeEntry(final ClientEntry entry) {
        this.entries.remove(entry);
        numberOfEntries.set(entries.size());

        var removeEntryTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                upstreamCommandsQueue.put(new UpstreamCommand(UpstreamCommand.Command.PUSH, new Change("REMOVE", new RemoveOperation(entry.getUUID()))));
                return null;
            }
        };

        this.executorService.submit(removeEntryTask);
    }

    public SortedList<ClientEntry> getSortedEntries() {
        return sortedEntries;
    }

    public IntegerProperty numberOfEntries() {
        return numberOfEntries;
    }

    public ConnectionStatus getConnectionStatus() {
        return connected;
    }

    public BooleanProperty isConnected() {
        return connected.getConnectionProperty();
    }

    public void getFile(FileEntry entry) {
        entry.getIsDownloadingProperty().set(true);
        entry.getDownloadProgressProperty().set(0.0);

        var task = new Task<>() {
            protected Void call() throws Exception {
            try {
                entriesServerDataSource.getFile(entry.getUUID(), entry.getContent().getFileName(), new EntriesServerDataSource.ProgressCallback() {
                    @Override
                    public void callback(int progress) {
                        Platform.runLater(() -> {
                            entry.getDownloadProgressProperty().set((double) progress / entry.getContent().getFileSize());
                        });
                    }
                });
            }catch(Exception e) {
                e.printStackTrace();
                throw e;
            }

            return null;
            }
        };

        task.setOnSucceeded((event) -> {
            entry.getIsDownloadingProperty().set(false);
        });

        task.setOnCancelled((event) -> {
            entry.getIsDownloadingProperty().set(false);
        });

        task.setOnFailed((event) -> {
            entry.getIsDownloadingProperty().set(false);
        });

        executorService.submit(task);
    }

    @Override
    public void onQuit() {
        this.entriesServerDataSource.onQuit();
        this.executorService.shutdownNow();
    }

}
