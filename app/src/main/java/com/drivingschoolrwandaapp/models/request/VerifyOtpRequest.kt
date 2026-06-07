package com.drivingschoolrwandaapp.models.request

import com.google.gson.annotations.SerializedName

data class VerifyOtpRequest(
    @SerializedName("email") var email: String = "",
    @SerializedName("otp") var otp: String = ""
)
