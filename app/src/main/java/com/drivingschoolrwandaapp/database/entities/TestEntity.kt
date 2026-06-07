package com.drivingschoolrwandaapp.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.drivingschoolrwandaapp.models.entities.TestTranslation
import com.drivingschoolrwandaapp.utils.DataConverter

@Entity(tableName = "tests")
data class TestEntity(
    @PrimaryKey var id: Int = 0,
    var title: String? = null,
    var description: String? = null,
    var testNumber: Int = 0,
    var imageUrl: String? = null,
    var totalMarks: Int = 0,
    var passMarks: Int = 0,
    var duration: Int = 0,
    var isFree: Boolean = false,
    var questionCount: Int = 0,
    var lastRefreshed: Long = 0L,
    @TypeConverters(DataConverter::class)
    var testTranslations: List<TestTranslation>? = null
)
