package com.tenzens.charliefx.Repository.Authentication.LocalDataSource;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalAuthenticationSource {

    private final Gson gson;
    private final Path profilePath;

    public LocalAuthenticationSource(Path profilePath, Gson gson) {
        this.profilePath = profilePath;
        this.gson = gson;
    }

    public DiskProfile loadDiskSession() throws IOException {
        return gson.fromJson(Files.readString(profilePath), DiskProfile.class);
    }

    public void storeDiskProfile(DiskProfile diskSession) throws IOException {
        Files.createDirectories(profilePath.getParent());
        Files.writeString(profilePath, gson.toJson(diskSession));
    }
}
