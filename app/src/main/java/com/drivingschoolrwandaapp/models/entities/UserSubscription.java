package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;

public class UserSubscription {

    @SerializedName("id")
    private int id;

    @SerializedName("userId")
    private int userId;

    @SerializedName("subscriptionPlanId")
    private int subscriptionPlanId;

    @SerializedName("subscriptionPlan")
    private SubscriptionPlan subscriptionPlan;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getSubscriptionPlanId() {
        return subscriptionPlanId;
    }

    public void setSubscriptionPlanId(int subscriptionPlanId) {
        this.subscriptionPlanId = subscriptionPlanId;
    }

    public SubscriptionPlan getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public void setSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }
}
