package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class TestTranslation(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("testId") var testId: Int = 0,
    @SerializedName("languageId") var languageId: Int = 0,
    @SerializedName("title") var title: String? = null,
    @SerializedName("description") var description: String? = null,
    @SerializedName("imageUrl") var imageUrl: String? = null
) : Serializable
