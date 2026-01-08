package ehe_server.dto.websocket;

import java.util.Objects;

public class AlertUnsubscriptionResponse {
    String subscriptionId;

    public AlertUnsubscriptionResponse(String subscriptionId) {
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
        AlertUnsubscriptionResponse that = (AlertUnsubscriptionResponse) o;
        return Objects.equals(subscriptionId, that.subscriptionId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(subscriptionId);
    }
}
