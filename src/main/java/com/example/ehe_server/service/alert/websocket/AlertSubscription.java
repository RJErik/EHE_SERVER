package com.example.ehe_server.service.alert.websocket;

import java.time.LocalDateTime;

public class AlertSubscription {

    private final String id;
    private final Integer userId;
    private final String sessionId;
    private final String destination;
    private volatile boolean initialCheckCompleted;
    private volatile LocalDateTime lastCheckedMinuteCandle;

    public AlertSubscription(String id, Integer userId, String sessionId, String destination) {
        this.id = id;
        this.userId = userId;
        this.sessionId = sessionId;
        this.destination = destination;
        this.initialCheckCompleted = false;
        this.lastCheckedMinuteCandle = null;
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

    public boolean isInitialCheckCompleted() {
        return initialCheckCompleted;
    }

    public LocalDateTime getLastCheckedMinuteCandle() {
        return lastCheckedMinuteCandle;
    }

    public void markInitialCheckCompleted() {
        this.initialCheckCompleted = true;
    }

    public void updateLastCheckedMinuteCandle(LocalDateTime timestamp) {
        this.lastCheckedMinuteCandle = timestamp;
    }
}