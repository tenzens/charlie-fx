package com.tenzens.charliefx.Repository.Model;

import java.io.File;

public class FileContent extends EntryContent{
    private final String fileName;
    private final long fileSize;
    private transient File file = null;

    public FileContent(String fileName, long fileSize, File file) {
        super("FILE");
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    @Override
    public String toSearchableString() {
        return fileName;
    }
}
