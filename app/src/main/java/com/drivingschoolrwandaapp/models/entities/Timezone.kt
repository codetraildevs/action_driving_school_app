package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Timezone(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("timezoneName") var timezoneName: String? = null,
    @SerializedName("utcOffset") var utcOffset: String? = null,
    @SerializedName("countryName") var countryName: String? = null
) : Serializable
