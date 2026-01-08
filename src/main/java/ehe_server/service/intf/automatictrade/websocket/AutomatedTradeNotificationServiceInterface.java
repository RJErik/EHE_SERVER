package ehe_server.service.intf.automatictrade.websocket;

import ehe_server.dto.TradeExecutionResponse;
import ehe_server.entity.AutomatedTradeRule;
import ehe_server.entity.MarketCandle;
import ehe_server.service.automatedtraderule.websocket.AutomatedTradeSubscription;

public interface AutomatedTradeNotificationServiceInterface {

    void sendTradeNotification(
            AutomatedTradeRule rule,
            MarketCandle triggeringCandle,
            boolean success,
            TradeExecutionResponse tradeResult,
            Integer transactionId,
            AutomatedTradeSubscription subscription);
}