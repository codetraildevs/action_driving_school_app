package com.drivingschoolrwandaapp.models.response;

import com.drivingschoolrwandaapp.models.entities.UserSubscription;
import com.google.gson.annotations.SerializedName;

public class UserSubscriptionResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private UserSubscription data;

    public boolean isSuccess() {
        return success;
    }

    public UserSubscription getData() {
        return data;
    }
}
