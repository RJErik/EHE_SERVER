package com.example.ehe_server.service.automatedtraderule.websocket;

public class AutomatedTradeSubscription {

    private final String id;
    private final Integer userId;
    private final String sessionId;
    private final String destination;

    public AutomatedTradeSubscription(String id, Integer userId, String sessionId, String destination) {
        this.id = id;
        this.userId = userId;
        this.sessionId = sessionId;
        this.destination = destination;
    }

    public String getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getDestination() {
        return destination;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutomatedTradeSubscription that = (AutomatedTradeSubscription) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "AutomatedTradeSubscription{" +
                "id='" + id + '\'' +
                ", userId=" + userId +
                ", sessionId='" + sessionId + '\'' +
                ", destination='" + destination + '\'' +
                '}';
    }
}