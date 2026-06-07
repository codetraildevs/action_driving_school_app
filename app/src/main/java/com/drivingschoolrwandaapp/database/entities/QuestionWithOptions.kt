package com.drivingschoolrwandaapp.database.entities

import androidx.room.Embedded
import androidx.room.Relation

data class QuestionWithOptions(
    @JvmField @Embedded var question: TestQuestionEntity? = null,
    @JvmField @Relation(
        parentColumn = "id",
        entityColumn = "questionId"
    )
    var options: List<QuestionOptionEntity>? = null
)
