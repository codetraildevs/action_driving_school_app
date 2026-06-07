package com.drivingschoolrwandaapp.models.request;

import com.google.gson.annotations.SerializedName;

public class CreateSessionRequest {
    @SerializedName("pdfId")
    private int pdfId;

    @SerializedName("currentPage")
    private int currentPage;

    public CreateSessionRequest(int pdfId, int currentPage) {
        this.pdfId = pdfId;
        this.currentPage = currentPage;
    }

    public int getPdfId() { return pdfId; }
    public int getCurrentPage() { return currentPage; }
}