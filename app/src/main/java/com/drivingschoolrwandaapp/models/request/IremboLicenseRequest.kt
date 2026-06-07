package com.drivingschoolrwandaapp.models.request

data class IremboLicenseRequest(
    var category: String = "",
    var licenseType: String = "",
    var applicationType: String = "",
    var applicantName: String = "",
    var applicantPhoneNumber: String = "",
    var applicantNationalId: String = "",
    var address: String = ""
)
