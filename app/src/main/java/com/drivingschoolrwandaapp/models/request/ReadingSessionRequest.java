package com.drivingschoolrwandaapp.models.request;

public class ReadingSessionRequest {
    private int currentPage;
    private int pdfId;

    public ReadingSessionRequest(int currentPage, int pdfId) {
        this.currentPage = currentPage;
        this.pdfId = pdfId;
    }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getPdfId() { return pdfId; }
    public void setPdfId(int pdfId) { this.pdfId = pdfId; }
}