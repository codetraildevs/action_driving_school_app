package com.drivingschoolrwandaapp.models.request

import com.google.gson.annotations.SerializedName

data class RatingRequest(
    @SerializedName("pdfId") var pdfId: Int = 0,
    @SerializedName("rating") var rating: Double = 0.0,
    @SerializedName("comment") var comment: String? = null
)
