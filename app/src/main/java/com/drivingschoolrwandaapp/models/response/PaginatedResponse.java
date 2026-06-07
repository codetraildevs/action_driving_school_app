package com.drivingschoolrwandaapp.models.response;

import java.util.List;

public class PaginatedResponse<T> {
    private List<T> items;
    private int currentPage;
    private int totalPages;
    private int totalItems;
    private boolean hasNext;

    // Getters and setters
    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }

    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
}