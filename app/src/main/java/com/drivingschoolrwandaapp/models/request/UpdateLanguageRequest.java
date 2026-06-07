package com.drivingschoolrwandaapp.models.request;

import com.google.gson.annotations.SerializedName;

public class UpdateLanguageRequest {
    @SerializedName("languageId")
    private int languageId;

    public UpdateLanguageRequest(int languageId) {
        this.languageId = languageId;
    }

    public int getLanguageId() { return languageId; }
}
