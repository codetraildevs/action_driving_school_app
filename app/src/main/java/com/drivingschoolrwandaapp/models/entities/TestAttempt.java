package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class TestAttempt implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("testId")
    private int testId;

    @SerializedName("test")
    private Test test;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("endTime")
    private String endTime;

    @SerializedName("totalScore")
    private double totalScore;

    @SerializedName("passed")
    private boolean passed;

    @SerializedName("status")
    private String status;

    // Getters and Setters
    public int getId() { return id; }
    public int getTestId() { return testId; }
    public Test getTest() { return test; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public double getTotalScore() { return totalScore; }
    public boolean isPassed() { return passed; }
    public String getStatus() { return status; }
}