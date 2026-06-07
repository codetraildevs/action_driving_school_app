package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Device(
    @SerializedName("physicalAddress") var physicalAddress: String? = null,
    @SerializedName("manufacturer") var manufacturer: String? = null,
    @SerializedName("model") var model: String? = null,
    @SerializedName("name") var name: String? = null
) : Serializable
