package com.drivingschoolrwandaapp.models.request

import com.google.gson.annotations.SerializedName

data class ProfileUpdateRequest(
    var firstName: String? = null,
    var middleName: String? = null,
    var lastName: String? = null,
    var profilePicture: String? = null,
    var languageId: Int? = null,
    var timezoneId: Int? = null
)
