package com.drivingschoolrwandaapp.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user_subscription")
data class UserSubscriptionEntity(
    @PrimaryKey var id: Int = 0,
    var userId: Int = 0,
    var subscriptionPlanId: Int = 0,
    var startDate: Date? = null,
    var endDate: Date? = null,
    var status: String? = null
)
