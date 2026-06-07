package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Transaction implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("subscriptionPlan")
    private SubscriptionPlan subscriptionPlan;

    @SerializedName("paid")
    private boolean paid;

    @SerializedName("method")
    private String method;

    @SerializedName("refId")
    private String refId;

    @SerializedName("amount")
    private double amount;

    @SerializedName("createdAt")
    private String createdAt;

    // Getters and Setters
    public int getId() { return id; }
    public SubscriptionPlan getSubscriptionPlan() { return subscriptionPlan; }
    public boolean isPaid() { return paid; }
    public String getMethod() { return method; }
    public String getRefId() { return refId; }
    public double getAmount() { return amount; }
    public String getCreatedAt() { return createdAt; }

    public String getFormattedAmount() {
        return String.format("RWF %.2f", amount);
    }
}
