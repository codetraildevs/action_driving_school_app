package com.drivingschoolrwandaapp.models.response

import com.google.gson.annotations.SerializedName
import com.drivingschoolrwandaapp.models.entities.PdfFile

/**
 * Response of GET /api/files (used by the PDF library with type=pdf).
 * The backend returns { data: [File], total, page, pageSize, totalPages } —
 * a raw list under "data", not wrapped in ApiResponse/PaginatedResponse.
 */
data class PdfFilesResponse(
    @SerializedName("data") var files: List<PdfFile>? = null,
    @SerializedName("total") var total: Int = 0,
    @SerializedName("page") var currentPage: Int = 0,
    @SerializedName("pageSize") var pageSize: Int = 0,
    @SerializedName("totalPages") var totalPages: Int = 0
)
