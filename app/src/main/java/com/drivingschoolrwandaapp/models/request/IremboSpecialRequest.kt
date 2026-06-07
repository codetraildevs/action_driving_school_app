package com.drivingschoolrwandaapp.models.request

import com.google.gson.annotations.SerializedName

data class IremboSpecialRequest(
    @SerializedName("category") var category: String = "",
    @SerializedName("firstName") var firstName: String = "",
    @SerializedName("lastName") var lastName: String = "",
    @SerializedName("phoneNumber") var phoneNumber: String = "",
    @SerializedName("nationalId") var nationalId: String = "",
    @SerializedName("address") var address: String = ""
)
