package com.drivingschoolrwandaapp.models.request;

import com.google.gson.annotations.SerializedName;

public class SubscriptionRequest {
    private int subscriptionPlanId;

    public SubscriptionRequest(int subscriptionPlanId) {
        this.subscriptionPlanId = subscriptionPlanId;
    }

    public int getSubscriptionPlanId() { return subscriptionPlanId; }
    public void setSubscriptionPlanId(int subscriptionPlanId) { this.subscriptionPlanId = subscriptionPlanId; }
}