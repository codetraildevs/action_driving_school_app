package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName

data class QuestionOption(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("questionId") var questionId: Int = 0,
    @SerializedName("text") var text: String? = null,
    @SerializedName("imageUrl") var imageUrl: String? = null,
    @SerializedName("isCorrect") var isCorrect: Boolean = false,
    @SerializedName("createdAt") var createdAt: String? = null,
    @SerializedName("updatedAt") var updatedAt: String? = null,
    @SerializedName("questionOptionTranslations") var questionOptionTranslations: List<QuestionOptionTranslation>? = null
)
