package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ReadingSession(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("pdfId") var pdfId: Int = 0,
    @SerializedName("currentPage") var currentPage: Int = 0,
    @SerializedName("totalPages") var totalPages: Int = 0,
    @SerializedName("pdf") var pdf: PdfFile? = null,
    @SerializedName("createdAt") var createdAt: String? = null,
    @SerializedName("updatedAt") var updatedAt: String? = null
) : Serializable {

    fun getProgressPercentage(): Int {
        if (totalPages == 0) return 0
        return ((currentPage.toFloat() / totalPages) * 100).toInt()
    }
}
