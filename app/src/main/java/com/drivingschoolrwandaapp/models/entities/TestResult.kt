package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName

data class TestResult(
    val score: Int,
    val totalMarks: Int,
    @get:JvmName("isPassed") val passed: Boolean,
    val testNumber: Int = 0,
    val testName: String = "",
    val testId: Int = 0,
    val date: Long = 0,
    val duration: Int = 0,
    val elapsedSeconds: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val skippedCount: Int = 0
)
