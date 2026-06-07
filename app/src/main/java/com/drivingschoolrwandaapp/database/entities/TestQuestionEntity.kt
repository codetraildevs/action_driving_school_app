package com.drivingschoolrwandaapp.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.drivingschoolrwandaapp.models.entities.QuestionTranslation
import com.drivingschoolrwandaapp.utils.DataConverter

@Entity(
    tableName = "test_questions",
    foreignKeys = [ForeignKey(
        entity = TestEntity::class,
        parentColumns = ["id"],
        childColumns = ["testId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["testId"])]
)
data class TestQuestionEntity(
    @PrimaryKey var id: Int = 0,
    var testId: Int = 0,
    var questionText: String? = null,
    var questionType: String? = null,
    var imageUrl: String? = null,
    @TypeConverters(DataConverter::class)
    var questionTranslations: List<QuestionTranslation>? = null
)
