package com.example.ehe_server.service.trade;

import com.example.ehe_server.dto.TradeExecutionResponse;
import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.exception.custom.ApiKeyMissingException;
import com.example.ehe_server.exception.custom.InvalidQuantityTypeException;
import com.example.ehe_server.exception.custom.PortfolioNotFoundException;
import com.example.ehe_server.exception.custom.TradeExecutionException;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.binance.BinanceAccountService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.trade.TradingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class TradingService implements TradingServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final LoggingServiceInterface loggingService;
    private final BinanceAccountService binanceAccountService;

    // Define precision constants for Binance API
    private static final int QUOTE_ORDER_QTY_PRECISION = 8; // Standard precision for most quote assets like USDT
    private static final int QUANTITY_PRECISION = 8; // Standard precision for most base assets

    public TradingService(
            PortfolioRepository portfolioRepository,
            LoggingServiceInterface loggingService,
            BinanceAccountService binanceAccountService) {
        this.portfolioRepository = portfolioRepository;
        this.loggingService = loggingService;
        this.binanceAccountService = binanceAccountService;
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
        String encryptedApiKey = apiKey.getApiKeyValueEncrypt();
        String encryptedSecretKey = apiKey.getSecretKeyEncrypt();

        // In a real application, you would need to decrypt these values
        // For this example, we'll assume they're not encrypted
        String apiKeyValue = encryptedApiKey; // In reality: decryptionService.decrypt(encryptedApiKey);
        String secretKeyValue = encryptedSecretKey; // In reality: decryptionService.decrypt(encryptedSecretKey);

        // Make sure the stock symbol includes USDT if needed
        if (!stockSymbol.endsWith("USDT")) {
            stockSymbol = stockSymbol + "USDT";
        }

        // Determine quantity and quoteOrderQty based on action and quantityType
        BigDecimal quantity = null;
        BigDecimal quoteOrderQty = null;

        if ("QUANTITY".equalsIgnoreCase(quantityType)) {
            // Scale the quantity to the appropriate precision
            quantity = amount.setScale(QUANTITY_PRECISION, RoundingMode.DOWN);
        } else if ("QUOTE_ORDER_QTY".equalsIgnoreCase(quantityType)) {
            // Scale the quoteOrderQty to the appropriate precision
            quoteOrderQty = amount.setScale(QUOTE_ORDER_QTY_PRECISION, RoundingMode.DOWN);
        } else {
            throw new InvalidQuantityTypeException(quantityType);
        }

        // Execute the order
        Map<String, Object> orderResult = binanceAccountService.placeMarketOrder(
                apiKeyValue, secretKeyValue, stockSymbol, action, "MARKET", quantity, quoteOrderQty);


        // Log the successful trade
        String tradeDetails = action + " " + (quantity != null ? quantity : quoteOrderQty) +
                " of " + stockSymbol + " using " + quantityType;
        loggingService.logAction("Market order executed: " + tradeDetails);

        return new TradeExecutionResponse(
                (Long) orderResult.get("orderId"),
                (String) orderResult.get("symbol"),
                (String) orderResult.get("side"),
                (BigDecimal) orderResult.get("origQty"),
                (BigDecimal) orderResult.get("executedQty"),
                (BigDecimal) orderResult.get("cummulativeQuoteQty"),
                (String) orderResult.get("status")
        );
    }
}