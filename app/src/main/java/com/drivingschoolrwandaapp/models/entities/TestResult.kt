package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName

data class TestResult(
    val score: Int,
    val totalMarks: Int,
    @get:JvmName("isPassed") val passed: Boolean,
    val testNumber: Int = 0,
    val testName: String = "",
    val testId: Int = 0
)
