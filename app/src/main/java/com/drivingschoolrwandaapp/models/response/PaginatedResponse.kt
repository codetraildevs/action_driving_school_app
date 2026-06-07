package com.drivingschoolrwandaapp.models.response

data class PaginatedResponse<T>(
    var items: List<T>? = null,
    var currentPage: Int = 0,
    var totalPages: Int = 0,
    var totalItems: Int = 0,
    var hasNext: Boolean = false
)
