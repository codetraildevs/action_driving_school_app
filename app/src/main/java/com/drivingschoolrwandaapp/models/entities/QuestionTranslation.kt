package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class QuestionTranslation(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("questionId") var questionId: Int = 0,
    @SerializedName("languageId") var languageId: Int = 0,
    @SerializedName("questionText") var questionText: String? = null,
    @SerializedName("imageUrl") var imageUrl: String? = null
) : Serializable
