package com.drivingschoolrwandaapp.database.entities

import androidx.room.Embedded
import androidx.room.Relation

data class UserSubscriptionWithPlan(
    @Embedded var userSubscription: UserSubscriptionEntity? = null,
    @Relation(
        parentColumn = "subscriptionPlanId",
        entityColumn = "id"
    )
    var subscriptionPlan: SubscriptionPlan? = null
)
