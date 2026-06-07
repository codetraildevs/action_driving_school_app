package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class QuestionOptionTranslation(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("optionId") var optionId: Int = 0,
    @SerializedName("languageId") var languageId: Int = 0,
    @SerializedName("text") var text: String? = null
) : Serializable
