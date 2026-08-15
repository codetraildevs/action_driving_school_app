package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// Maps the backend File model (GET /api/files). The backend File has
// { id, name, description, filePath, fileType, fileSize, thumbnailUrl,
//   folderId, createdAt, updatedAt } — fields without a backend counterpart
// (author, totalPages, ratings, language, isPublic) keep their defaults.
data class PdfFile(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("name") var title: String? = null,
    @SerializedName("filePath") var filePath: String? = null,
    @SerializedName("author") var author: String? = null,
    @SerializedName("totalPages") var totalPages: Int = 0,
    @SerializedName("description") var description: String? = null,
    @SerializedName("isPublic") var isPublic: Boolean = false,
    @SerializedName("createdAt") var uploadedAt: String? = null,
    @SerializedName("language") var language: Language? = null,
    @SerializedName("averageRating") var averageRating: Double = 0.0,
    @SerializedName("totalRatings") var totalRatings: Int = 0
) : Serializable
