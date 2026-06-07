package com.drivingschoolrwandaapp.models.response

import com.drivingschoolrwandaapp.models.entities.User
import com.google.gson.annotations.SerializedName
import java.util.Date

data class LoginResponse(
    @get:JvmName("isSuccess") @SerializedName("success") var success: Boolean = false,
    @SerializedName("message") var message: String? = null,
    @SerializedName("user") var user: User? = null,
    @SerializedName("accessToken") var accessToken: String? = null,
    @SerializedName("refreshToken") var refreshToken: String? = null,
    var expiresAt: Date? = null
)
