package com.tenzens.charliefx.Repository.Authentication;

import com.google.gson.annotations.SerializedName;

public class Session {
    private String accessToken;
    private String refreshToken;

    @SerializedName(value = "access_expiration")
    private long accessTokenExpiration;

    @SerializedName(value = "refresh_expiration")
    private long refreshTokenExpiration;

    public String getAccessToken() {
        return accessToken;
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}