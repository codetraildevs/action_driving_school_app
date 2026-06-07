package com.drivingschoolrwandaapp.models.request

data class LoginRequest(
    var identifier: String = "",
    var password: String = "",
    var deviceId: String = ""
)
