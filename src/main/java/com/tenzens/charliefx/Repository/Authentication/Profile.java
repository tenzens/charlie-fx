package com.tenzens.charliefx.Repository.Authentication;

public class Profile {
    private final String serverUrl;
    private final transient String password;
    private Aloha aloha;

    public Profile(String serverUrl, String password) {
        this.serverUrl = serverUrl;
        this.password = password;
    }

    public void setAloha(final Aloha aloha) {
        this.aloha = aloha;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getPassword() {
        return password;
    }

    public Aloha getAloha() {
        return aloha;
    }
}
