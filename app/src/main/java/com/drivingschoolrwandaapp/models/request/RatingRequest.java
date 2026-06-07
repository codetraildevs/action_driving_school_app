package com.drivingschoolrwandaapp.models.request;

import com.google.gson.annotations.SerializedName;

public class RatingRequest {
    private float rating;
    private String comment;

    public RatingRequest(float rating, String comment) {
        this.rating = rating;
        this.comment = comment;
    }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
