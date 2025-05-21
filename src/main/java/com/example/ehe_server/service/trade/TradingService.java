package com.example.ehe_server.service.trade;

import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
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
public class TradingService implements TradingServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;
    private final BinanceAccountService binanceAccountService;

    // Define precision constants for Binance API
    private static final int QUOTE_ORDER_QTY_PRECISION = 8; // Standard precision for most quote assets like USDT
    private static final int QUANTITY_PRECISION = 8; // Standard precision for most base assets

    public TradingService(
            PortfolioRepository portfolioRepository,
            UserRepository userRepository,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService,
            BinanceAccountService binanceAccountService) {
        this.portfolioRepository = portfolioRepository;
        this.userRepository = userRepository;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
        this.binanceAccountService = binanceAccountService;
    }

    @Override
    @Transactional
    public Map<String, Object> executeMarketOrder(Integer portfolioId, String stockSymbol, String action,
                                                  BigDecimal amount, String quantityType, Integer explicitUserId) {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("Executing market order: " + action + " " + amount + " of " + stockSymbol +
                    " for portfolio: " + portfolioId + " using " + quantityType);

            // Get current user ID from audit context
            Integer userId;
            String userIdStr;

            if (explicitUserId != null) {
                userId = explicitUserId;
                userIdStr = explicitUserId.toString();
            } else {
                // Fall back to audit context for regular (non-automated) trades
                userIdStr = auditContextService.getCurrentUser();
                if (userIdStr == null || userIdStr.isEmpty()) {
                    result.put("success", false);
                    result.put("message", "No user context available");
                    return result;
                }
                userId = Integer.parseInt(userIdStr);
            }

            // Check if user exists and is active
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "User not found");
                loggingService.logAction(null, userIdStr, "Market order execution failed: User not found");
                return result;
            }

            User user = userOptional.get();
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                result.put("success", false);
                result.put("message", "Account is not active");
                loggingService.logAction(userId, userIdStr,
                        "Market order execution failed: Account not active, status=" + user.getAccountStatus());
                return result;
            }

            // Check if portfolio exists and belongs to the user
            Optional<Portfolio> portfolioOptional = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId);
            if (portfolioOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "Portfolio not found or doesn't belong to the user");
                loggingService.logAction(userId, userIdStr,
                        "Market order execution failed: Portfolio not found or doesn't belong to user, portfolioId=" + portfolioId);
                return result;
            }

            Portfolio portfolio = portfolioOptional.get();
            ApiKey apiKey = portfolio.getApiKey();

            if (apiKey == null) {
                result.put("success", false);
                result.put("message", "No API key associated with portfolio. Cannot execute trade.");
                loggingService.logAction(userId, userIdStr,
                        "Market order execution failed: No API key associated with portfolio: " + portfolioId);
                return result;
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
                result.put("success", false);
                result.put("message", "Invalid quantity type: " + quantityType);
                loggingService.logAction(userId, userIdStr,
                        "Market order execution failed: Invalid quantity type: " + quantityType);
                return result;
            }

            // Execute the order
            Map<String, Object> orderResult = binanceAccountService.placeMarketOrder(
                    apiKeyValue, secretKeyValue, stockSymbol, action, "MARKET", quantity, quoteOrderQty);

            if (orderResult.containsKey("orderId")) {
                // Order was successful
                result.put("success", true);
                result.put("order", orderResult);

                // Log the successful trade
                String tradeDetails = action + " " + (quantity != null ? quantity : quoteOrderQty) +
                        " of " + stockSymbol + " using " + quantityType;
                loggingService.logAction(userId, userIdStr, "Market order executed: " + tradeDetails);
            } else {
                // Order failed
                result.put("success", false);
                result.put("message", "Failed to execute order: " +
                        (orderResult.containsKey("message") ? orderResult.get("message") : "Unknown error"));

                loggingService.logAction(userId, userIdStr, "Market order failed: " +
                        (orderResult.containsKey("message") ? orderResult.get("message") : "Unknown error"));
            }

        } catch (Exception e) {
            System.out.println("Error executing market order: " + e.getMessage());
            e.printStackTrace();

            // Log error
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error executing market order: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while executing market order: " + e.getMessage());
        }

        return result;
    }
}