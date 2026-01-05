package com.example.ehe_server.service.trade;

import com.example.ehe_server.dto.TradeExecutionResponse;
import com.example.ehe_server.entity.*;
import com.example.ehe_server.exception.custom.ApiKeyMissingException;
import com.example.ehe_server.exception.custom.PlatformStockNotFoundException;
import com.example.ehe_server.exception.custom.PortfolioNotFoundException;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.repository.TransactionRepository;
import com.example.ehe_server.service.intf.binance.BinanceAccountServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.trade.TradingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class TradingService implements TradingServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final LoggingServiceInterface loggingService;
    private final BinanceAccountServiceInterface binanceAccountService;
    private final TransactionRepository transactionRepository;
    private final PlatformStockRepository platformStockRepository;

    private static final int QUOTE_ORDER_QTY_PRECISION = 8;
    private static final int QUANTITY_PRECISION = 8;

    public TradingService(
            PortfolioRepository portfolioRepository,
            LoggingServiceInterface loggingService,
            BinanceAccountServiceInterface binanceAccountService,
            TransactionRepository transactionRepository,
            PlatformStockRepository platformStockRepository) {
        this.portfolioRepository = portfolioRepository;
        this.loggingService = loggingService;
        this.binanceAccountService = binanceAccountService;
        this.transactionRepository = transactionRepository;
        this.platformStockRepository = platformStockRepository;
    }

    @Override
    @Transactional
    public TradeExecutionResponse executeTrade(Integer userId, Integer portfolioId, String stockSymbol,
                                               Transaction.TransactionType action,
                                               BigDecimal amount, AutomatedTradeRule.QuantityType quantityType) {
        // Check if portfolio exists and belongs to the user
        Optional<Portfolio> portfolioOptional = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId);
        if (portfolioOptional.isEmpty()) {
            throw new PortfolioNotFoundException(portfolioId);
        }

        Portfolio portfolio = portfolioOptional.get();
        ApiKey apiKey = portfolio.getApiKey();

        if (apiKey == null) {
            throw new ApiKeyMissingException(portfolioId);
        }

        // Get credentials for Binance API
        String platformName = apiKey.getPlatform().getPlatformName();

        // Get PlatformStock entity for transaction record
        PlatformStock platformStock = platformStockRepository
                .findByStockNameAndPlatformName(stockSymbol, platformName)
                .orElseThrow(() -> new PlatformStockNotFoundException(stockSymbol, platformName));


        // Make sure the stock symbol includes USDT if needed for Binance API
        String binanceSymbol = stockSymbol;
        if (!binanceSymbol.endsWith("USDT")) {
            binanceSymbol = binanceSymbol + "USDT";
        }

        // Determine quantity and quoteOrderQty based on action and quantityType
        BigDecimal quantity = null;
        BigDecimal quoteOrderQty = null;

        if (quantityType.equals(AutomatedTradeRule.QuantityType.QUANTITY)) {
            quantity = amount.setScale(QUANTITY_PRECISION, RoundingMode.DOWN);
        } else if (quantityType.equals(AutomatedTradeRule.QuantityType.QUOTE_ORDER_QTY)) {
            quoteOrderQty = amount.setScale(QUOTE_ORDER_QTY_PRECISION, RoundingMode.DOWN);
        }

        Transaction transaction;

        try {
            // Execute the order
            Map<String, Object> orderResult = binanceAccountService.placeMarketOrder(
                    apiKey.getApiKeyValue(), apiKey.getSecretKey(), binanceSymbol, action.toString(), "MARKET", quantity, quoteOrderQty);

            // Extract trade details from order result
            BigDecimal executedQty = orderResult.get("executedQty") != null ?
                    new BigDecimal(orderResult.get("executedQty").toString()) : BigDecimal.ZERO;

            BigDecimal cumulativeQuoteQty = orderResult.get("cummulativeQuoteQty") != null ?
                    new BigDecimal(orderResult.get("cummulativeQuoteQty").toString()) : BigDecimal.ZERO;

            // Calculate average price
            BigDecimal averagePrice = BigDecimal.ZERO;
            if (executedQty.compareTo(BigDecimal.ZERO) > 0) {
                averagePrice = cumulativeQuoteQty.divide(executedQty, 8, RoundingMode.HALF_UP);
            }

            String orderStatus = (String) orderResult.get("status");

            // Create and save transaction
            transaction = new Transaction();
            transaction.setPortfolio(portfolio);
            transaction.setPlatformStock(platformStock);
            transaction.setTransactionType(action);
            transaction.setQuantity(executedQty);
            transaction.setPrice(averagePrice);

            // Set status based on order result
            if ("FILLED".equals(orderStatus)) {
                transaction.setStatus(Transaction.Status.COMPLETED);
            } else if ("REJECTED".equals(orderStatus) || "EXPIRED".equals(orderStatus)) {
                transaction.setStatus(Transaction.Status.FAILED);
            } else {
                transaction.setStatus(Transaction.Status.PENDING);
            }

            transaction = transactionRepository.save(transaction);

            String tradeDetails = action + " " + executedQty + " of " + stockSymbol +
                    " at " + averagePrice + " (total: " + cumulativeQuoteQty + ")";
            loggingService.logAction("Market order executed: " + tradeDetails +
                    " - Transaction ID: " + transaction.getTransactionId());

            return new TradeExecutionResponse(
                    orderResult.get("orderId") != null ?
                            ((Number) orderResult.get("orderId")).intValue() : null,
                    (String) orderResult.get("symbol"),
                    (String) orderResult.get("side"),
                    orderResult.get("origQty") != null ?
                            new BigDecimal(orderResult.get("origQty").toString()) : null,
                    executedQty,
                    cumulativeQuoteQty,
                    orderStatus
            );

        } catch (Exception e) {
            transaction = new Transaction();
            transaction.setPortfolio(portfolio);
            transaction.setPlatformStock(platformStock);
            transaction.setTransactionType(action);
            transaction.setQuantity(quantity != null ? quantity : BigDecimal.ZERO);
            transaction.setPrice(BigDecimal.ZERO);
            transaction.setStatus(Transaction.Status.FAILED);

            transactionRepository.save(transaction);

            throw e;
        }
    }
}