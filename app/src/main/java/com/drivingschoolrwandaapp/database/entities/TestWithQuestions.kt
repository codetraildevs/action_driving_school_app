package com.drivingschoolrwandaapp.database.entities

import androidx.room.Embedded
import androidx.room.Relation

data class TestWithQuestions(
    @JvmField @Embedded var test: TestEntity? = null,
    @JvmField @Relation(
        entity = TestQuestionEntity::class,
        parentColumn = "id",
        entityColumn = "testId"
    )
    var questions: List<QuestionWithOptions>? = null
)
