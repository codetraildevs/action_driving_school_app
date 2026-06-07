package com.drivingschoolrwandaapp.models.response;

import com.drivingschoolrwandaapp.models.entities.SubscriptionPlan;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SubscriptionPlansResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private List<SubscriptionPlan> data;

    public boolean isSuccess() {
        return success;
    }

    public List<SubscriptionPlan> getData() {
        return data;
    }
}
