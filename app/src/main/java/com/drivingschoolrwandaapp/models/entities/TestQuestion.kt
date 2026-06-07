package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName

data class TestQuestion(
    @SerializedName("id") var id: Int = 0,
    var testId: Int = 0,
    @SerializedName("questionText") var questionText: String? = null,
    @SerializedName("questionType") var questionType: String? = null,
    @SerializedName("imageUrl") var imageUrl: String? = null,
    @SerializedName("correctOptionId") var correctOptionId: Int = 0,
    @SerializedName("options") var options: List<QuestionOption>? = null,
    @SerializedName("questionTranslations") var questionTranslations: List<QuestionTranslation>? = null,
    @SerializedName("createdAt") var createdAt: String? = null,
    @SerializedName("updatedAt") var updatedAt: String? = null
)
