package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName

data class SubscriptionPlan(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("planName") var planName: String? = null,
    @SerializedName("amount") var amount: String? = null,
    @SerializedName("duration") var duration: Int = 0,
    @SerializedName("maxTestAccess") var maxTestAccess: Int = 0,
    @SerializedName("permissions") var permissions: List<Permission> = mutableListOf()
)
