package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TestQuestion {

    @SerializedName("id")
    private int id;

    private int testId;

    @SerializedName("questionText")
    private String questionText;

    @SerializedName("questionType")
    private String questionType;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("correctOptionId")
    private int correctOptionId;

    @SerializedName("options")
    private List<QuestionOption> options;

    @SerializedName("questionTranslations")
    private List<QuestionTranslation> questionTranslations;

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

    public int getTestId() {
        return testId;
    }

    public void setTestId(int testId) {
        this.testId = testId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getCorrectOptionId() {
        return correctOptionId;
    }

    public void setCorrectOptionId(int correctOptionId) {
        this.correctOptionId = correctOptionId;
    }

    public List<QuestionOption> getOptions() {
        return options;
    }

    public void setOptions(List<QuestionOption> options) {
        this.options = options;
    }

    public List<QuestionTranslation> getQuestionTranslations() {
        return questionTranslations;
    }

    public void setQuestionTranslations(List<QuestionTranslation> questionTranslations) {
        this.questionTranslations = questionTranslations;
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
