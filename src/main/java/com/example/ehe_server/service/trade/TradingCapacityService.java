package com.example.ehe_server.service.trade;

import com.example.ehe_server.dto.TradingCapacityResponse;
import com.example.ehe_server.entity.*;
import com.example.ehe_server.exception.custom.ApiKeyMissingException;
import com.example.ehe_server.exception.custom.PlatformStockNotFoundException;
import com.example.ehe_server.exception.custom.PortfolioNotFoundException;
import com.example.ehe_server.exception.custom.PriceDataNotFoundException;
import com.example.ehe_server.repository.*;
import com.example.ehe_server.service.intf.binance.BinanceAccountServiceInterface;
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
    private final BinanceAccountServiceInterface binanceAccountService;

    public TradingCapacityService(
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
    @Transactional
    public TradingCapacityResponse getTradingCapacity(Integer userId, Integer portfolioId, String stockSymbol) {

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

        // Update holdings from exchange API
        try {
            updateHoldingsFromExchange(portfolio, apiKey);
        } catch (Exception e) {
            // Log the error but continue with existing data
            loggingService.logError("Warning: Failed to update holdings from exchange: " + e.getMessage(), e);
        }

        // Get platform and stock
        String platformName = apiKey.getPlatform().getPlatformName();

        List<PlatformStock> stocks = platformStockRepository.findByPlatformPlatformNameAndStockStockSymbol(
                platformName, stockSymbol);

        if (stocks.isEmpty()) {
            if (!stockSymbol.endsWith("USDT")) {
                stocks = platformStockRepository.findByPlatformPlatformNameAndStockStockSymbol(
                        platformName, stockSymbol + "USDT");
                if (!stocks.isEmpty()) {
                    stockSymbol = stockSymbol + "USDT";
                }
            }

            if (stocks.isEmpty()) {
                if (stockSymbol.endsWith("USDT")) {
                    String baseSymbol = stockSymbol.substring(0, stockSymbol.length() - 4);
                    stocks = platformStockRepository.findByPlatformPlatformNameAndStockStockSymbol(
                            platformName, baseSymbol);
                    if (!stocks.isEmpty()) {
                        stockSymbol = baseSymbol;
                    }
                }
            }

            if (stocks.isEmpty()) {
                throw new PlatformStockNotFoundException(platformName, stockSymbol);
            }
        }

        PlatformStock stock = stocks.getFirst();

        // Find current holding of this stock
        BigDecimal currentQuantity = BigDecimal.ZERO;
        List<Holding> holdings = holdingRepository.findByPortfolio(portfolio);

        for (Holding holding : holdings) {
            if (holding.getPlatformStock().getStock().getStockSymbol().equals(stockSymbol)) {
                currentQuantity = holding.getQuantity();
                break;
            }
        }

        // Get the latest market price for this stock
        BigDecimal currentPrice;
        MarketCandle latestCandle = marketCandleRepository.findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                stock, MarketCandle.Timeframe.M1);

        if (latestCandle != null) {
            currentPrice = latestCandle.getClosePrice();
        } else {
            throw new PriceDataNotFoundException(stock.getStock().getStockSymbol());
        }

        // Get the portfolio's reserved cash
        BigDecimal reservedCash = portfolio.getReservedCash() != null ?
                portfolio.getReservedCash() : BigDecimal.ZERO;

        // Calculate maximum buy capacity based on available cash
        BigDecimal maxBuyQuantity = BigDecimal.ZERO;
        if (currentPrice.compareTo(BigDecimal.ZERO) > 0) {
            maxBuyQuantity = reservedCash.divide(currentPrice, 8, RoundingMode.DOWN);
            System.out.println("Max buy quantity: " + maxBuyQuantity);
        }

        BigDecimal maxSellQuantity = currentQuantity;
        System.out.println("Max sell quantity: " + maxSellQuantity);

        loggingService.logAction("Calculated trading capacity for " + stockSymbol + " in portfolio " + portfolio.getPortfolioName());

        return new TradingCapacityResponse(
                stockSymbol,
                currentQuantity,
                reservedCash.setScale(8, RoundingMode.HALF_UP),
                currentPrice,
                maxBuyQuantity,
                maxSellQuantity
        );
    }

    private void updateHoldingsFromExchange(Portfolio portfolio, ApiKey apiKey) {
        String encryptedApiKey = apiKey.getApiKeyValue();
        String encryptedSecretKey = apiKey.getSecretKey();

        // For now they are not encrypted
        String apiKeyValue = encryptedApiKey; // decryptionService.decrypt(encryptedApiKey);
        String secretKeyValue = encryptedSecretKey; // decryptionService.decrypt(encryptedSecretKey);

        // Get account information from Binance
        Map<String, Object> accountInfo = binanceAccountService.getAccountInfo(apiKeyValue, secretKeyValue);

        boolean apiCallSuccessful = accountInfo != null && accountInfo.containsKey("balances");

        if (apiCallSuccessful) {
            List<Map<String, Object>> balances = (List<Map<String, Object>>) accountInfo.get("balances");

            if (balances != null && !balances.isEmpty()) {
                String platformName = apiKey.getPlatform().getPlatformName();

                BigDecimal reservedCash = BigDecimal.ZERO;

                List<Holding> existingHoldings = holdingRepository.findByPortfolio(portfolio);
                holdingRepository.deleteAll(existingHoldings);
                holdingRepository.flush();

                for (Map<String, Object> balance : balances) {
                    String asset = (String) balance.get("asset");
                    String free = (String) balance.get("free");

                    BigDecimal quantity = new BigDecimal(free);

                    if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                        if ("USDT".equals(asset)) {
                            reservedCash = quantity;
                            continue;
                        }

                        List<PlatformStock> stocks = platformStockRepository.findByPlatformPlatformNameAndStockStockSymbol(
                                platformName, asset);

                        if (!stocks.isEmpty()) {
                            PlatformStock stock = stocks.getFirst();

                            // Create a new holding
                            Holding holding = new Holding();
                            holding.setPortfolio(portfolio);
                            holding.setPlatformStock(stock);
                            holding.setQuantity(quantity);

                            holdingRepository.save(holding);
                        } else {
                            List<PlatformStock> tradingPairs = platformStockRepository.findByPlatformPlatformNameAndStockStockSymbol(
                                    platformName, asset + "USDT");

                            if (!tradingPairs.isEmpty()) {
                                PlatformStock stock = tradingPairs.getFirst();

                                // Create a new holding
                                Holding holding = new Holding();
                                holding.setPortfolio(portfolio);
                                holding.setPlatformStock(stock);
                                holding.setQuantity(quantity);

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