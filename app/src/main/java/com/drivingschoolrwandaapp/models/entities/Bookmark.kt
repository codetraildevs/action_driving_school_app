package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Bookmark(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("pdfId") var pdfId: Int = 0,
    @SerializedName("pdf") var pdf: PdfFile? = null,
    @SerializedName("pageNumber") var pageNumber: Int = 0,
    @SerializedName("note") var note: String? = null,
    @SerializedName("createdAt") var createdAt: String? = null
) : Serializable
