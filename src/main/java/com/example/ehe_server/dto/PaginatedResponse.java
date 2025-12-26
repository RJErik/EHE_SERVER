package com.example.ehe_server.dto;

import java.util.List;

public class PaginatedResponse<T> {
    private int currentPage;
    private int totalPages;
    private List<T> items;

    public PaginatedResponse() {
    }

    public PaginatedResponse(int currentPage, int totalPages, List<T> items) {
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.items = items;
    }

    // Getters and setters
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
}