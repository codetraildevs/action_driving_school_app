package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Locale

data class Transaction(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("subscriptionPlan") var subscriptionPlan: SubscriptionPlan? = null,
    @SerializedName("paid") var paid: Boolean = false,
    @SerializedName("method") var method: String? = null,
    @SerializedName("refId") var refId: String? = null,
    @SerializedName("amount") var amount: Double = 0.0,
    @SerializedName("createdAt") var createdAt: String? = null
) : Serializable {

    fun getFormattedAmount(): String {
        return String.format(Locale.ROOT, "RWF %.2f", amount)
    }
}
