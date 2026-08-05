package com.drivingschoolrwandaapp.models.request

import com.google.gson.annotations.SerializedName

data class BookmarkRequest(
    @SerializedName("pageNumber") var pageNumber: Int = 0,
    @SerializedName("note") var note: String? = null
)
