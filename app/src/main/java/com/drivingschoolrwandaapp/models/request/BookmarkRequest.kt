package com.drivingschoolrwandaapp.models.request

data class BookmarkRequest(
    var pageNumber: Int = 0,
    var note: String? = null
)
