package com.example.ehe_server.service.intf.alert.websocket;

import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.service.alert.websocket.AlertSubscription;

public interface AlertNotificationServiceInterface {

    /**
     * Send a notification to the user that their alert has been triggered.
     *
     * @param alert            the alert that was triggered
     * @param triggeringCandle the candle that triggered the alert
     * @param subscription     the subscription to send the notification to
     */
    void sendTriggeredNotification(Alert alert, MarketCandle triggeringCandle, AlertSubscription subscription);
}