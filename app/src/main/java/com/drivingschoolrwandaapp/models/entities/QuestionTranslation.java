package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class QuestionTranslation implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("questionId")
    private int questionId;

    @SerializedName("languageId")
    private int languageId;

    @SerializedName("questionText")
    private String questionText;

    @SerializedName("imageUrl")
    private String imageUrl;

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

    public int getLanguageId() {
        return languageId;
    }

    public void setLanguageId(int languageId) {
        this.languageId = languageId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
