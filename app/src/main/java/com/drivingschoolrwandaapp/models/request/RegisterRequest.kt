package com.drivingschoolrwandaapp.models.request

data class RegisterRequest(
    var firstName: String = "",
    var middleName: String? = null,
    var lastName: String? = null,
    var email: String = "",
    var phoneNumber: String = "",
    var password: String = "",
    var dob: String = "",
    var languageId: Int = 1,
    var timezoneId: Int = 1,
    var device: DeviceInfoRequest? = null
)
