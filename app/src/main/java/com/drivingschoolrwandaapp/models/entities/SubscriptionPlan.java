package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionPlan {

    @SerializedName("id")
    private int id;

    @SerializedName("planName")
    private String planName;

    @SerializedName("amount")
    private String amount;

    @SerializedName("duration")
    private int duration;
    
    @SerializedName("maxTestAccess")
    private int maxTestAccess; // Added for new logic

    @SerializedName("permissions")
    private List<Permission> permissions = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public int getMaxTestAccess() {
        return maxTestAccess;
    }

    public void setMaxTestAccess(int maxTestAccess) {
        this.maxTestAccess = maxTestAccess;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }
}
