package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Rating implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("pdfId")
    private int pdfId;

    @SerializedName("rating")
    private double rating;

    @SerializedName("comment")
    private String comment;

    @SerializedName("user")
    private User user;

    @SerializedName("createdAt")
    private String createdAt;

    // Getters and Setters
    public int getId() { return id; }
    public int getPdfId() { return pdfId; }
    public double getRating() { return rating; }
    public String getComment() { return comment; }
    public User getUser() { return user; }
    public String getCreatedAt() { return createdAt; }
}