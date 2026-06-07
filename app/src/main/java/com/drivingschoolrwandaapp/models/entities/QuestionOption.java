package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class QuestionOption {

    @SerializedName("id")
    private int id;

    @SerializedName("questionId")
    private int questionId; // Added questionId

    @SerializedName("text")
    private String text;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("isCorrect")
    private boolean isCorrect;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    @SerializedName("questionOptionTranslations")
    private List<QuestionOptionTranslation> questionOptionTranslations;

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
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

    public List<QuestionOptionTranslation> getQuestionOptionTranslations() {
        return questionOptionTranslations;
    }

    public void setQuestionOptionTranslations(List<QuestionOptionTranslation> questionOptionTranslations) {
        this.questionOptionTranslations = questionOptionTranslations;
    }
}
