package com.drivingschoolrwandaapp.database.entities;

import androidx.room.Embedded;
import androidx.room.Relation;

public class UserSubscriptionWithPlan {
    @Embedded
    public UserSubscriptionEntity userSubscription;

    @Relation(
         parentColumn = "subscriptionPlanId",
         entityColumn = "id"
    )
    public SubscriptionPlan subscriptionPlan;
}
