package com.drivingschoolrwandaapp.models.response

import com.drivingschoolrwandaapp.models.entities.UserSubscription
import com.google.gson.annotations.SerializedName

data class UserSubscriptionResponse(
    @get:JvmName("isSuccess") @SerializedName("success") var success: Boolean = false,
    @SerializedName("data") var data: UserSubscription? = null
)
