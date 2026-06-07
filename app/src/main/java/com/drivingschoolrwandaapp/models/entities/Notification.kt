package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Notification(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("title") var title: String? = null,
    @SerializedName("message") var message: String? = null,
    @SerializedName("isRead") var isRead: Boolean = false,
    @SerializedName("createdAt") var createdAt: String? = null
) : Serializable
