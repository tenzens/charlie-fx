package com.tenzens.charliefx.Repository.Authentication.LocalDataSource;

public class DiskProfile {
    private final String serverUrl;
    private final String encryptedAloha;

    public DiskProfile(String serverUrl, String encryptedAloha) {
        this.serverUrl = serverUrl;
        this.encryptedAloha = encryptedAloha;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getEncryptedAloha() {
        return encryptedAloha;
    }
}
