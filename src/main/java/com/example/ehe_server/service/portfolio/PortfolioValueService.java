package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.entity.*;
import com.example.ehe_server.repository.*;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.binance.BinanceAccountService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PortfolioValueService implements PortfolioValueServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final PlatformStockRepository platformStockRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;
    private final BinanceAccountService binanceAccountService;

    public PortfolioValueService(
            PortfolioRepository portfolioRepository,
            UserRepository userRepository,
            HoldingRepository holdingRepository,
            PlatformStockRepository platformStockRepository,
            MarketCandleRepository marketCandleRepository,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService,
            BinanceAccountService binanceAccountService) {
        this.portfolioRepository = portfolioRepository;
        this.userRepository = userRepository;
        this.holdingRepository = holdingRepository;
        this.platformStockRepository = platformStockRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
        this.binanceAccountService = binanceAccountService;
    }

    @Override
    public Map<String, Object> calculatePortfolioValue(Integer portfolioId) {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("Starting portfolio value calculation for portfolioId: " + portfolioId);

            // Get current user ID from audit context
            String userIdStr = auditContextService.getCurrentUser();
            Integer userId = Integer.parseInt(userIdStr);
            System.out.println("User ID from audit context: " + userId);

            // Check if user exists and is active
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                System.out.println("User not found for ID: " + userId);
                result.put("success", false);
                result.put("message", "User not found");
                loggingService.logAction(null, userIdStr, "Portfolio value calculation failed: User not found");
                return result;
            }

            User user = userOptional.get();
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                System.out.println("User account not active. Status: " + user.getAccountStatus());
                result.put("success", false);
                result.put("message", "Account is not active");
                loggingService.logAction(userId, userIdStr,
                        "Portfolio value calculation failed: Account not active, status=" + user.getAccountStatus());
                return result;
            }

            // Check if portfolio exists and belongs to the user
            Optional<Portfolio> portfolioOptional = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId);
            if (portfolioOptional.isEmpty()) {
                System.out.println("Portfolio not found or doesn't belong to user. PortfolioId: " + portfolioId + ", UserId: " + userId);
                result.put("success", false);
                result.put("message", "Portfolio not found or doesn't belong to the user");
                loggingService.logAction(userId, userIdStr,
                        "Portfolio value calculation failed: Portfolio not found or doesn't belong to user, portfolioId=" + portfolioId);
                return result;
            }

            Portfolio portfolio = portfolioOptional.get();
            System.out.println("Found portfolio: " + portfolio.getPortfolioName() + " (ID: " + portfolio.getPortfolioId() + ")");

            // First try to update holdings from exchange API
            System.out.println("Attempting to update holdings from exchange API");
            Map<String, Object> updateResult = updateHoldings(portfolioId);
            boolean updateSuccess = updateResult != null && (boolean) updateResult.getOrDefault("success", false);

            if (updateSuccess) {
                System.out.println("Holdings successfully updated from exchange API");
            } else {
                System.out.println("Could not update holdings from exchange API, will use existing holdings");
            }

            // Get holdings for this portfolio (either updated or existing)
            List<Holding> holdings = holdingRepository.findByPortfolio(portfolio);
            System.out.println("Found " + holdings.size() + " holdings for portfolio");

            // Get the portfolio again to ensure we have the latest reservedCash value
            portfolio = portfolioRepository.findById(portfolioId).orElse(portfolio);

            // Calculate total value in USDT
            BigDecimal totalValue = BigDecimal.ZERO;

            // Add reserved cash to total value if it's not null
            if (portfolio.getReservedCash() != null) {
                totalValue = totalValue.add(portfolio.getReservedCash());
                System.out.println("Added reserved cash to total value: " + portfolio.getReservedCash());
            }

            List<Map<String, Object>> holdingsList = new ArrayList<>();

            for (Holding holding : holdings) {
                PlatformStock stock = holding.getPlatformStock();
                String symbol = stock.getStockSymbol();
                BigDecimal quantity = holding.getQuantity();
                BigDecimal valueInUsdt = BigDecimal.ZERO;

                System.out.println("Processing holding: Symbol=" + symbol + ", Quantity=" + quantity);

                // Look for the USDT trading pair for all assets
                String usdtPairSymbol = symbol.endsWith("USDT") ? symbol : symbol + "USDT";
                System.out.println("Looking for USDT pair: " + usdtPairSymbol + " on platform: " + stock.getPlatformName());

                List<PlatformStock> usdtPairs = platformStockRepository.findByPlatformNameAndStockSymbol(
                        stock.getPlatformName(), usdtPairSymbol);

                System.out.println("Found " + usdtPairs.size() + " matching USDT pairs");

                if (!usdtPairs.isEmpty()) {
                    PlatformStock usdtPair = usdtPairs.get(0);
                    System.out.println("Using USDT pair: " + usdtPair.getStockSymbol() + " (ID: " + usdtPair.getPlatformStockId() + ")");

                    // Get the latest M1 candle for this pair
                    MarketCandle latestCandle = marketCandleRepository.findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                            usdtPair, MarketCandle.Timeframe.M1);

                    if (latestCandle != null) {
                        // Use close price to calculate the value
                        BigDecimal price = latestCandle.getClosePrice();
                        System.out.println("Found latest candle with close price: " + price + " at time: " + latestCandle.getTimestamp());
                        valueInUsdt = quantity.multiply(price);
                        System.out.println("Calculated value: " + quantity + " * " + price + " = " + valueInUsdt);
                    } else {
                        System.out.println("No candle found for pair: " + usdtPairSymbol);
                    }
                } else {
                    System.out.println("No USDT trading pair found for: " + symbol);
                }

                // Add to total
                totalValue = totalValue.add(valueInUsdt);
                System.out.println("Running total after adding " + symbol + ": " + totalValue);

                // Add holding details to the list
                Map<String, Object> holdingMap = new HashMap<>();
                holdingMap.put("id", holding.getHoldingId());
                holdingMap.put("symbol", symbol);
                holdingMap.put("quantity", quantity);
                holdingMap.put("valueInUsdt", valueInUsdt.setScale(2, RoundingMode.HALF_UP));
                holdingsList.add(holdingMap);
            }

            // Round to 2 decimal places
            totalValue = totalValue.setScale(2, RoundingMode.HALF_UP);
            System.out.println("Final portfolio value: " + totalValue);

            // Prepare success response
            result.put("success", true);
            result.put("portfolioId", portfolioId);
            result.put("portfolioName", portfolio.getPortfolioName());
            result.put("totalValue", totalValue);
            result.put("holdings", holdingsList);
            result.put("reservedCash", portfolio.getReservedCash() != null ?
                    portfolio.getReservedCash().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2));

            // Log success
            loggingService.logAction(userId, userIdStr,
                    "Calculated portfolio value for " + portfolio.getPortfolioName() + ": " + totalValue + " USDT");

        } catch (Exception e) {
            System.out.println("Error calculating portfolio value: " + e.getMessage());
            e.printStackTrace();

            // Log error
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error calculating portfolio value: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while calculating portfolio value: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> updateHoldings(Integer portfolioId) {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("Starting updateHoldings method for portfolioId: " + portfolioId);

            // Get current user ID from audit context
            String userIdStr = auditContextService.getCurrentUser();
            Integer userId = Integer.parseInt(userIdStr);
            System.out.println("User ID from audit context: " + userId);

            // Check if user exists and is active
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                System.out.println("User not found for ID: " + userId);
                result.put("success", false);
                result.put("message", "User not found");
                loggingService.logAction(null, userIdStr, "Holdings update failed: User not found");
                return result;
            }

            User user = userOptional.get();
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                System.out.println("User account not active. Status: " + user.getAccountStatus());
                result.put("success", false);
                result.put("message", "Account is not active");
                loggingService.logAction(userId, userIdStr,
                        "Holdings update failed: Account not active, status=" + user.getAccountStatus());
                return result;
            }

            // Check if portfolio exists and belongs to the user
            Optional<Portfolio> portfolioOptional = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId);
            if (portfolioOptional.isEmpty()) {
                System.out.println("Portfolio not found or doesn't belong to user. PortfolioId: " + portfolioId + ", UserId: " + userId);
                result.put("success", false);
                result.put("message", "Portfolio not found or doesn't belong to the user");
                loggingService.logAction(userId, userIdStr,
                        "Holdings update failed: Portfolio not found or doesn't belong to user, portfolioId=" + portfolioId);
                return result;
            }

            Portfolio portfolio = portfolioOptional.get();
            System.out.println("Found portfolio: " + portfolio.getPortfolioName() + " (ID: " + portfolio.getPortfolioId() + ")");

            ApiKey apiKey = portfolio.getApiKey();
            if (apiKey == null) {
                System.out.println("No API key associated with portfolio. Cannot update holdings.");
                result.put("success", false);
                result.put("message", "No API key associated with portfolio. Cannot update holdings.");
                loggingService.logAction(userId, userIdStr,
                        "Holdings update failed: No API key associated with portfolio: " + portfolioId);
                return result;
            }

            System.out.println("Using API key ID: " + apiKey.getApiKeyId() + " for platform: " + apiKey.getPlatformName());

            // Get credentials for Binance API
            String encryptedApiKey = apiKey.getApiKeyValueEncrypt();
            String encryptedSecretKey = apiKey.getSecretKeyEncrypt();

            // In a real application, you would need to decrypt these values
            // For this example, we'll assume they're not encrypted
            String apiKeyValue = encryptedApiKey; // In reality: decryptionService.decrypt(encryptedApiKey);
            String secretKeyValue = encryptedSecretKey; // In reality: decryptionService.decrypt(encryptedSecretKey);

            // Get account information from Binance
            System.out.println("Calling Binance API to get account info");
            Map<String, Object> accountInfo = binanceAccountService.getAccountInfo(apiKeyValue, secretKeyValue);
            System.out.println("Received response from Binance API: " + accountInfo);

            // Check if we have a valid response with balances
            boolean apiCallSuccessful = accountInfo != null && accountInfo.containsKey("balances");

            if (apiCallSuccessful) {
                List<Map<String, Object>> balances = (List<Map<String, Object>>) accountInfo.get("balances");
                System.out.println("Found " + (balances != null ? balances.size() : 0) + " balances in Binance response");

                if (balances != null && !balances.isEmpty()) {
                    String platformName = apiKey.getPlatformName();

                    // Initialize reserved cash to zero, will be updated if USDT is found
                    BigDecimal reservedCash = BigDecimal.ZERO;
                    boolean usdtFound = false;

                    // First, delete all existing holdings for this portfolio
                    List<Holding> existingHoldings = holdingRepository.findByPortfolio(portfolio);
                    System.out.println("Deleting " + existingHoldings.size() + " existing holdings");
                    holdingRepository.deleteAll(existingHoldings);

                    // Track successful additions
                    int addedCount = 0;

                    // Process each balance from Binance
                    for (Map<String, Object> balance : balances) {
                        String asset = (String) balance.get("asset");
                        String free = (String) balance.get("free");
                        System.out.println("Processing balance: " + asset + " = " + free);

                        // Convert String to BigDecimal
                        BigDecimal quantity = new BigDecimal(free);

                        // Only consider assets with non-zero balance
                        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                            System.out.println("Asset " + asset + " has non-zero balance: " + quantity);

                            // Special handling for USDT - update reservedCash instead of creating a holding
                            if ("USDT".equals(asset)) {
                                System.out.println("USDT asset found with balance: " + quantity + ". Setting as reserved cash.");
                                reservedCash = quantity;
                                usdtFound = true;
                                continue; // Skip creating a holding for USDT
                            }

                            // First try exact match for the asset
                            List<PlatformStock> stocks = platformStockRepository.findByPlatformNameAndStockSymbol(
                                    platformName, asset);

                            if (!stocks.isEmpty()) {
                                PlatformStock stock = stocks.get(0);
                                System.out.println("Found direct match for " + asset + ": " + stock.getStockSymbol());

                                // Create a new holding
                                Holding holding = new Holding();
                                holding.setPortfolio(portfolio);
                                holding.setPlatformStock(stock);
                                holding.setQuantity(quantity);

                                // Save the holding
                                Holding savedHolding = holdingRepository.save(holding);
                                System.out.println("Saved new holding with ID: " + savedHolding.getHoldingId());
                                addedCount++;
                            } else {
                                // If no direct match, try looking for trading pairs
                                List<PlatformStock> tradingPairs = platformStockRepository.findByPlatformNameAndStockSymbol(
                                        platformName, asset + "USDT");

                                if (!tradingPairs.isEmpty()) {
                                    PlatformStock stock = tradingPairs.get(0);
                                    System.out.println("Found trading pair for " + asset + ": " + stock.getStockSymbol());

                                    // Create a new holding
                                    Holding holding = new Holding();
                                    holding.setPortfolio(portfolio);
                                    holding.setPlatformStock(stock);
                                    holding.setQuantity(quantity);

                                    // Save the holding
                                    Holding savedHolding = holdingRepository.save(holding);
                                    System.out.println("Saved new holding with ID: " + savedHolding.getHoldingId());
                                    addedCount++;
                                } else {
                                    System.out.println("No matching platform stock found for asset: " + asset);
                                }
                            }
                        } else {
                            System.out.println("Skipping asset " + asset + " with zero balance");
                        }
                    }

                    // Update portfolio with the reserved cash
                    portfolio.setReservedCash(reservedCash);
                    portfolioRepository.save(portfolio);

                    // Log whether USDT was found
                    if (usdtFound) {
                        System.out.println("Updated portfolio with reserved cash: " + reservedCash);
                    } else {
                        System.out.println("No USDT found in balances. Reserved cash set to zero.");
                    }

                    // Prepare success response
                    result.put("success", true);
                    result.put("message", "Holdings updated successfully");
                    result.put("updatedCount", addedCount);
                    result.put("reservedCash", reservedCash);
                    System.out.println("Successfully updated " + addedCount + " holdings");

                    // Log success
                    loggingService.logAction(userId, userIdStr,
                            "Updated holdings for portfolio " + portfolio.getPortfolioName() + ", added " + addedCount +
                                    " holdings, reserved cash: " + reservedCash);
                } else {
                    System.out.println("No balances found in Binance response");
                    result.put("success", false);
                    result.put("message", "Failed to retrieve balances from Binance");
                    loggingService.logAction(userId, userIdStr,
                            "Holdings update failed: No balances in Binance response");
                }
            } else {
                // API call was not successful, don't modify existing holdings
                System.out.println("Error in Binance API response. Keeping existing holdings.");
                result.put("success", false);
                result.put("message", "Failed to connect to Binance API. Using existing holdings.");
                loggingService.logAction(userId, userIdStr,
                        "Holdings update failed: Binance API error. Using existing holdings.");
            }

        } catch (Exception e) {
            // Log error
            System.out.println("Error updating holdings: " + e.getMessage());
            e.printStackTrace();

            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error updating holdings: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while updating holdings: " + e.getMessage());
        }

        return result;
    }
}
