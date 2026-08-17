package com.drivingschoolrwandaapp.models

import java.io.Serializable

data class IremboApplication(
    val type: String? = null,
    val title: String,
    val reference: String,
    val status: String,
    val date: String,
    val message: String,
    val completionPercentage: Int,
    val currentStep: String
) : Serializable
