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
import com.example.ehe_server.service.intf.binance.BinanceAccountServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PortfolioValueService implements PortfolioValueServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final PlatformStockRepository platformStockRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final LoggingServiceInterface loggingService;
    private final BinanceAccountServiceInterface binanceAccountService;

    // Threshold to filter out invalid/test balances
    private static final BigDecimal MAX_VALID_BALANCE = new BigDecimal("18000");

    public PortfolioValueService(
            PortfolioRepository portfolioRepository,
            HoldingRepository holdingRepository,
            PlatformStockRepository platformStockRepository,
            MarketCandleRepository marketCandleRepository,
            LoggingServiceInterface loggingService,
            BinanceAccountServiceInterface binanceAccountService) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.platformStockRepository = platformStockRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.loggingService = loggingService;
        this.binanceAccountService = binanceAccountService;
    }

    @Override
    public PortfolioValueResponse calculatePortfolioValue(Integer userId, Integer portfolioId) {
        long startTime = System.currentTimeMillis();
        System.out.println("[TIMING] calculatePortfolioValue started at: " + startTime);

        System.out.println("Starting portfolio value calculation for portfolioId: " + portfolioId);

        // Check if portfolio exists and belongs to the user
        long portfolioCheckStart = System.currentTimeMillis();
        Optional<Portfolio> portfolioOptional = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId);
        System.out.println("[TIMING] Portfolio lookup completed in: " + (System.currentTimeMillis() - portfolioCheckStart) + "ms");

        if (portfolioOptional.isEmpty()) {
            System.out.println("Portfolio not found or doesn't belong to user. PortfolioId: " + portfolioId + ", UserId: " + userId);
            throw new PortfolioNotFoundException(portfolioId);
        }

        Portfolio portfolio = portfolioOptional.get();
        System.out.println("Found portfolio: " + portfolio.getPortfolioName() + " (ID: " + portfolio.getPortfolioId() + ")");

        // First try to update holdings from exchange API
        System.out.println("Attempting to update holdings from exchange API");
        long updateHoldingsStart = System.currentTimeMillis();
        updateHoldings(userId, portfolio);
        System.out.println("[TIMING] updateHoldings completed in: " + (System.currentTimeMillis() - updateHoldingsStart) + "ms");

        // Get holdings for this portfolio (either updated or existing)
        long getHoldingsStart = System.currentTimeMillis();
        List<Holding> holdings = holdingRepository.findByPortfolio(portfolio);
        System.out.println("[TIMING] Get holdings completed in: " + (System.currentTimeMillis() - getHoldingsStart) + "ms");
        System.out.println("Found " + holdings.size() + " holdings for portfolio");

        // Get the portfolio again to ensure we have the latest reservedCash value
        long portfolioRefreshStart = System.currentTimeMillis();
        portfolio = portfolioRepository.findById(portfolioId).orElse(portfolio);
        System.out.println("[TIMING] Portfolio refresh completed in: " + (System.currentTimeMillis() - portfolioRefreshStart) + "ms");

        BigDecimal reservedCash = Optional.ofNullable(portfolio.getReservedCash()).orElse(BigDecimal.ZERO);

        // Calculate total value in USDT
        BigDecimal totalValue = BigDecimal.ZERO;

        // Add reserved cash to total value if it's not null
        if (portfolio.getReservedCash() != null) {
            totalValue = totalValue.add(portfolio.getReservedCash());
            System.out.println("Added reserved cash to total value: " + portfolio.getReservedCash());
        }

        List<HoldingDetails> holdingsList = new ArrayList<>();

        long holdingsProcessingStart = System.currentTimeMillis();
        System.out.println("[TIMING] Starting holdings processing loop");

        for (Holding holding : holdings) {
            long holdingStart = System.currentTimeMillis();

            PlatformStock stock = holding.getPlatformStock();
            String symbol = stock.getStock().getStockName();
            BigDecimal quantity = holding.getQuantity();
            BigDecimal valueInUsdt = BigDecimal.ZERO;

            // Look for the USDT trading pair
            String usdtPairSymbol = symbol.endsWith("USDT") ? symbol : symbol + "USDT";

            long stockLookupStart = System.currentTimeMillis();
            List<PlatformStock> usdtPairs = platformStockRepository.findByPlatformPlatformNameAndStockStockName(
                    stock.getPlatform().getPlatformName(), usdtPairSymbol);
            System.out.println("[TIMING] Stock lookup for " + symbol + " completed in: " + (System.currentTimeMillis() - stockLookupStart) + "ms");

            if (!usdtPairs.isEmpty()) {
                PlatformStock usdtPair = usdtPairs.get(0);

                long candleLookupStart = System.currentTimeMillis();
                MarketCandle latestCandle = marketCandleRepository.findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                        usdtPair, MarketCandle.Timeframe.M1);
                System.out.println("[TIMING] Candle lookup for " + symbol + " completed in: " + (System.currentTimeMillis() - candleLookupStart) + "ms");

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

            System.out.println("[TIMING] Holding " + symbol + " processed in: " + (System.currentTimeMillis() - holdingStart) + "ms");
        }

        System.out.println("[TIMING] All holdings processed in: " + (System.currentTimeMillis() - holdingsProcessingStart) + "ms");

        // Round to 2 decimal places
        totalValue = totalValue.setScale(2, RoundingMode.HALF_UP);
        System.out.println("Final portfolio value: " + totalValue);

        // Log success
        long loggingStart = System.currentTimeMillis();
        loggingService.logAction("Calculated portfolio value for " + portfolio.getPortfolioName() + ": " + totalValue + " USDT");
        System.out.println("[TIMING] Logging completed in: " + (System.currentTimeMillis() - loggingStart) + "ms");

        System.out.println("[TIMING] calculatePortfolioValue TOTAL TIME: " + (System.currentTimeMillis() - startTime) + "ms");

        return new PortfolioValueResponse(
                portfolioId,
                portfolio.getPortfolioName(),
                totalValue,
                reservedCash.setScale(2, RoundingMode.HALF_UP),
                holdingsList
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public HoldingsUpdateResponse updateHoldings(Integer userId, Integer portfolioId) {
        long startTime = System.currentTimeMillis();
        System.out.println("[TIMING] updateHoldings(ID wrapper) started at: " + startTime);
        System.out.println("Starting updateHoldings (ID wrapper) for portfolioId: " + portfolioId);

        long portfolioLookupStart = System.currentTimeMillis();
        Optional<Portfolio> portfolioOptional = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId);
        System.out.println("[TIMING] Portfolio lookup in updateHoldings(ID) completed in: " + (System.currentTimeMillis() - portfolioLookupStart) + "ms");

        if (portfolioOptional.isEmpty()) {
            System.out.println("Portfolio not found. PortfolioId: " + portfolioId);
            throw new PortfolioNotFoundException(portfolioId);
        }

        // Delegate to the new logic method
        HoldingsUpdateResponse response = updateHoldings(userId, portfolioOptional.get());
        System.out.println("[TIMING] updateHoldings(ID wrapper) TOTAL TIME: " + (System.currentTimeMillis() - startTime) + "ms");
        return response;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public HoldingsUpdateResponse updateHoldings(Integer userId, Portfolio portfolio) {
        long startTime = System.currentTimeMillis();
        System.out.println("[TIMING] updateHoldings(Portfolio object) started at: " + startTime);

        System.out.println("Starting updateHoldings method for portfolio: " + portfolio.getPortfolioName());

        // 1. Validation: Instead of a DB Query, we check ownership in memory
        long validationStart = System.currentTimeMillis();
        if (!portfolio.getUser().getUserId().equals(userId)) {
            System.out.println("Portfolio doesn't belong to user. PortfolioId: " + portfolio.getPortfolioId() + ", UserId: " + userId);
            // Throw the same exception so the frontend receives the same 404 error
            throw new PortfolioNotFoundException(portfolio.getPortfolioId());
        }
        System.out.println("[TIMING] Ownership validation completed in: " + (System.currentTimeMillis() - validationStart) + "ms");

        System.out.println("Using portfolio: " + portfolio.getPortfolioName() + " (ID: " + portfolio.getPortfolioId() + ")");

        long apiKeyCheckStart = System.currentTimeMillis();
        ApiKey apiKey = portfolio.getApiKey();
        if (apiKey == null) {
            System.out.println("No API key associated with portfolio. Cannot update holdings.");
            throw new ApiKeyMissingException(portfolio.getPortfolioId());
        }
        System.out.println("[TIMING] API key check completed in: " + (System.currentTimeMillis() - apiKeyCheckStart) + "ms");

        System.out.println("Using API key ID: " + apiKey.getApiKeyId() + " for platform: " + apiKey.getPlatform().getPlatformName());

        // Get credentials for Binance API
        String apiKeyValue = apiKey.getApiKeyValue();
        String secretKeyValue = apiKey.getSecretKey();

        // Get account information from Binance
        System.out.println("Calling Binance API to get account info");
        long binanceApiStart = System.currentTimeMillis();
        Map<String, Object> accountInfo = binanceAccountService.getAccountInfo(apiKeyValue, secretKeyValue);
        System.out.println("[TIMING] Binance API call completed in: " + (System.currentTimeMillis() - binanceApiStart) + "ms");
        System.out.println("Received response from Binance API: " + accountInfo);

        // Check if we have a valid response with balances
        boolean apiCallSuccessful = accountInfo != null && accountInfo.containsKey("balances");

        if (apiCallSuccessful) {
            long processingStart = System.currentTimeMillis();

            List<Map<String, Object>> balances = (List<Map<String, Object>>) accountInfo.get("balances");
            System.out.println("Found " + (balances != null ? balances.size() : 0) + " balances in Binance response");

            if (balances != null && !balances.isEmpty()) {
                String platformName = apiKey.getPlatform().getPlatformName();

                // Initialize reserved cash to zero, will be updated if USDT is found
                BigDecimal reservedCash = BigDecimal.ZERO;
                boolean usdtFound = false;

                // OPTIMIZATION 1: Filter out zero balances and invalid test data
                long filterStart = System.currentTimeMillis();
                List<Map<String, Object>> validBalances = balances.stream()
                        .filter(balance -> {
                            String free = (String) balance.get("free");
                            BigDecimal quantity = new BigDecimal(free);
                            // Filter out zero balances and suspiciously high test values (like 18446)
                            return quantity.compareTo(BigDecimal.ZERO) > 0
                                    && quantity.compareTo(MAX_VALID_BALANCE) < 0;
                        })
                        .collect(Collectors.toList());

                System.out.println("[TIMING] Balance filtering completed in: " + (System.currentTimeMillis() - filterStart) + "ms");
                System.out.println("Filtered to " + validBalances.size() + " valid balances (from " + balances.size() + " total)");

                // OPTIMIZATION 2: Build set of all trading pair symbols we need to look up
                long symbolCollectionStart = System.currentTimeMillis();
                Set<String> tradingPairSymbols = new HashSet<>();
                for (Map<String, Object> balance : validBalances) {
                    String asset = (String) balance.get("asset");
                    if (!"USDT".equals(asset)) {  // Skip USDT as it becomes reserved cash
                        tradingPairSymbols.add(asset + "USDT");
                    }
                }
                System.out.println("[TIMING] Symbol collection completed in: " + (System.currentTimeMillis() - symbolCollectionStart) + "ms");
                System.out.println("Collected " + tradingPairSymbols.size() + " trading pair symbols to lookup");

                // OPTIMIZATION 3: Single batch query to get all matching stocks
                long batchQueryStart = System.currentTimeMillis();
                List<PlatformStock> matchingStocks = platformStockRepository
                        .findByPlatformNameAndStockNameIn(platformName, tradingPairSymbols);
                System.out.println("[TIMING] Batch stock query completed in: " + (System.currentTimeMillis() - batchQueryStart) + "ms");
                System.out.println("Found " + matchingStocks.size() + " matching stocks in database");

                // OPTIMIZATION 4: Build lookup map for O(1) access
                long mapBuildStart = System.currentTimeMillis();
                Map<String, PlatformStock> stockMap = matchingStocks.stream()
                        .collect(Collectors.toMap(ps -> ps.getStock().getStockName(), stock -> stock));
                System.out.println("[TIMING] Stock map building completed in: " + (System.currentTimeMillis() - mapBuildStart) + "ms");

                // Delete all existing holdings for this portfolio
                long deleteHoldingsStart = System.currentTimeMillis();
                List<Holding> existingHoldings = holdingRepository.findByPortfolio(portfolio);
                System.out.println("Deleting " + existingHoldings.size() + " existing holdings");
                holdingRepository.deleteAll(existingHoldings);
                System.out.println("[TIMING] Delete existing holdings completed in: " + (System.currentTimeMillis() - deleteHoldingsStart) + "ms");

                // Track successful additions
                int addedCount = 0;

                // Process each valid balance
                long balanceProcessingStart = System.currentTimeMillis();
                System.out.println("[TIMING] Starting balance processing loop");

                for (Map<String, Object> balance : validBalances) {
                    long balanceStart = System.currentTimeMillis();

                    String asset = (String) balance.get("asset");
                    String free = (String) balance.get("free");
                    System.out.println("Processing balance: " + asset + " = " + free);

                    BigDecimal quantity = new BigDecimal(free);

                    // Special handling for USDT - update reservedCash instead of creating a holding
                    if ("USDT".equals(asset)) {
                        System.out.println("USDT asset found with balance: " + quantity + ". Setting as reserved cash.");
                        reservedCash = quantity;
                        usdtFound = true;
                        continue; // Skip creating a holding for USDT
                    }

                    // Look up the trading pair in our pre-built map (O(1) operation, no database query!)
                    String tradingPairSymbol = asset + "USDT";
                    PlatformStock stock = stockMap.get(tradingPairSymbol);

                    if (stock != null) {
                        System.out.println("Found trading pair for " + asset + ": " + stock.getStock().getStockName());

                        // Create a new holding
                        long saveHoldingStart = System.currentTimeMillis();
                        Holding holding = new Holding();
                        holding.setPortfolio(portfolio);
                        holding.setPlatformStock(stock);
                        holding.setQuantity(quantity);

                        // Save the holding
                        Holding savedHolding = holdingRepository.save(holding);
                        System.out.println("[TIMING] Save holding for " + asset + " completed in: " + (System.currentTimeMillis() - saveHoldingStart) + "ms");
                        System.out.println("Saved new holding with ID: " + savedHolding.getHoldingId());
                        addedCount++;
                    } else {
                        System.out.println("No matching platform stock found for asset: " + asset + " (looked for " + tradingPairSymbol + ")");
                    }

                    System.out.println("[TIMING] Balance " + asset + " processed in: " + (System.currentTimeMillis() - balanceStart) + "ms");
                }

                System.out.println("[TIMING] All balances processed in: " + (System.currentTimeMillis() - balanceProcessingStart) + "ms");

                // Handle special case: Check for USDT in the original balances list if not found in valid balances
                if (!usdtFound) {
                    for (Map<String, Object> balance : balances) {
                        String asset = (String) balance.get("asset");
                        if ("USDT".equals(asset)) {
                            String free = (String) balance.get("free");
                            BigDecimal quantity = new BigDecimal(free);
                            if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                                reservedCash = quantity;
                                usdtFound = true;
                                System.out.println("Found USDT in original balances: " + quantity);
                            }
                            break;
                        }
                    }
                }

                // Update portfolio with the reserved cash
                long savePortfolioStart = System.currentTimeMillis();
                portfolio.setReservedCash(reservedCash);
                portfolioRepository.save(portfolio);
                System.out.println("[TIMING] Save portfolio with reserved cash completed in: " + (System.currentTimeMillis() - savePortfolioStart) + "ms");

                // Log whether USDT was found
                if (usdtFound) {
                    System.out.println("Updated portfolio with reserved cash: " + reservedCash);
                } else {
                    System.out.println("No USDT found in balances. Reserved cash set to zero.");
                }

                // Prepare success response
                System.out.println("Successfully updated " + addedCount + " holdings");

                // Log success
                long loggingStart = System.currentTimeMillis();
                loggingService.logAction("Updated holdings for portfolio " + portfolio.getPortfolioName() +
                        ", added " + addedCount + " holdings, reserved cash: " + reservedCash);
                System.out.println("[TIMING] Logging completed in: " + (System.currentTimeMillis() - loggingStart) + "ms");

                System.out.println("[TIMING] Balance processing completed in: " + (System.currentTimeMillis() - processingStart) + "ms");
                System.out.println("[TIMING] updateHoldings(Portfolio object) TOTAL TIME: " + (System.currentTimeMillis() - startTime) + "ms");

                return new HoldingsUpdateResponse(
                        addedCount,
                        reservedCash
                );

            } else {
                System.out.println("No balances found in Binance response");
                throw new NoBalancesFoundException(portfolio.getPortfolioId());
            }
        } else {
            // API call was not successful, don't modify existing holdings
            System.out.println("Error in Binance API response. Keeping existing holdings.");
            throw new BinanceApiCommunicationException();
        }
    }
}