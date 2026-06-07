package com.drivingschoolrwandaapp.models.request

data class UpdateBookmarkRequest(
    var pageNumber: Int? = null,
    var note: String? = null
)
