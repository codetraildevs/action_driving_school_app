package com.drivingschoolrwandaapp.models.response

import com.google.gson.annotations.SerializedName

data class RegisterResponse(
    @get:JvmName("isSuccess") @SerializedName("success") var success: Boolean = false,
    @SerializedName("message") private var _message: String? = null,
    @SerializedName("error") var error: String? = null,
    @SerializedName("userId") var userId: Int = 0
) {
    val message: String?
        get() = _message ?: error
}
