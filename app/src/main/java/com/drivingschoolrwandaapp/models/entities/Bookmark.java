package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Bookmark implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("pdfId")
    private int pdfId;

    @SerializedName("pdf")
    private PdfFile pdf;

    @SerializedName("pageNumber")
    private int pageNumber;

    @SerializedName("note")
    private String note;

    @SerializedName("createdAt")
    private String createdAt;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPdfId() { return pdfId; }
    public PdfFile getPdf() { return pdf; }
    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getCreatedAt() { return createdAt; }
}