package com.drivingschoolrwandaapp.models.request;

import com.google.gson.annotations.SerializedName;

public class ResetPasswordRequest {
    @SerializedName("token")
    private String token;

    @SerializedName("newPassword")
    private String newPassword;

    @SerializedName("confirmPassword")
    private String confirmPassword;

    public ResetPasswordRequest(String token, String newPassword, String confirmPassword) {
        this.token = token;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

    public String getToken() { return token; }
    public String getNewPassword() { return newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
}
