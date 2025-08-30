package com.example.ehe_server.dto.websocket;

import java.time.LocalDateTime;

public class SubscriptionUpdateSubscriptionRequest {
    private String subscriptionId;
    private LocalDateTime newStartDate;
    private LocalDateTime newEndDate;
    private Boolean resetData; // Whether to clear existing data and refetch
    private String subscriptionType; // Added field for subscription type

    // Getters and setters
    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public LocalDateTime getNewStartDate() {
        return newStartDate;
    }

    public void setNewStartDate(LocalDateTime newStartDate) {
        this.newStartDate = newStartDate;
    }

    public LocalDateTime getNewEndDate() {
        return newEndDate;
    }

    public void setNewEndDate(LocalDateTime newEndDate) {
        this.newEndDate = newEndDate;
    }

    public Boolean getResetData() {
        return resetData;
    }

    public void setResetData(Boolean resetData) {
        this.resetData = resetData;
    }

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }
}
