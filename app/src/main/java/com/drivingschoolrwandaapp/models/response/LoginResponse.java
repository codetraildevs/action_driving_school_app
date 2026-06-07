package com.drivingschoolrwandaapp.models.response;

import com.drivingschoolrwandaapp.models.entities.User;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class LoginResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("user")
    private User user;

    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("refreshToken")
    private String refreshToken;

    // No expiresAt in the provided backend code, but keeping it in case it's needed later
    private Date expiresAt;

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }
}
