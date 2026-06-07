package com.drivingschoolrwandaapp.models

import com.google.gson.annotations.SerializedName

/** Root JSON structure: { "exams": [...] } */
data class LocalExamWrapper(
    @SerializedName("exams") val exams: List<LocalExam>
)

/** Represents a single exam from the JSON file */
data class LocalExam(
    @SerializedName("quizId") val quizId: String,
    @SerializedName("examType") val examType: String,
    @SerializedName("title") val title: String,
    @SerializedName("examImgUrl") val examImgUrl: String,
    @SerializedName("questions") val questions: List<LocalQuestion>
)

/** Represents a single question from the JSON file */
data class LocalQuestion(
    @SerializedName("question") val question: String,
    @SerializedName("option1") val option1: String,
    @SerializedName("option2") val option2: String,
    @SerializedName("option3") val option3: String,
    @SerializedName("option4") val option4: String,
    @SerializedName("correctAnswer") val correctAnswer: String,
    @SerializedName("questionImgUrl") val questionImgUrl: String
) {
    /** Get all option texts as a list */
    fun getOptions(): List<String> = listOf(option1, option2, option3, option4)

    /** Get the index (0-3) of the correct answer option */
    fun getCorrectOptionIndex(): Int {
        val options = getOptions()
        return options.indexOfFirst { it.equals(correctAnswer, ignoreCase = true) }
            .coerceIn(0, options.size - 1)
    }
}
