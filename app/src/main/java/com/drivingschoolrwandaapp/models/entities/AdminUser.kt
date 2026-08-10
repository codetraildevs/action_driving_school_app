package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName

/** Wrapper for GET admin/users → { success, data: [AdminUser] }. */
data class AdminUsersResponse(
    @SerializedName("success") var success: Boolean = false,
    @SerializedName("message") var message: String? = null,
    @SerializedName("data") var data: List<AdminUser>? = null
) : java.io.Serializable

data class AdminUser(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("firstName") var firstName: String? = null,
    @SerializedName("middleName") var middleName: String? = null,
    @SerializedName("lastName") var lastName: String? = null,
    @SerializedName("email") var email: String? = null,
    @SerializedName("phoneNumber") var phoneNumber: String? = null,
    @SerializedName("isActive") var isActive: Boolean = false,
    @SerializedName("createdAt") var createdAt: String? = null,
    @SerializedName("role") var role: AdminRole? = null
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

data class AdminRole(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("roleName") var roleName: String? = null,
    @SerializedName("description") var description: String? = null
) : java.io.Serializable
