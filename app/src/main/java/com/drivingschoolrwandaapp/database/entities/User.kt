package com.drivingschoolrwandaapp.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "users")
data class User(
    @PrimaryKey var id: Int = 0,
    var firstName: String? = null,
    var middleName: String? = null,
    var lastName: String? = null,
    var email: String? = null,
    var phoneNumber: String? = null,
    var dob: Date? = null,
    var isActive: Boolean = false,
    var profilePicture: String? = null,
    var roleId: Int = 0,
    var languageId: Int = 0,
    var timezoneId: Int = 0,
    var createdAt: String? = null,
    var maxTestAccess: Int = 0,
    var testAccessExpiresAt: String? = null,
    var testAccessStatus: String? = null
) {
    fun getLanguage(): String {
        return when (languageId) {
            41 -> "English"
            48 -> "Français"
            else -> "Kinyarwanda"
        }
    }

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
