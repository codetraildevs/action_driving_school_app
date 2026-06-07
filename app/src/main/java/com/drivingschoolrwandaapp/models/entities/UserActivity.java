package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class UserActivity implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("activityType")
    private String activityType;

    @SerializedName("description")
    private String description;

    @SerializedName("createdAt")
    private String createdAt;

    // Getters and Setters
    public int getId() { return id; }
    public String getActivityType() { return activityType; }
    public String getDescription() { return description; }
    public String getCreatedAt() { return createdAt; }
}
