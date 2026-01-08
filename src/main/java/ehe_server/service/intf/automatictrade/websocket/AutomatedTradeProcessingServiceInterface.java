package ehe_server.service.intf.automatictrade.websocket;

import ehe_server.dto.TradeExecutionResponse;
import ehe_server.entity.AutomatedTradeRule;
import ehe_server.entity.MarketCandle;
import ehe_server.entity.PlatformStock;

import java.util.Optional;

public interface AutomatedTradeProcessingServiceInterface {

    Optional<MarketCandle> getLatestMinuteCandle(PlatformStock platformStock);

    boolean checkRuleCondition(AutomatedTradeRule rule, MarketCandle candle);

    TradeExecutionResult executeAutomatedTrade(AutomatedTradeRule rule, MarketCandle triggeringCandle);


    record TradeExecutionResult(
            boolean success,
            TradeExecutionResponse tradeResponse,
            Integer transactionId
    ) {}
}