package com.drivingschoolrwandaapp.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.drivingschoolrwandaapp.models.entities.QuestionOptionTranslation
import com.drivingschoolrwandaapp.utils.DataConverter

@Entity(
    tableName = "question_options",
    foreignKeys = [ForeignKey(
        entity = TestQuestionEntity::class,
        parentColumns = ["id"],
        childColumns = ["questionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["questionId"])]
)
data class QuestionOptionEntity(
    @PrimaryKey var id: Int = 0,
    var questionId: Int = 0,
    var text: String? = null,
    var imageUrl: String? = null,
    var isCorrect: Boolean = false,
    @TypeConverters(DataConverter::class)
    var questionOptionTranslations: List<QuestionOptionTranslation>? = null
)
