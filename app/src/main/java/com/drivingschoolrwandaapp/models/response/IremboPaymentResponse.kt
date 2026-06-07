package com.drivingschoolrwandaapp.models.response

import com.google.gson.annotations.SerializedName

data class IremboPaymentResponse(
    @get:JvmName("isSuccess") @SerializedName("success") var success: Boolean = false,
    @SerializedName("message") var message: String? = null,
    @SerializedName("reference") var reference: String? = null,
    @SerializedName("status") var status: String? = null,
    @SerializedName("amount") var amount: Double = 0.0,
    @SerializedName("currency") var currency: String? = null
)
