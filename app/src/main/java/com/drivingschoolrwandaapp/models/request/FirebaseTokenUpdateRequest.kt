package com.drivingschoolrwandaapp.models.request

import com.google.gson.annotations.SerializedName

data class FirebaseTokenUpdateRequest(
    @SerializedName("firebaseToken") var firebaseToken: String = "",
    @SerializedName("deviceId") var deviceId: String = ""
)
