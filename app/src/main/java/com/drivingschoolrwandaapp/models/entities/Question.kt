package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Question(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("questionText") var questionText: String? = null,
    @SerializedName("questionType") var questionType: String? = null,
    @SerializedName("imageUrl") var imageUrl: String? = null,
    @SerializedName("fromPage") var fromPage: Int = 0,
    @SerializedName("toPage") var toPage: Int = 0,
    @SerializedName("options") var options: List<QuestionOption>? = null
) : Serializable
