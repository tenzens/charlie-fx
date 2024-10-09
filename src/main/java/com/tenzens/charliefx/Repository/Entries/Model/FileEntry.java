package com.tenzens.charliefx.Repository.Entries.Model;

import com.tenzens.charliefx.Repository.Model.Entry;
import com.tenzens.charliefx.Repository.Model.EntryBody;
import com.tenzens.charliefx.Repository.Model.FileContent;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.io.File;
import java.util.UUID;

public class FileEntry extends ClientEntry {
    private final SimpleDoubleProperty downloadProgress;
    private final SimpleBooleanProperty isDownloading;

    public FileEntry(long id, Entry entry, boolean isLocal, String deliveryStatus) {
        super(id, entry, isLocal, deliveryStatus);

        this.downloadProgress = new SimpleDoubleProperty(0.0);
        this.isDownloading = new SimpleBooleanProperty(false);
    }

    public static FileEntry createNewFileEntry(String deviceId, String fileName, long fileSize, File file, String color, String accentColor, boolean pinned) {
        EntryBody body = new EntryBody(
                color,
                accentColor,
                pinned,
                new FileContent(
                        fileName,
                        fileSize,
                        file
                )
        );

        Entry entry = new Entry(
                UUID.randomUUID().toString(),
                deviceId,
                System.currentTimeMillis(),
                body
        );

        return new FileEntry(
                -1, entry, true, DeliveryStatus.UNDELIVERED
        );
    }

    public FileContent getContent() {
        return (FileContent) entry.getBody().getContent();
    }

    public double getDownloadProgress() {
        return downloadProgress.get();
    }

    public SimpleDoubleProperty getDownloadProgressProperty() {
        return downloadProgress;
    }

    public boolean IsDownloading() {
        return isDownloading.get();
    }

    public SimpleBooleanProperty getIsDownloadingProperty() {
        return isDownloading;
    }
}
