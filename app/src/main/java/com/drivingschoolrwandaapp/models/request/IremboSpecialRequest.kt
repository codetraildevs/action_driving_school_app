package com.drivingschoolrwandaapp.models.request

import com.google.gson.annotations.SerializedName

data class IremboSpecialRequest(
    @SerializedName("serviceName") var serviceName: String = "",
    @SerializedName("category") var category: String = "",
    @SerializedName("applicantName") var applicantName: String = "",
    @SerializedName("applicantPhone") var applicantPhone: String = "",
    @SerializedName("nationalId") var nationalId: String = "",
    @SerializedName("description") var description: String = ""
)
