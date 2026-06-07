package com.drivingschoolrwandaapp.models.request;

import com.google.gson.annotations.SerializedName;

public class VerifyEmailRequest {
    @SerializedName("token")
    private String token;

    public VerifyEmailRequest(String token) {
        this.token = token;
    }

    public String getToken() { return token; }
}