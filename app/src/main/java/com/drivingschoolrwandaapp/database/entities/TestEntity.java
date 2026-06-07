package com.drivingschoolrwandaapp.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.drivingschoolrwandaapp.models.entities.TestTranslation;
import com.drivingschoolrwandaapp.utils.DataConverter;

import java.util.List;

@Entity(tableName = "tests")
public class TestEntity {

    @PrimaryKey
    private int id;
    private String title;
    private String description;
    private int testNumber;
    private String imageUrl;
    private int totalMarks;
    private int passMarks;
    private int duration;
    private boolean isFree;
    private int questionCount;
    private long lastRefreshed;

    @TypeConverters(DataConverter.class)
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

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public long getLastRefreshed() {
        return lastRefreshed;
    }

    public void setLastRefreshed(long lastRefreshed) {
        this.lastRefreshed = lastRefreshed;
    }

    public List<TestTranslation> getTestTranslations() {
        return testTranslations;
    }

    public void setTestTranslations(List<TestTranslation> testTranslations) {
        this.testTranslations = testTranslations;
    }
}
