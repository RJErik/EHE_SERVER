package com.example.ehe_server.service.intf.automatictrade.websocket;

import com.example.ehe_server.dto.TradeExecutionResponse;
import com.example.ehe_server.entity.AutomatedTradeRule;
import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.service.automatedtraderule.websocket.AutomatedTradeSubscription;

public interface AutomatedTradeNotificationServiceInterface {

    void sendTradeNotification(
            AutomatedTradeRule rule,
            MarketCandle triggeringCandle,
            boolean success,
            TradeExecutionResponse tradeResult,
            Integer transactionId,
            AutomatedTradeSubscription subscription);
}