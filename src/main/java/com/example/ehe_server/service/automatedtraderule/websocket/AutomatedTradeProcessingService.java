package com.example.ehe_server.service.automatedtraderule.websocket;

import com.example.ehe_server.dto.TradeExecutionResponse;
import com.example.ehe_server.entity.AutomatedTradeRule;
import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.entity.Transaction;
import com.example.ehe_server.repository.MarketCandleRepository;
import com.example.ehe_server.repository.TransactionRepository;
import com.example.ehe_server.service.intf.automatictrade.websocket.AutomatedTradeProcessingServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.trade.TradingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AutomatedTradeProcessingService implements AutomatedTradeProcessingServiceInterface {

    private final MarketCandleRepository marketCandleRepository;
    private final TransactionRepository transactionRepository;
    private final TradingServiceInterface tradingService;
    private final LoggingServiceInterface loggingService;

    public AutomatedTradeProcessingService(
            MarketCandleRepository marketCandleRepository,
            TransactionRepository transactionRepository,
            TradingServiceInterface tradingService,
            LoggingServiceInterface loggingService) {
        this.marketCandleRepository = marketCandleRepository;
        this.transactionRepository = transactionRepository;
        this.tradingService = tradingService;
        this.loggingService = loggingService;
    }

    @Override
    public Optional<MarketCandle> getLatestMinuteCandle(PlatformStock platformStock) {
        return Optional.ofNullable(
                marketCandleRepository.findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                        platformStock, MarketCandle.Timeframe.M1));
    }

    @Override
    public boolean checkRuleCondition(AutomatedTradeRule rule, MarketCandle candle) {
        BigDecimal priceToCheck = candle.getClosePrice();
        BigDecimal thresholdValue = rule.getThresholdValue();

        return switch (rule.getConditionType()) {
            case PRICE_ABOVE -> priceToCheck.compareTo(thresholdValue) > 0;
            case PRICE_BELOW -> priceToCheck.compareTo(thresholdValue) < 0;
        };
    }

    @Override
    public TradeExecutionResult executeAutomatedTrade(AutomatedTradeRule rule, MarketCandle triggeringCandle) {
        int userId = rule.getUser().getUserId();

        try {
            loggingService.logAction("Executing automated trade for rule #" + rule.getAutomatedTradeRuleId());

            TradeExecutionResponse tradeResult = executeTradeOrder(rule, userId);
            boolean success = isTradeSuccessful(tradeResult);
            Integer transactionId = success ? findTransactionId(rule, tradeResult) : null;

            return new TradeExecutionResult(success, tradeResult, transactionId);

        } catch (Exception e) {
            loggingService.logError("Error executing automated trade for rule #" +
                    rule.getAutomatedTradeRuleId() + ": " + e.getMessage(), e);
            return new TradeExecutionResult(false, null, null);
        }
    }

    private TradeExecutionResponse executeTradeOrder(AutomatedTradeRule rule, int userId) {
        return tradingService.executeTrade(
                userId,
                rule.getPortfolio().getPortfolioId(),
                rule.getPlatformStock().getStock().getStockSymbol(),
                Transaction.TransactionType.valueOf(rule.getActionType().toString()),
                rule.getQuantity(),
                rule.getQuantityType());
    }

    private boolean isTradeSuccessful(TradeExecutionResponse tradeResult) {
        return tradeResult != null && "FILLED".equals(tradeResult.getStatus());
    }

    private Integer findTransactionId(AutomatedTradeRule rule, TradeExecutionResponse tradeResult) {
        if (tradeResult.getOrderId() == null) {
            return null;
        }

        Integer portfolioId = rule.getPortfolio().getPortfolioId();
        List<Transaction> transactions = transactionRepository.findByPortfolio_PortfolioId(portfolioId);

        if (transactions.isEmpty()) {
            return null;
        }

        transactions.sort((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()));
        return transactions.getFirst().getTransactionId();
    }
}