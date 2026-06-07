package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class UserTestAccess implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("userId")
    private int userId;

    @SerializedName("maxTest")
    private int maxTest;

    @SerializedName("expiresAt")
    private String expiresAt;

    @SerializedName("status")
    private String status; // ACTIVE, PENDING, INACTIVE

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    // Getters and Setters

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

    public int getMaxTest() {
        return maxTest;
    }

    public void setMaxTest(int maxTest) {
        this.maxTest = maxTest;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
