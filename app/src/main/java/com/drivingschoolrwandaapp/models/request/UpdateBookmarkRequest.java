package com.drivingschoolrwandaapp.models.request;

import com.google.gson.annotations.SerializedName;

public class UpdateBookmarkRequest {
    @SerializedName("note")
    private String note;

    public UpdateBookmarkRequest(String note) {
        this.note = note;
    }

    public String getNote() { return note; }
}

