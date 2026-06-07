package com.drivingschoolrwandaapp.database.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.drivingschoolrwandaapp.models.entities.QuestionTranslation;
import com.drivingschoolrwandaapp.utils.DataConverter;

import java.util.List;

@Entity(tableName = "test_questions",
        foreignKeys = @ForeignKey(entity = TestEntity.class,
                                  parentColumns = "id",
                                  childColumns = "testId",
                                  onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "testId")})
public class TestQuestionEntity {

    @PrimaryKey
    private int id;
    private int testId;
    private String questionText;
    private String questionType;
    private String imageUrl;

    @TypeConverters(DataConverter.class)
    private List<QuestionTranslation> questionTranslations;

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

    public List<QuestionTranslation> getQuestionTranslations() {
        return questionTranslations;
    }

    public void setQuestionTranslations(List<QuestionTranslation> questionTranslations) {
        this.questionTranslations = questionTranslations;
    }
}
