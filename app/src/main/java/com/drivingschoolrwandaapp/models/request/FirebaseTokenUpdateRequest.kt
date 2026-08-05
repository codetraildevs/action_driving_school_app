package com.drivingschoolrwandaapp.models.request

import com.google.gson.annotations.SerializedName

data class FirebaseTokenUpdateRequest(
    @SerializedName("oldToken") var oldToken: String = "",
    @SerializedName("newToken") var newToken: String = ""
)
