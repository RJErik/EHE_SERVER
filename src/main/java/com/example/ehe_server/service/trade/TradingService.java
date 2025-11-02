package com.example.ehe_server.service.trade;

import com.example.ehe_server.dto.TradeExecutionResponse;
import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.entity.Transaction;
import com.example.ehe_server.exception.custom.ApiKeyMissingException;
import com.example.ehe_server.exception.custom.InvalidQuantityTypeException;
import com.example.ehe_server.exception.custom.PortfolioNotFoundException;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.repository.TransactionRepository;
import com.example.ehe_server.service.binance.BinanceAccountService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.trade.TradingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class TradingService implements TradingServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final LoggingServiceInterface loggingService;
    private final BinanceAccountService binanceAccountService;
    private final TransactionRepository transactionRepository;
    private final PlatformStockRepository platformStockRepository;

    // Define precision constants for Binance API
    private static final int QUOTE_ORDER_QTY_PRECISION = 8;
    private static final int QUANTITY_PRECISION = 8;

    public TradingService(
            PortfolioRepository portfolioRepository,
            LoggingServiceInterface loggingService,
            BinanceAccountService binanceAccountService,
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
    public TradeExecutionResponse executeTrade(Integer userId, Integer portfolioId, String stockSymbol, String action,
                                               BigDecimal amount, String quantityType) {
        System.out.println("Executing market order: " + action + " " + amount + " of " + stockSymbol +
                " for portfolio: " + portfolioId + " using " + quantityType);

        // Check if portfolio exists and belongs to the user
        Optional<Portfolio> portfolioOptional = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId);
        if (portfolioOptional.isEmpty()) {
            throw new PortfolioNotFoundException(portfolioId, userId);
        }

        Portfolio portfolio = portfolioOptional.get();
        ApiKey apiKey = portfolio.getApiKey();

        if (apiKey == null) {
            throw new ApiKeyMissingException(portfolioId);
        }

        // Get credentials for Binance API
        String encryptedApiKey = apiKey.getApiKeyValue();
        String encryptedSecretKey = apiKey.getSecretKey();

        // In a real application, you would need to decrypt these values
        String apiKeyValue = encryptedApiKey;
        String secretKeyValue = encryptedSecretKey;

        // Get PlatformStock entity for transaction record (using original stockSymbol from parameter)
        PlatformStock platformStock = platformStockRepository
                .findByStockSymbol(stockSymbol).getFirst();

        // Make sure the stock symbol includes USDT if needed for Binance API
        String binanceSymbol = stockSymbol;
        if (!binanceSymbol.endsWith("USDT")) {
            binanceSymbol = binanceSymbol + "USDT";
        }

        // Determine quantity and quoteOrderQty based on action and quantityType
        BigDecimal quantity = null;
        BigDecimal quoteOrderQty = null;

        if ("QUANTITY".equalsIgnoreCase(quantityType)) {
            quantity = amount.setScale(QUANTITY_PRECISION, RoundingMode.DOWN);
        } else if ("QUOTE_ORDER_QTY".equalsIgnoreCase(quantityType)) {
            quoteOrderQty = amount.setScale(QUOTE_ORDER_QTY_PRECISION, RoundingMode.DOWN);
        } else {
            throw new InvalidQuantityTypeException(quantityType);
        }

        Transaction transaction = null;

        try {
            // Execute the order
            Map<String, Object> orderResult = binanceAccountService.placeMarketOrder(
                    apiKeyValue, secretKeyValue, binanceSymbol, action, "MARKET", quantity, quoteOrderQty);

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

            // Create and save transaction record
            transaction = new Transaction();
            transaction.setPortfolio(portfolio);
            transaction.setPlatformStock(platformStock);
            transaction.setTransactionType("BUY".equalsIgnoreCase(action) ?
                    Transaction.TransactionType.Buy : Transaction.TransactionType.Sell);
            transaction.setQuantity(executedQty);
            transaction.setPrice(averagePrice);
            transaction.setApiKey(apiKey);
            transaction.setTransactionDate(LocalDateTime.now());

            // Set status based on order result
            if ("FILLED".equals(orderStatus)) {
                transaction.setStatus(Transaction.Status.Completed);
            } else if ("REJECTED".equals(orderStatus) || "EXPIRED".equals(orderStatus)) {
                transaction.setStatus(Transaction.Status.Failed);
            } else {
                transaction.setStatus(Transaction.Status.Pending);
            }

            transaction = transactionRepository.save(transaction);

            // Log the successful trade
            String tradeDetails = action + " " + executedQty + " of " + stockSymbol +
                    " at " + averagePrice + " (total: " + cumulativeQuoteQty + ")";
            loggingService.logAction("Market order executed: " + tradeDetails +
                    " - Transaction ID: " + transaction.getTransactionId());

            return new TradeExecutionResponse(
                    orderResult.get("orderId") != null ?
                            ((Number) orderResult.get("orderId")).longValue() : null,
                    (String) orderResult.get("symbol"),
                    (String) orderResult.get("side"),
                    orderResult.get("origQty") != null ?
                            new BigDecimal(orderResult.get("origQty").toString()) : null,
                    executedQty,
                    cumulativeQuoteQty,
                    orderStatus
            );

        } catch (Exception e) {
            // Create a failed transaction record
            transaction = new Transaction();
            transaction.setPortfolio(portfolio);
            transaction.setPlatformStock(platformStock);
            transaction.setTransactionType("BUY".equalsIgnoreCase(action) ?
                    Transaction.TransactionType.Buy : Transaction.TransactionType.Sell);
            transaction.setQuantity(quantity != null ? quantity : BigDecimal.ZERO);
            transaction.setPrice(BigDecimal.ZERO);
            transaction.setApiKey(apiKey);
            transaction.setTransactionDate(LocalDateTime.now());
            transaction.setStatus(Transaction.Status.Failed);

            transactionRepository.save(transaction);

            throw e;
        }
    }
}