package com.drivingschoolrwandaapp.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted exam attempt result.
 *
 * Stored on the device the moment an exam is submitted so the user's score
 * history survives app restarts and is fully available offline. [date] is the
 * wall-clock completion timestamp (millis) and doubles as the primary key:
 * re-submissions at the same instant are ignored, so a re-rendered result can
 * never create duplicate history entries.
 */
@Entity(tableName = "test_results")
data class TestResultEntity(
    @PrimaryKey var date: Long = 0,
    var score: Int = 0,
    var totalMarks: Int = 0,
    var passed: Boolean = false,
    var testNumber: Int = 0,
    var testName: String? = null,
    var testId: Int = 0,
    var duration: Int = 0,
    var elapsedSeconds: Int = 0,
    var correctCount: Int = 0,
    var wrongCount: Int = 0,
    var skippedCount: Int = 0
)
