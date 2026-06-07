package com.drivingschoolrwandaapp.models.request

import com.google.gson.annotations.SerializedName

data class FirebaseTokenRequest(
    @SerializedName("token") var token: String = ""
)
