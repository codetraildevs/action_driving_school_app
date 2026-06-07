package com.drivingschoolrwandaapp.models.request

data class ReadingSessionRequest(
    var pdfId: Int = 0,
    var currentPage: Int = 0,
    var totalPages: Int = 0
)
