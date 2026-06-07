package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UserActivity(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("activityType") var activityType: String? = null,
    @SerializedName("description") var description: String? = null,
    @SerializedName("createdAt") var createdAt: String? = null
) : Serializable
