package com.drivingschoolrwandaapp.models.request

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("identifier") var identifier: String = "",
    @SerializedName("password") var password: String = "",
    @SerializedName("deviceId") var deviceId: String = "",
    // Marks this request as coming from the native Android app so the backend
    // can allow phone-only admin login (shared admin login) while still
    // requiring the real password from the web console.
    @SerializedName("clientType") var clientType: String = CLIENT_TYPE_ANDROID_APP
) {
    companion object {
        /** Marker sent with every login from the native Android app. */
        const val CLIENT_TYPE_ANDROID_APP = "android_app"
    }
}
