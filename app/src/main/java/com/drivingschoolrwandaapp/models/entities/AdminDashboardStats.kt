package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName

/** Wrapper for GET admin/analytics/dashboard → { data: AdminDashboardStats }. */
data class AdminDashboardResponse(
    @SerializedName("data") var data: AdminDashboardStats? = null
) : java.io.Serializable

data class AdminDashboardStats(
    @SerializedName("totalUsers") var totalUsers: Long = 0,
    @SerializedName("activeUsers") var activeUsers: Long = 0,
    @SerializedName("totalSubscriptions") var totalSubscriptions: Long = 0,
    @SerializedName("totalContent") var totalContent: Long = 0,
    @SerializedName("totalTests") var totalTests: Long = 0,
    @SerializedName("totalLearningMaterials") var totalLearningMaterials: Long = 0,
    @SerializedName("totalPdfFiles") var totalPdfFiles: Long = 0,
    @SerializedName("recentActivity") var recentActivity: List<AdminRecentActivity>? = null,
    @SerializedName("popularContent") var popularContent: List<AdminPopularContent>? = null
) : java.io.Serializable

data class AdminRecentActivity(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("type") var type: String? = null,
    @SerializedName("title") var title: String? = null,
    @SerializedName("description") var description: String? = null,
    @SerializedName("timestamp") var timestamp: String? = null
) : java.io.Serializable

data class AdminPopularContent(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("title") var title: String? = null,
    @SerializedName("type") var type: String? = null,
    @SerializedName("downloads") var downloads: Long = 0,
    @SerializedName("fileType") var fileType: String? = null
) : java.io.Serializable
