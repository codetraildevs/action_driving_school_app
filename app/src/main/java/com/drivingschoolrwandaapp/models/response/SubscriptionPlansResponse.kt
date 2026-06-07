package com.drivingschoolrwandaapp.models.response

import com.drivingschoolrwandaapp.models.entities.SubscriptionPlan
import com.google.gson.annotations.SerializedName

data class SubscriptionPlansResponse(
    @SerializedName("success") var success: Boolean = false,
    @SerializedName("data") var data: List<SubscriptionPlan>? = null
)
