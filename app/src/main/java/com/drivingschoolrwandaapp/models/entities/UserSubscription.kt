package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName

data class UserSubscription(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("userId") var userId: Int = 0,
    @SerializedName("subscriptionPlanId") var subscriptionPlanId: Int = 0,
    @SerializedName("subscriptionPlan") var subscriptionPlan: SubscriptionPlan? = null
)
