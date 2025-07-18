package com.example.ehe_server.service.trade;

import com.example.ehe_server.entity.*;
import com.example.ehe_server.repository.*;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.binance.BinanceAccountService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.trade.TradingCapacityServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Transactional
public class TradingCapacityService implements TradingCapacityServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final PlatformStockRepository platformStockRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final LoggingServiceInterface loggingService;
    private final BinanceAccountService binanceAccountService;
    private final UserContextService userContextService;

    public TradingCapacityService(
            PortfolioRepository portfolioRepository,
            HoldingRepository holdingRepository,
            PlatformStockRepository platformStockRepository,
            MarketCandleRepository marketCandleRepository,
            LoggingServiceInterface loggingService,
            BinanceAccountService binanceAccountService, UserContextService userContextService) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.platformStockRepository = platformStockRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.loggingService = loggingService;
        this.binanceAccountService = binanceAccountService;
        this.userContextService = userContextService;
    }

    @Override
    @Transactional
    public Map<String, Object> getTradingCapacity(Integer portfolioId, String stockSymbol) {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("Calculating trading capacity for portfolioId: " + portfolioId + ", stock: " + stockSymbol);

            // Get current user ID from user context
            Integer userId = Integer.parseInt(userContextService.getCurrentUserIdAsString());

            // Check if portfolio exists and belongs to the user
            Optional<Portfolio> portfolioOptional = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId);
            if (portfolioOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "Portfolio not found or doesn't belong to the user");
                loggingService.logAction("Trading capacity calculation failed: Portfolio not found or doesn't belong to user, portfolioId=" + portfolioId);
                return result;
            }

            Portfolio portfolio = portfolioOptional.get();
            ApiKey apiKey = portfolio.getApiKey();

            if (apiKey == null) {
                result.put("success", false);
                result.put("message", "No API key associated with portfolio. Cannot determine trading capacity.");
                loggingService.logAction("Trading capacity calculation failed: No API key associated with portfolio: " + portfolioId);
                return result;
            }

            // Update holdings from exchange API
            try {
                System.out.println("Updating holdings from exchange API");
                updateHoldingsFromExchange(portfolio, apiKey);
                System.out.println("Holdings updated successfully");
            } catch (Exception e) {
                // Log the error but continue with existing data
                System.out.println("Warning: Failed to update holdings from exchange: " + e.getMessage());
                loggingService.logError("Warning: Failed to update holdings from exchange: " + e.getMessage(), e);
                // We'll continue with the data we already have
            }

            // Get platform name
            String platformName = apiKey.getPlatformName();
            System.out.println("Platform name: " + platformName);

            // Find platform stock (try with different symbol formats)
            List<PlatformStock> stocks = platformStockRepository.findByPlatformNameAndStockSymbol(
                    platformName, stockSymbol);

            if (stocks.isEmpty()) {
                // Try with USDT suffix if it's not already there
                if (!stockSymbol.endsWith("USDT")) {
                    stocks = platformStockRepository.findByPlatformNameAndStockSymbol(
                            platformName, stockSymbol + "USDT");
                    if (!stocks.isEmpty()) {
                        stockSymbol = stockSymbol + "USDT";
                        System.out.println("Found stock with USDT suffix: " + stockSymbol);
                    }
                }

                if (stocks.isEmpty()) {
                    // Try without USDT suffix if it's there
                    if (stockSymbol.endsWith("USDT")) {
                        String baseSymbol = stockSymbol.substring(0, stockSymbol.length() - 4);
                        stocks = platformStockRepository.findByPlatformNameAndStockSymbol(
                                platformName, baseSymbol);
                        if (!stocks.isEmpty()) {
                            stockSymbol = baseSymbol;
                            System.out.println("Found stock without USDT suffix: " + stockSymbol);
                        }
                    }
                }

                if (stocks.isEmpty()) {
                    result.put("success", false);
                    result.put("message", "Stock not found for the given platform: " + stockSymbol);
                    loggingService.logAction("Trading capacity calculation failed: Stock not found: " + stockSymbol + " on platform: " + platformName);
                    return result;
                }
            }

            PlatformStock stock = stocks.get(0);
            System.out.println("Found platform stock: " + stock.getStockSymbol() + " (ID: " + stock.getPlatformStockId() + ")");

            // Find current holding of this stock
            BigDecimal currentQuantity = BigDecimal.ZERO;
            List<Holding> holdings = holdingRepository.findByPortfolio(portfolio);

            for (Holding holding : holdings) {
                if (holding.getPlatformStock().getStockSymbol().equals(stockSymbol)) {
                    currentQuantity = holding.getQuantity();
                    System.out.println("Current holding quantity: " + currentQuantity);
                    break;
                }
            }

            // Get the latest market price for this stock
            BigDecimal currentPrice;
            MarketCandle latestCandle = marketCandleRepository.findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                    stock, MarketCandle.Timeframe.M1);

            if (latestCandle != null) {
                currentPrice = latestCandle.getClosePrice();
                System.out.println("Current price: " + currentPrice);
            } else {
                result.put("success", false);
                result.put("message", "Could not retrieve current price for " + stockSymbol);
                loggingService.logAction("Trading capacity calculation failed: No price data for: " + stockSymbol);
                return result;
            }

            // Get the portfolio's reserved cash
            BigDecimal reservedCash = portfolio.getReservedCash() != null ?
                    portfolio.getReservedCash() : BigDecimal.ZERO;
            System.out.println("Reserved cash: " + reservedCash);

            // Calculate maximum buy capacity based on available cash
            BigDecimal maxBuyQuantity = BigDecimal.ZERO;
            if (currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                maxBuyQuantity = reservedCash.divide(currentPrice, 8, RoundingMode.DOWN);
                System.out.println("Max buy quantity: " + maxBuyQuantity);
            }

            // The maximum sell quantity is simply the current quantity held
            BigDecimal maxSellQuantity = currentQuantity;
            System.out.println("Max sell quantity: " + maxSellQuantity);

            // Prepare the result
            Map<String, Object> capacityData = new HashMap<>();
            capacityData.put("stockSymbol", stockSymbol);
            capacityData.put("currentHolding", currentQuantity);
            capacityData.put("reservedCash", reservedCash.setScale(8, RoundingMode.HALF_UP));
            capacityData.put("currentPrice", currentPrice);
            capacityData.put("maxBuyQuantity", maxBuyQuantity);
            capacityData.put("maxSellQuantity", maxSellQuantity);

            result.put("success", true);
            result.put("capacity", capacityData);

            // Log success
            loggingService.logAction("Calculated trading capacity for " + stockSymbol + " in portfolio " + portfolio.getPortfolioName());

        } catch (Exception e) {
            System.out.println("Error calculating trading capacity: " + e.getMessage());
            e.printStackTrace();

            // Log error
            loggingService.logError("Error calculating trading capacity: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while calculating trading capacity: " + e.getMessage());
        }

        return result;
    }

    private void updateHoldingsFromExchange(Portfolio portfolio, ApiKey apiKey) {
        // Get credentials for Binance API
        String encryptedApiKey = apiKey.getApiKeyValueEncrypt();
        String encryptedSecretKey = apiKey.getSecretKeyEncrypt();

        // In a real application, you would need to decrypt these values
        // For this example, we'll assume they're not encrypted
        String apiKeyValue = encryptedApiKey; // In reality: decryptionService.decrypt(encryptedApiKey);
        String secretKeyValue = encryptedSecretKey; // In reality: decryptionService.decrypt(encryptedSecretKey);

        // Get account information from Binance
        Map<String, Object> accountInfo = binanceAccountService.getAccountInfo(apiKeyValue, secretKeyValue);

        // Check if we have a valid response with balances
        boolean apiCallSuccessful = accountInfo != null && accountInfo.containsKey("balances");

        if (apiCallSuccessful) {
            List<Map<String, Object>> balances = (List<Map<String, Object>>) accountInfo.get("balances");

            if (balances != null && !balances.isEmpty()) {
                String platformName = apiKey.getPlatformName();

                // Initialize reserved cash to zero, will be updated if USDT is found
                BigDecimal reservedCash = BigDecimal.ZERO;

                // First, delete all existing holdings for this portfolio
                List<Holding> existingHoldings = holdingRepository.findByPortfolio(portfolio);
                holdingRepository.deleteAll(existingHoldings);

                // Process each balance from Binance
                for (Map<String, Object> balance : balances) {
                    String asset = (String) balance.get("asset");
                    String free = (String) balance.get("free");

                    // Convert String to BigDecimal
                    BigDecimal quantity = new BigDecimal(free);

                    // Only consider assets with non-zero balance
                    if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                        // Special handling for USDT - update reservedCash instead of creating a holding
                        if ("USDT".equals(asset)) {
                            reservedCash = quantity;
                            continue; // Skip creating a holding for USDT
                        }

                        // First try exact match for the asset
                        List<PlatformStock> stocks = platformStockRepository.findByPlatformNameAndStockSymbol(
                                platformName, asset);

                        if (!stocks.isEmpty()) {
                            PlatformStock stock = stocks.get(0);

                            // Create a new holding
                            Holding holding = new Holding();
                            holding.setPortfolio(portfolio);
                            holding.setPlatformStock(stock);
                            holding.setQuantity(quantity);

                            // Save the holding
                            holdingRepository.save(holding);
                        } else {
                            // If no direct match, try looking for trading pairs
                            List<PlatformStock> tradingPairs = platformStockRepository.findByPlatformNameAndStockSymbol(
                                    platformName, asset + "USDT");

                            if (!tradingPairs.isEmpty()) {
                                PlatformStock stock = tradingPairs.get(0);

                                // Create a new holding
                                Holding holding = new Holding();
                                holding.setPortfolio(portfolio);
                                holding.setPlatformStock(stock);
                                holding.setQuantity(quantity);

                                // Save the holding
                                holdingRepository.save(holding);
                            }
                        }
                    }
                }

                // Update portfolio with the reserved cash
                portfolio.setReservedCash(reservedCash);
                portfolioRepository.save(portfolio);
            }
        } else {
            throw new RuntimeException("Failed to retrieve account information from Binance API");
        }
    }
}
