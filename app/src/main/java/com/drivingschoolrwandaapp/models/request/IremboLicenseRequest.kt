package com.drivingschoolrwandaapp.models.request

import com.google.gson.annotations.SerializedName

data class IremboLicenseRequest(
    @SerializedName("category") var category: String = "",
    @SerializedName("licenseType") var licenseType: String = "",
    @SerializedName("applicationType") var applicationType: String = "",
    @SerializedName("applicantName") var applicantName: String = "",
    @SerializedName("applicantPhoneNumber") var applicantPhoneNumber: String = "",
    @SerializedName("applicantNationalId") var applicantNationalId: String = "",
    @SerializedName("address") var address: String = ""
)
