package com.drivingschoolrwandaapp.database.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.drivingschoolrwandaapp.models.entities.QuestionOptionTranslation;
import com.drivingschoolrwandaapp.utils.DataConverter;

import java.util.List;

@Entity(tableName = "question_options",
        foreignKeys = @ForeignKey(entity = TestQuestionEntity.class,
                                  parentColumns = "id",
                                  childColumns = "questionId",
                                  onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "questionId")})
public class QuestionOptionEntity {

    @PrimaryKey
    private int id;
    private int questionId;
    private String text;
    private String imageUrl;
    private boolean isCorrect;

    @TypeConverters(DataConverter.class)
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

    public List<QuestionOptionTranslation> getQuestionOptionTranslations() {
        return questionOptionTranslations;
    }

    public void setQuestionOptionTranslations(List<QuestionOptionTranslation> questionOptionTranslations) {
        this.questionOptionTranslations = questionOptionTranslations;
    }
}
