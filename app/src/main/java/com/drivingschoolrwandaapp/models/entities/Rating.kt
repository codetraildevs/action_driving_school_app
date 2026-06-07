package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName

data class Rating(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("pdfId") var pdfId: Int = 0,
    @SerializedName("rating") var rating: Double = 0.0,
    @SerializedName("comment") var comment: String? = null,
    @SerializedName("user") var user: User? = null,
    @SerializedName("createdAt") var createdAt: String? = null
) : java.io.Serializable
