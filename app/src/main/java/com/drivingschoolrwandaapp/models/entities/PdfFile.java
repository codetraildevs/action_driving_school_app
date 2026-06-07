package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class PdfFile implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("filePath")
    private String filePath;

    @SerializedName("author")
    private String author;

    @SerializedName("totalPages")
    private int totalPages;

    @SerializedName("description")
    private String description;

    @SerializedName("isPublic")
    private boolean isPublic;

    @SerializedName("uploadedAt")
    private String uploadedAt;

    @SerializedName("language")
    private Language language;

    @SerializedName("averageRating")
    private double averageRating;

    @SerializedName("totalRatings")
    private int totalRatings;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }
    public String getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(String uploadedAt) { this.uploadedAt = uploadedAt; }
    public Language getLanguage() { return language; }
    public void setLanguage(Language language) { this.language = language; }
    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public int getTotalRatings() { return totalRatings; }
    public void setTotalRatings(int totalRatings) { this.totalRatings = totalRatings; }
}