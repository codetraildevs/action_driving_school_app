package com.drivingschoolrwandaapp.models.request

data class DeviceInfoRequest(
    var physicalAddress: String = "",
    var manufacturer: String = "",
    var model: String = "",
    var name: String = ""
)
