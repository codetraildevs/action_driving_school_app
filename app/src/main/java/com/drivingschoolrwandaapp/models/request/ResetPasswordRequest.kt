package com.drivingschoolrwandaapp.models.request

import com.google.gson.annotations.SerializedName

data class ResetPasswordRequest(
    @SerializedName("token") var token: String = "",
    @SerializedName("newPassword") var newPassword: String = "",
    @SerializedName("confirmPassword") var confirmPassword: String = ""
)
