package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName

/** Wrapper for GET admin/users/{id} → { success, data: AdminUserDetail }. */
data class AdminUserDetailResponse(
    @SerializedName("success") var success: Boolean = false,
    @SerializedName("data") var data: AdminUserDetail? = null
) : java.io.Serializable

/** A single user with their current subscription, for the admin detail dialog. */
data class AdminUserDetail(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("firstName") var firstName: String? = null,
    @SerializedName("middleName") var middleName: String? = null,
    @SerializedName("lastName") var lastName: String? = null,
    @SerializedName("email") var email: String? = null,
    @SerializedName("phoneNumber") var phoneNumber: String? = null,
    @SerializedName("isActive") var isActive: Boolean = false,
    @SerializedName("createdAt") var createdAt: String? = null,
    @SerializedName("role") var role: AdminRole? = null,
    @SerializedName("userSubscription") var userSubscription: AdminUserSubscription? = null
) : java.io.Serializable {

    fun getFullName(): String {
        return buildString {
            if (firstName != null) append(firstName)
            if (!middleName.isNullOrEmpty()) {
                if (isNotEmpty()) append(" ")
                append(middleName)
            }
            if (!lastName.isNullOrEmpty()) {
                if (isNotEmpty()) append(" ")
                append(lastName)
            }
        }
    }

    fun getInitials(): String {
        val first = firstName?.takeIf { it.isNotBlank() }?.firstOrNull()?.toString() ?: ""
        val last = lastName?.takeIf { it.isNotBlank() }?.firstOrNull()?.toString() ?: ""
        return (first + last).ifBlank { "?" }
    }

    fun isAdmin(): Boolean {
        return role?.id == 1 || role?.id == 2
    }
}

data class AdminUserSubscription(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("startDate") var startDate: String? = null,
    @SerializedName("endDate") var endDate: String? = null,
    @SerializedName("subscriptionPlan") var subscriptionPlan: AdminSubscriptionPlan? = null
) : java.io.Serializable

data class AdminSubscriptionPlan(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("planName") var planName: String? = null,
    @SerializedName("amount") var amount: String? = null,
    @SerializedName("duration") var duration: Int = 0
) : java.io.Serializable
