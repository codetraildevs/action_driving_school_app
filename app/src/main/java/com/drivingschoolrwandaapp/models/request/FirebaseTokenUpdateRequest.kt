package com.drivingschoolrwandaapp.models.request

data class FirebaseTokenUpdateRequest(
    var oldToken: String = "",
    var newToken: String = ""
)
