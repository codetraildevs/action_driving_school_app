package com.drivingschoolrwandaapp.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscription_plans")
data class SubscriptionPlan(
    @PrimaryKey var id: Int = 0,
    var planName: String? = null,
    var amount: String? = null,
    var duration: Int = 0
)
