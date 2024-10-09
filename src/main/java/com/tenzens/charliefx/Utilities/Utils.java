package com.tenzens.charliefx.Utilities;

public class Utils {
    public static String formatFileSize(long sizeInBytes) {
        final long ONE_KB = 1024;
        final long ONE_MB = ONE_KB * 1024;
        final long ONE_GB = ONE_MB * 1024;
        final long ONE_TB = ONE_GB * 1024;

        if (sizeInBytes >= ONE_TB) {
            return String.format("%.2f TB", (double) sizeInBytes / ONE_TB);
        } else if (sizeInBytes >= ONE_GB) {
            return String.format("%.2f GB", (double) sizeInBytes / ONE_GB);
        } else if (sizeInBytes >= ONE_MB) {
            return String.format("%.2f MB", (double) sizeInBytes / ONE_MB);
        } else if (sizeInBytes >= ONE_KB) {
            return String.format("%.2f KB", (double) sizeInBytes / ONE_KB);
        } else {
            return sizeInBytes + " Bytes";
        }
    }
}
