package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName

/**
 * A single Irembo request from GET admin/requests (bare list).
 * type is "DRIVING_LICENSE" or "SPECIAL".
 */
data class AdminRequest(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("type") var type: String? = null,
    @SerializedName("title") var title: String? = null,
    @SerializedName("status") var status: String? = null,
    @SerializedName("message") var message: String? = null,
    @SerializedName("completionPercentage") var completionPercentage: Int = 0,
    @SerializedName("updatedAt") var updatedAt: String? = null
) : java.io.Serializable
