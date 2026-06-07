package com.drivingschoolrwandaapp.models.response

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @get:JvmName("isSuccess") @SerializedName("success") var success: Boolean = false,
    @SerializedName("message") var message: String? = null,
    @SerializedName("data") var data: T? = null,
    @SerializedName("error") var error: String? = null
)
