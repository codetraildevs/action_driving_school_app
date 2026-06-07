package com.drivingschoolrwandaapp.database.entities

import kotlin.jvm.JvmField

data class Bookmark(
    @JvmField var id: Int = 0,
    @JvmField var pdfId: Int = 0,
    @JvmField var pageNumber: Int = 0,
    @JvmField var name: String? = null
)
