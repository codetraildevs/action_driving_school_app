package com.drivingschoolrwandaapp.models.response;

import com.google.gson.annotations.SerializedName;

public class RegisterResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("error")
    private String error;

    @SerializedName("userId")
    private int userId;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message != null ? message : error;
    }

    public int getUserId() {
        return userId;
    }
}
