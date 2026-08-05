package com.drivingschoolrwandaapp.models.request

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("identifier") var identifier: String = "",
    @SerializedName("password") var password: String = "",
    @SerializedName("deviceId") var deviceId: String = ""
)
