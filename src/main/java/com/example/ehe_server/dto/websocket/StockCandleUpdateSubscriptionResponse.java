package com.example.ehe_server.dto.websocket;

import java.util.Objects;

public class StockCandleUpdateSubscriptionResponse {
    String subscriptionId;
    String subscriptionType;

    public StockCandleUpdateSubscriptionResponse(String subscriptionId, String subscriptionType) {
        this.subscriptionId = subscriptionId;
        this.subscriptionType = subscriptionType;
    }

    public StockCandleUpdateSubscriptionResponse(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StockCandleUpdateSubscriptionResponse that = (StockCandleUpdateSubscriptionResponse) o;
        return Objects.equals(subscriptionId, that.subscriptionId) && Objects.equals(subscriptionType, that.subscriptionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriptionId, subscriptionType);
    }
}
