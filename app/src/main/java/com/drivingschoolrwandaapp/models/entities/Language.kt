package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Language(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("languageCode") var languageCode: String? = null,
    @SerializedName("languageName") var languageName: String? = null,
    @SerializedName("nativeName") var nativeName: String? = null,
    @SerializedName("isActive") var isActive: Boolean = false
) : Serializable
