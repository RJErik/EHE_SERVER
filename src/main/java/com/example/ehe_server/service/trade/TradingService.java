package com.example.ehe_server.service.trade;

import com.example.ehe_server.entity.*;
import com.example.ehe_server.repository.*;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.binance.BinanceAccountService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.trade.TradingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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
    private final PlatformStockRepository platformStockRepository;
    private final TransactionRepository transactionRepository;
    private final MarketCandleRepository marketCandleRepository;

    // Define precision constants for Binance API
    private static final int QUOTE_ORDER_QTY_PRECISION = 8; // Standard precision for most quote assets like USDT
    private static final int QUANTITY_PRECISION = 8; // Standard precision for most base assets

    public TradingService(
            PortfolioRepository portfolioRepository,
            UserRepository userRepository,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService,
            BinanceAccountService binanceAccountService,
            PlatformStockRepository platformStockRepository,
            TransactionRepository transactionRepository,
            MarketCandleRepository marketCandleRepository) {
        this.portfolioRepository = portfolioRepository;
        this.userRepository = userRepository;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
        this.binanceAccountService = binanceAccountService;
        this.platformStockRepository = platformStockRepository;
        this.transactionRepository = transactionRepository;
        this.marketCandleRepository = marketCandleRepository;
    }

    @Override
    @Transactional
    public Map<String, Object> executeMarketOrder(Integer portfolioId, String stockSymbol, String action,
                                                  BigDecimal amount, String quantityType) {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("Executing market order: " + action + " " + amount + " of " + stockSymbol +
                    " for portfolio: " + portfolioId + " using " + quantityType);

            // Get current user ID from audit context
            String userIdStr = auditContextService.getCurrentUser();
            Integer userId = Integer.parseInt(userIdStr);

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

            // Make sure the stock symbol includes USDT if needed
            String fullStockSymbol = stockSymbol;
            if (!stockSymbol.endsWith("USDT")) {
                fullStockSymbol = stockSymbol + "USDT";
            }

            System.out.println("Looking for platform stock: " + fullStockSymbol);

            // Find or create the PlatformStock
            PlatformStock platformStock;
            java.util.List<PlatformStock> platformStocks =
                    platformStockRepository.findByPlatformNameAndStockSymbol("BINANCE", fullStockSymbol);

            if (platformStocks == null || platformStocks.isEmpty()) {
                System.out.println("Creating new platform stock for: " + fullStockSymbol);
                platformStock = new PlatformStock();
                platformStock.setPlatformName("BINANCE");
                platformStock.setStockSymbol(fullStockSymbol);
                platformStock = platformStockRepository.save(platformStock);
            } else {
                platformStock = platformStocks.get(0);
                System.out.println("Found existing platform stock: " + platformStock.getPlatformStockId());
            }

            // Get the latest price for the symbol from market candles
            MarketCandle latestCandle = marketCandleRepository.findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                    platformStock, MarketCandle.Timeframe.M1);

            BigDecimal currentPrice;
            if (latestCandle != null) {
                currentPrice = latestCandle.getClosePrice();
                System.out.println("Latest candle price found: " + currentPrice);
            } else {
                // If no candle is available, we'll need to use a default valid price
                // We'll attempt to get the price from the exchange API execution later
                currentPrice = new BigDecimal("0.00000001");
                System.out.println("No latest candle found, using default minimum value");
            }

            // Set a minimum valid value to satisfy the constraints
            BigDecimal MIN_VALID_VALUE = new BigDecimal("0.00000001");

            // Create a pending transaction
            Transaction transaction = new Transaction();
            transaction.setPortfolio(portfolio);
            transaction.setPlatformStock(platformStock);
            transaction.setTransactionType("BUY".equalsIgnoreCase(action) ?
                    Transaction.TransactionType.Buy : Transaction.TransactionType.Sell);
            transaction.setApiKey(apiKey);
            transaction.setStatus(Transaction.Status.Pending);
            transaction.setTransactionDate(LocalDateTime.now());

            // Set initial values that satisfy the validation constraints
            transaction.setQuantity(MIN_VALID_VALUE);
            transaction.setPrice(currentPrice.compareTo(BigDecimal.ZERO) > 0 ? currentPrice : MIN_VALID_VALUE);

            // Save the pending transaction
            transaction = transactionRepository.save(transaction);

            // Get credentials for Binance API
            String encryptedApiKey = apiKey.getApiKeyValueEncrypt();
            String encryptedSecretKey = apiKey.getSecretKeyEncrypt();

            // In a real application, you would need to decrypt these values
            // For this example, we'll assume they're not encrypted
            String apiKeyValue = encryptedApiKey; // In reality: decryptionService.decrypt(encryptedApiKey);
            String secretKeyValue = encryptedSecretKey; // In reality: decryptionService.decrypt(encryptedSecretKey);

            // Determine quantity and quoteOrderQty based on action and quantityType
            BigDecimal quantity = null;
            BigDecimal quoteOrderQty = null;

            if ("QUANTITY".equalsIgnoreCase(quantityType)) {
                // Scale the quantity to the appropriate precision
                quantity = amount.setScale(QUANTITY_PRECISION, RoundingMode.DOWN);
                // Make sure quantity is at least the minimum valid value
                if (quantity.compareTo(MIN_VALID_VALUE) >= 0) {
                    transaction.setQuantity(quantity);
                }
            } else if ("QUOTE_ORDER_QTY".equalsIgnoreCase(quantityType)) {
                // Scale the quoteOrderQty to the appropriate precision
                quoteOrderQty = amount.setScale(QUOTE_ORDER_QTY_PRECISION, RoundingMode.DOWN);
                // For QUOTE_ORDER_QTY, we don't know the exact quantity until after execution
                if (currentPrice.compareTo(MIN_VALID_VALUE) > 0) {
                    // Estimate the quantity based on current price
                    BigDecimal estimatedQty = quoteOrderQty.divide(currentPrice, 8, RoundingMode.DOWN);
                    if (estimatedQty.compareTo(MIN_VALID_VALUE) >= 0) {
                        transaction.setQuantity(estimatedQty);
                    }
                }
            } else {
                // Update transaction to failed status
                transaction.setStatus(Transaction.Status.Failed);
                transactionRepository.save(transaction);

                result.put("success", false);
                result.put("message", "Invalid quantity type: " + quantityType);
                loggingService.logAction(userId, userIdStr,
                        "Market order execution failed: Invalid quantity type: " + quantityType);
                return result;
            }

            // Save the transaction with initial values
            transactionRepository.save(transaction);

            // Execute the order
            Map<String, Object> orderResult = binanceAccountService.placeMarketOrder(
                    apiKeyValue, secretKeyValue, fullStockSymbol, action, "MARKET", quantity, quoteOrderQty);

            if (orderResult.containsKey("orderId")) {
                // Order was successful
                result.put("success", true);
                result.put("order", orderResult);

                // Update the transaction with execution details
                transaction.setStatus(Transaction.Status.Completed);

                // Extract actual execution price and quantity from the order result
                if (orderResult.containsKey("fills")) {
                    Object fills = orderResult.get("fills");
                    if (fills instanceof java.util.List && !((java.util.List<?>) fills).isEmpty()) {
                        Object firstFill = ((java.util.List<?>) fills).get(0);
                        if (firstFill instanceof Map) {
                            Map<?, ?> fillMap = (Map<?, ?>) firstFill;

                            // Extract executed price
                            if (fillMap.containsKey("price")) {
                                try {
                                    BigDecimal executedPrice = new BigDecimal(fillMap.get("price").toString());
                                    transaction.setPrice(executedPrice);
                                } catch (NumberFormatException e) {
                                    // If price parsing fails, use the latest candle price
                                    if (currentPrice.compareTo(MIN_VALID_VALUE) >= 0) {
                                        transaction.setPrice(currentPrice);
                                    } else {
                                        // Use a safe minimum value
                                        transaction.setPrice(MIN_VALID_VALUE);
                                    }
                                }
                            }

                            // Extract executed quantity if we used QUOTE_ORDER_QTY
                            if ("QUOTE_ORDER_QTY".equalsIgnoreCase(quantityType) && fillMap.containsKey("qty")) {
                                try {
                                    BigDecimal executedQty = new BigDecimal(fillMap.get("qty").toString());
                                    // Make sure we don't set a value that would fail validation
                                    if (executedQty.compareTo(MIN_VALID_VALUE) >= 0) {
                                        transaction.setQuantity(executedQty);
                                    }
                                } catch (NumberFormatException e) {
                                    // Keep the estimated quantity, which is already set to a valid value
                                    System.out.println("Error parsing executed quantity: " + e.getMessage());
                                }
                            }
                        }
                    }
                } else if (orderResult.containsKey("executedQty")) {
                    // If we have executedQty in the response
                    try {
                        BigDecimal executedQty = new BigDecimal(orderResult.get("executedQty").toString());
                        // Make sure the quantity passes validation
                        if (executedQty.compareTo(MIN_VALID_VALUE) >= 0) {
                            transaction.setQuantity(executedQty);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Error parsing executedQty: " + e.getMessage());
                        // Keep the current quantity
                    }
                }

                // If there's a cumulative quote quantity (total spent/received), use that to calculate price
                if (orderResult.containsKey("cummulativeQuoteQty") && transaction.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    try {
                        BigDecimal quoteQty = new BigDecimal(orderResult.get("cummulativeQuoteQty").toString());
                        if (transaction.getQuantity().compareTo(MIN_VALID_VALUE) > 0 && quoteQty.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal calculatedPrice = quoteQty.divide(transaction.getQuantity(), 8, RoundingMode.HALF_UP);
                            // Ensure the calculated price passes validation
                            if (calculatedPrice.compareTo(MIN_VALID_VALUE) >= 0) {
                                transaction.setPrice(calculatedPrice);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error calculating price from quote quantity: " + e.getMessage());
                        // If calculation fails, keep the current price (which is already set to a valid value)
                    }
                }

                // Save the updated transaction
                transactionRepository.save(transaction);

                // Log the successful trade
                String tradeDetails = action + " " + transaction.getQuantity() +
                        " of " + fullStockSymbol + " at price " + transaction.getPrice();
                loggingService.logAction(userId, userIdStr, "Market order executed: " + tradeDetails);
            } else {
                // Order failed
                transaction.setStatus(Transaction.Status.Failed);
                transactionRepository.save(transaction);

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