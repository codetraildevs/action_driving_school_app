package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName

/**
 * A single row in the global leaderboard.
 *
 * The backend returns a ranked list of users with their aggregate exam stats.
 * [rank] is 1-based (1 = top scorer). [averageScore] is the mean percentage
 * across all completed exams.
 */
data class LeaderboardEntry(
    @SerializedName("rank") val rank: Int = 0,
    @SerializedName("userId") val userId: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("averageScore") val averageScore: Int = 0,
    @SerializedName("examsTaken") val examsTaken: Int = 0,
    @SerializedName("bestScore") val bestScore: Int = 0,
    @SerializedName("passRate") val passRate: Int = 0
)
