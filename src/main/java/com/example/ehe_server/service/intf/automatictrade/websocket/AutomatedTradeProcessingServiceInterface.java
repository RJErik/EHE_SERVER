package com.example.ehe_server.service.intf.automatictrade.websocket;

import com.example.ehe_server.entity.AutomatedTradeRule;
import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.entity.PlatformStock;

import java.util.Optional;

public interface AutomatedTradeProcessingServiceInterface {

    Optional<MarketCandle> getLatestMinuteCandle(PlatformStock platformStock);

    boolean checkRuleCondition(AutomatedTradeRule rule, MarketCandle candle);

    TradeExecutionResult executeAutomatedTrade(AutomatedTradeRule rule, MarketCandle triggeringCandle);


    record TradeExecutionResult(
            boolean success,
            com.example.ehe_server.dto.TradeExecutionResponse tradeResponse,
            Integer transactionId
    ) {}
}