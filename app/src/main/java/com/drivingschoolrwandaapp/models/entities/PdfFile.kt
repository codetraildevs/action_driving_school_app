package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class PdfFile(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("title") var title: String? = null,
    @SerializedName("filePath") var filePath: String? = null,
    @SerializedName("author") var author: String? = null,
    @SerializedName("totalPages") var totalPages: Int = 0,
    @SerializedName("description") var description: String? = null,
    @SerializedName("isPublic") var isPublic: Boolean = false,
    @SerializedName("uploadedAt") var uploadedAt: String? = null,
    @SerializedName("language") var language: Language? = null,
    @SerializedName("averageRating") var averageRating: Double = 0.0,
    @SerializedName("totalRatings") var totalRatings: Int = 0
) : Serializable
