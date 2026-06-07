package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Question implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("questionText")
    private String questionText;

    @SerializedName("questionType")
    private String questionType;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("fromPage")
    private int fromPage;

    @SerializedName("toPage")
    private int toPage;

    @SerializedName("options")
    private List<QuestionOption> options;

    // Getters and Setters
    public int getId() { return id; }
    public String getQuestionText() { return questionText; }
    public String getQuestionType() { return questionType; }
    public String getImageUrl() { return imageUrl; }
    public int getFromPage() { return fromPage; }
    public int getToPage() { return toPage; }
    public List<QuestionOption> getOptions() { return options; }
    public void setOptions(List<QuestionOption> options) { this.options = options; }
}
