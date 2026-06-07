package com.drivingschoolrwandaapp.models.request;

public class BookmarkRequest {
    private int pageNumber;
    private String note;

    public BookmarkRequest(int pageNumber, String note) {
        this.pageNumber = pageNumber;
        this.note = note;
    }

    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}