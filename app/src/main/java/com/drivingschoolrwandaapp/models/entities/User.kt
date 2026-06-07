package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date

data class User(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("firstName") var firstName: String? = null,
    @SerializedName("middleName") var middleName: String? = null,
    @SerializedName("lastName") var lastName: String? = null,
    @SerializedName("email") var email: String? = null,
    @SerializedName("phoneNumber") var phoneNumber: String? = null,
    @SerializedName("dob") var dob: Date? = null,
    @SerializedName("isActive") var isActive: Boolean = false,
    @SerializedName("profilePicture") var profilePicture: String? = null,
    @SerializedName("password") var password: String? = null,
    @SerializedName("timezone") var timezone: String? = null,
    @SerializedName("device") var device: Device? = null,
    @SerializedName("language") var language: String? = null,
    @SerializedName("languageId") var languageId: Int = 0,
    @SerializedName("createdAt") var createdAt: String? = null,
    @SerializedName("role") var role: Int = 0,
    @SerializedName("userTestAccess") var userTestAccess: UserTestAccess? = null
) : Serializable {

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
}
