package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Address(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("village") var village: String? = null,
    @SerializedName("cell") var cell: String? = null,
    @SerializedName("sector") var sector: String? = null,
    @SerializedName("district") var district: String? = null,
    @SerializedName("province") var province: String? = null
) : Serializable {

    fun getFullAddress(): String {
        return "$village, $cell, $sector, $district, $province"
    }
}
