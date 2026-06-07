package com.drivingschoolrwandaapp.models.request;

import com.google.gson.annotations.SerializedName;

public class VerifyOtpRequest {

    @SerializedName("email")
    private String email;

    @SerializedName("otp")
    private String otp;

    public VerifyOtpRequest(String email, String otp) {
        this.email = email;
        this.otp = otp;
    }

    public String getEmail() {
        return email;
    }

    public String getOtp() {
        return otp;
    }
}
