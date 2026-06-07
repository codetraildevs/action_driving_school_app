package com.drivingschoolrwandaapp.models.request

import com.google.gson.annotations.SerializedName

data class PasswordChangeRequest(
    @SerializedName("currentPassword") var currentPassword: String = "",
    @SerializedName("newPassword") var newPassword: String = "",
    @SerializedName("confirmPassword") var confirmPassword: String = ""
)
