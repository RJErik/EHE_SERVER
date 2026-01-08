package ehe_server.dto.websocket;

import java.util.Objects;

public class AlertUnsubscriptionRequest {
    String subscriptionId;

    public AlertUnsubscriptionRequest(String subscriptionId) {
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
        AlertUnsubscriptionRequest that = (AlertUnsubscriptionRequest) o;
        return Objects.equals(subscriptionId, that.subscriptionId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(subscriptionId);
    }
}
