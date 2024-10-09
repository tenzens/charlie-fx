package com.tenzens.charliefx.Repository.Entries.LocalDataSource;

import com.tenzens.charliefx.Repository.Model.EntryContent;
import com.tenzens.charliefx.Repository.Model.FileContent;

import java.io.File;

public class PersistedFileContent extends EntryContent {
    private final String fileName;
    private final long fileSize;
    private final String filePath;

    public PersistedFileContent(FileContent fileContent) {
        super("FILE");
        this.fileName = fileContent.getFileName();
        this.fileSize = fileContent.getFileSize();

        if (fileContent.getFile() != null) {
            this.filePath = fileContent.getFile().getAbsolutePath();
        }else {
            this.filePath = null;
        }
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public String toSearchableString() {
        return fileName;
    }
}
