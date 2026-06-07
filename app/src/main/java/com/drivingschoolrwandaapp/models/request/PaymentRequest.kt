package com.drivingschoolrwandaapp.models.request

data class PaymentRequest(
    var subscriptionId: Int = 0,
    var method: String = "",
    var refId: String = ""
)
