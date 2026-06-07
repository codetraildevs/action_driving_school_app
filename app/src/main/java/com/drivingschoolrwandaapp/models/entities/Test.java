package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Test {

    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("testNumber")
    private int testNumber; // Added testNumber

    @SerializedName("imageUrl")
    private String imageUrl; // Added imageUrl

    @SerializedName("totalMarks")
    private int totalMarks;

    @SerializedName("passMarks")
    private int passMarks;

    @SerializedName("duration")
    private int duration;

    @SerializedName("isFree")
    private boolean isFree; // Added isFree

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    @SerializedName("subscriptionId")
    private int subscriptionId;

    @SerializedName("subscription")
    private SubscriptionInfo subscription;

    @SerializedName("_count")
    private TestCount count;

    @SerializedName("questions")
    private List<TestQuestion> questions;

    @SerializedName("testTranslations")
    private List<TestTranslation> testTranslations; // Added translations

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getTestNumber() {
        return testNumber;
    }

    public void setTestNumber(int testNumber) {
        this.testNumber = testNumber;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public int getPassMarks() {
        return passMarks;
    }

    public void setPassMarks(int passMarks) {
        this.passMarks = passMarks;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public boolean isFree() {
        return isFree;
    }

    public void setFree(boolean free) {
        isFree = free;
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

    public int getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(int subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public SubscriptionInfo getSubscription() {
        return subscription;
    }

    public void setSubscription(SubscriptionInfo subscription) {
        this.subscription = subscription;
    }

    public TestCount getCount() {
        return count;
    }

    public void setCount(TestCount count) {
        this.count = count;
    }

    public List<TestQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<TestQuestion> questions) {
        this.questions = questions;
    }

    public List<TestTranslation> getTestTranslations() {
        return testTranslations;
    }

    public void setTestTranslations(List<TestTranslation> testTranslations) {
        this.testTranslations = testTranslations;
    }

    // Inner classes for nested JSON objects

    public static class SubscriptionInfo {
        @SerializedName("planName")
        private String planName;

        public String getPlanName() {
            return planName;
        }

        public void setPlanName(String planName) {
            this.planName = planName;
        }
    }

    public static class TestCount {
        @SerializedName("testQuestions")
        private int testQuestions;

        public int getTestQuestions() {
            return testQuestions;
        }

        public void setTestQuestions(int testQuestions) {
            this.testQuestions = testQuestions;
        }
    }
}
