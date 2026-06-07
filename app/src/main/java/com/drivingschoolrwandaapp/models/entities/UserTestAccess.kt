package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UserTestAccess(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("userId") var userId: Int = 0,
    @SerializedName("maxTest") var maxTest: Int = 0,
    @SerializedName("expiresAt") var expiresAt: String? = null,
    @SerializedName("status") var status: String? = null,
    @SerializedName("createdAt") var createdAt: String? = null,
    @SerializedName("updatedAt") var updatedAt: String? = null
) : Serializable
