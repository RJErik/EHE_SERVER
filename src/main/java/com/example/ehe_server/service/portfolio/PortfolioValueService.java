package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.dto.HoldingDetails;
import com.example.ehe_server.dto.HoldingsUpdateResponse;
import com.example.ehe_server.dto.PortfolioValueResponse;
import com.example.ehe_server.entity.*;
import com.example.ehe_server.exception.custom.ApiKeyMissingException;
import com.example.ehe_server.exception.custom.BinanceApiCommunicationException;
import com.example.ehe_server.exception.custom.NoBalancesFoundException;
import com.example.ehe_server.exception.custom.PortfolioNotFoundException;
import com.example.ehe_server.repository.*;
import com.example.ehe_server.service.binance.BinanceAccountService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Transactional
public class PortfolioValueService implements PortfolioValueServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final PlatformStockRepository platformStockRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final LoggingServiceInterface loggingService;
    private final BinanceAccountService binanceAccountService;

    public PortfolioValueService(
            PortfolioRepository portfolioRepository,
            HoldingRepository holdingRepository,
            PlatformStockRepository platformStockRepository,
            MarketCandleRepository marketCandleRepository,
            LoggingServiceInterface loggingService,
            BinanceAccountService binanceAccountService) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.platformStockRepository = platformStockRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.loggingService = loggingService;
        this.binanceAccountService = binanceAccountService;
    }

    @Override
    public PortfolioValueResponse calculatePortfolioValue(Integer userId, Integer portfolioId) {

        System.out.println("Starting portfolio value calculation for portfolioId: " + portfolioId);

        // Check if portfolio exists and belongs to the user
        Optional<Portfolio> portfolioOptional = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId);
        if (portfolioOptional.isEmpty()) {
            System.out.println("Portfolio not found or doesn't belong to user. PortfolioId: " + portfolioId + ", UserId: " + userId);
            throw new PortfolioNotFoundException(portfolioId, userId);
        }

        Portfolio portfolio = portfolioOptional.get();
        System.out.println("Found portfolio: " + portfolio.getPortfolioName() + " (ID: " + portfolio.getPortfolioId() + ")");

        // First try to update holdings from exchange API
        System.out.println("Attempting to update holdings from exchange API");
        updateHoldings(userId, portfolioId);

        // Get holdings for this portfolio (either updated or existing)
        List<Holding> holdings = holdingRepository.findByPortfolio(portfolio);
        System.out.println("Found " + holdings.size() + " holdings for portfolio");

        // Get the portfolio again to ensure we have the latest reservedCash value
        portfolio = portfolioRepository.findById(portfolioId).orElse(portfolio);
        BigDecimal reservedCash = Optional.ofNullable(portfolio.getReservedCash()).orElse(BigDecimal.ZERO);

        // Calculate total value in USDT
        BigDecimal totalValue = BigDecimal.ZERO;

        // Add reserved cash to total value if it's not null
        if (portfolio.getReservedCash() != null) {
            totalValue = totalValue.add(portfolio.getReservedCash());
            System.out.println("Added reserved cash to total value: " + portfolio.getReservedCash());
        }

        List<HoldingDetails> holdingsList = new ArrayList<>();

        for (Holding holding : holdings) {
            PlatformStock stock = holding.getPlatformStock();
            String symbol = stock.getStockSymbol();
            BigDecimal quantity = holding.getQuantity();
            BigDecimal valueInUsdt = BigDecimal.ZERO;

            // Look for the USDT trading pair
            String usdtPairSymbol = symbol.endsWith("USDT") ? symbol : symbol + "USDT";
            List<PlatformStock> usdtPairs = platformStockRepository.findByPlatformNameAndStockSymbol(
                    stock.getPlatformName(), usdtPairSymbol);

            if (!usdtPairs.isEmpty()) {
                PlatformStock usdtPair = usdtPairs.get(0);
                MarketCandle latestCandle = marketCandleRepository.findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                        usdtPair, MarketCandle.Timeframe.M1);

                if (latestCandle != null) {
                    BigDecimal price = latestCandle.getClosePrice();
                    valueInUsdt = quantity.multiply(price);
                }
            }

            totalValue = totalValue.add(valueInUsdt);

            // Add holding details to the DTO list
            holdingsList.add(new HoldingDetails(
                    holding.getHoldingId(),
                    symbol,
                    quantity,
                    valueInUsdt.setScale(2, RoundingMode.HALF_UP)
            ));
        }

        // Round to 2 decimal places
        totalValue = totalValue.setScale(2, RoundingMode.HALF_UP);
        System.out.println("Final portfolio value: " + totalValue);

        // Log success
        loggingService.logAction("Calculated portfolio value for " + portfolio.getPortfolioName() + ": " + totalValue + " USDT");

        return new PortfolioValueResponse(
                portfolioId,
                portfolio.getPortfolioName(),
                totalValue,
                reservedCash.setScale(2, RoundingMode.HALF_UP),
                holdingsList
        );
    }

    @Override
    @Transactional
    public HoldingsUpdateResponse updateHoldings(Integer userId, Integer portfolioId) {

        System.out.println("Starting updateHoldings method for portfolioId: " + portfolioId);

        // Check if portfolio exists and belongs to the user
        Optional<Portfolio> portfolioOptional = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId);
        if (portfolioOptional.isEmpty()) {
            System.out.println("Portfolio not found or doesn't belong to user. PortfolioId: " + portfolioId + ", UserId: " + userId);
            throw new PortfolioNotFoundException(portfolioId, userId);
        }

        Portfolio portfolio = portfolioOptional.get();
        System.out.println("Found portfolio: " + portfolio.getPortfolioName() + " (ID: " + portfolio.getPortfolioId() + ")");

        ApiKey apiKey = portfolio.getApiKey();
        if (apiKey == null) {
            System.out.println("No API key associated with portfolio. Cannot update holdings.");
            throw new ApiKeyMissingException(portfolioId);
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
                System.out.println("Successfully updated " + addedCount + " holdings");

                // Log success
                loggingService.logAction("Updated holdings for portfolio " + portfolio.getPortfolioName() +
                        ", added " + addedCount + " holdings, reserved cash: " + reservedCash);

                return new HoldingsUpdateResponse(
                        addedCount,
                        reservedCash
                );

            } else {
                System.out.println("No balances found in Binance response");
                throw new NoBalancesFoundException(portfolioId);
            }
        } else {
            // API call was not successful, don't modify existing holdings
            System.out.println("Error in Binance API response. Keeping existing holdings.");
            throw new BinanceApiCommunicationException();
        }
    }
}
