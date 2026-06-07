package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class QuestionOptionTranslation implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("optionId")
    private int optionId;

    @SerializedName("languageId")
    private int languageId;

    @SerializedName("text")
    private String text;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOptionId() {
        return optionId;
    }

    public void setOptionId(int optionId) {
        this.optionId = optionId;
    }

    public int getLanguageId() {
        return languageId;
    }

    public void setLanguageId(int languageId) {
        this.languageId = languageId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
