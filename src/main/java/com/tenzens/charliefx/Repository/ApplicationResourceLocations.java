package com.tenzens.charliefx.Repository;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ApplicationResourceLocations {
    private static final String applicationFolderName = "charlie-fx/";

    public static String getApplicationFolderName() {
        return applicationFolderName;
    }

    public static Path getApplicationFolderPath() {
        var homeDir = Paths.get(System.getProperty("user.home"));
        return Paths.get(homeDir.toString(), ApplicationResourceLocations.getApplicationFolderName());
    }
}
