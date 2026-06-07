package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName

data class TestResult(
    val score: Int,
    val totalMarks: Int,
    @get:JvmName("isPassed") val passed: Boolean
)
