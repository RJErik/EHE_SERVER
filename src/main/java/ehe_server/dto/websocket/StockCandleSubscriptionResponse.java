package ehe_server.dto.websocket;

import java.util.Objects;

public class StockCandleSubscriptionResponse {
    String subscriptionId;

    public StockCandleSubscriptionResponse(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StockCandleSubscriptionResponse that = (StockCandleSubscriptionResponse) o;
        return Objects.equals(subscriptionId, that.subscriptionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriptionId);
    }
}
