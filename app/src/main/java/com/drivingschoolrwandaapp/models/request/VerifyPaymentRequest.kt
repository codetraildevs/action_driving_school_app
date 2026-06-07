package com.drivingschoolrwandaapp.models.request

data class VerifyPaymentRequest(
    var refId: String = "",
    var subscriptionId: Int = 0
)
