package ehe_server.service.trade;

import ehe_server.dto.TradingCapacityResponse;
import ehe_server.entity.*;
import ehe_server.exception.custom.ApiKeyMissingException;
import ehe_server.exception.custom.PlatformStockNotFoundException;
import ehe_server.exception.custom.PortfolioNotFoundException;
import ehe_server.exception.custom.PriceDataNotFoundException;
import ehe_server.repository.HoldingRepository;
import ehe_server.repository.MarketCandleRepository;
import ehe_server.repository.PlatformStockRepository;
import ehe_server.repository.PortfolioRepository;
import ehe_server.service.intf.log.LoggingServiceInterface;
import ehe_server.service.intf.portfolio.HoldingsSyncServiceInterface;
import ehe_server.service.intf.trade.TradingCapacityServiceInterface;
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
    private final HoldingsSyncServiceInterface holdingsSyncService;

    public TradingCapacityService(
            PortfolioRepository portfolioRepository,
            HoldingRepository holdingRepository,
            PlatformStockRepository platformStockRepository,
            MarketCandleRepository marketCandleRepository,
            LoggingServiceInterface loggingService,
            HoldingsSyncServiceInterface holdingsSyncService) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.platformStockRepository = platformStockRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.loggingService = loggingService;
        this.holdingsSyncService = holdingsSyncService;
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
            holdingsSyncService.syncHoldings(userId, portfolioId);
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
        }

        BigDecimal maxSellQuantity = currentQuantity;

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
}