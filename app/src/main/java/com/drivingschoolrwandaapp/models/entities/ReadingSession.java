package com.drivingschoolrwandaapp.models.entities;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class ReadingSession implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("pdfId")
    private int pdfId;

    @SerializedName("currentPage")
    private int currentPage;

    @SerializedName("totalPages")
    private int totalPages;

    @SerializedName("pdf")
    private PdfFile pdf;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    // Getters and Setters
    public int getId() { return id; }
    public int getPdfId() { return pdfId; }
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    public int getTotalPages() { return totalPages; }
    public PdfFile getPdf() { return pdf; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    public int getProgressPercentage() {
        if (totalPages == 0) return 0;
        return (int) ((currentPage / (float) totalPages) * 100);
    }
}
