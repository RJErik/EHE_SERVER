package com.example.ehe_server.dto.websocket;

import java.time.LocalDateTime;
import java.util.List;

public class CandleUpdateMessage {
    private String subscriptionId;
    private String updateType;
    private List<CandleDataResponse.CandleData> updatedCandles;
    private LocalDateTime updateTimestamp;

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getUpdateType() {
        return updateType;
    }

    public void setUpdateType(String updateType) {
        this.updateType = updateType;
    }

    public List<CandleDataResponse.CandleData> getUpdatedCandles() {
        return updatedCandles;
    }

    public void setUpdatedCandles(List<CandleDataResponse.CandleData> updatedCandles) {
        this.updatedCandles = updatedCandles;
    }

    public LocalDateTime getUpdateTimestamp() {
        return updateTimestamp;
    }

    public void setUpdateTimestamp(LocalDateTime updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }
}
