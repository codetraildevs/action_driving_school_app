package com.drivingschoolrwandaapp.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_materials")
data class LearningMaterial(
    @PrimaryKey var id: Int = 0,
    var title: String? = null,
    var description: String? = null,
    var filePath: String? = null,
    var fileType: String? = null,
    var thumbnailUrl: String? = null,
    var isPublic: Boolean = false,
    var createdAt: String? = null,
    var updatedAt: String? = null,
    var localPath: String? = null
)
