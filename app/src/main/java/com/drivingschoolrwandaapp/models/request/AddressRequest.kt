package com.drivingschoolrwandaapp.models.request

import com.google.gson.annotations.SerializedName

data class AddressRequest(
    var village: String? = null,
    var cell: String? = null,
    var sector: String? = null,
    var district: String? = null,
    var province: String? = null
)
