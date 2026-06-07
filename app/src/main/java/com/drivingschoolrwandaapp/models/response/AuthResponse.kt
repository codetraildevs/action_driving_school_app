package com.drivingschoolrwandaapp.models.response

data class AuthResponse(
    var success: Boolean = false,
    var message: String? = null,
    var data: LoginResponse? = null
)
