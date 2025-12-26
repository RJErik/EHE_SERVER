package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.dto.CashDetails;
import com.example.ehe_server.dto.PortfolioDetailsResponse;
import com.example.ehe_server.dto.StockDetails;
import com.example.ehe_server.entity.*;
import com.example.ehe_server.exception.custom.PortfolioNotFoundException;
import com.example.ehe_server.repository.*;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioDetailsServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class PortfolioDetailsService implements PortfolioDetailsServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final LoggingServiceInterface loggingService;
    private final PortfolioValueServiceInterface portfolioValueService;
    private final EntityManager entityManager;  // ← Add this

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PortfolioDetailsService(
            PortfolioRepository portfolioRepository,
            HoldingRepository holdingRepository,
            MarketCandleRepository marketCandleRepository,
            LoggingServiceInterface loggingService,
            PortfolioValueServiceInterface portfolioValueService,
            EntityManager entityManager) {  // ← Add this
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.loggingService = loggingService;
        this.portfolioValueService = portfolioValueService;
        this.entityManager = entityManager;  // ← Add this
    }

    @Override
    public PortfolioDetailsResponse getPortfolioDetails(Integer userId, Integer portfolioId) {

        // Check if portfolio exists and belongs to the user
        Optional<Portfolio> portfolioOptional = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId);
        if (portfolioOptional.isEmpty()) {
            throw new PortfolioNotFoundException(portfolioId);
        }

        Portfolio portfolio = portfolioOptional.get();
        ApiKey apiKey = portfolio.getApiKey();
        String platformName = apiKey != null ? apiKey.getPlatform().getPlatformName() : "Unknown";

        try {
            // Update holdings on exchange before computing details
//            portfolioValueService.updateHoldings(userId, portfolioId);
//
//            // ✅ CLEAR THE PERSISTENCE CONTEXT
//            entityManager.flush();   // Ensure all changes are written to DB
//            entityManager.clear();   // Clear the cache

        } catch (Exception e) {
            loggingService.logError("Failed to update holdings before details: " + e.getMessage(), e);
        }

        // ✅ Reload portfolio from DB (fresh copy)
        portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        // ✅ Get holdings AFTER clearing cache
        List<Holding> holdings = holdingRepository.findByPortfolio(portfolio);

        // Handle reserved cash
        BigDecimal reservedCash = portfolio.getReservedCash() != null ?
                portfolio.getReservedCash() : BigDecimal.ZERO;

        // Determine cash currency based on platform
        String cashCurrency = "Binance".equalsIgnoreCase(platformName) ? "USDT" : "USD";

        // Process holdings
        List<StockDetails> stocksList = new ArrayList<>();
        BigDecimal totalPortfolioValue = reservedCash;
        CashDetails cashDetails = new CashDetails(cashCurrency, reservedCash.setScale(2, RoundingMode.HALF_UP));

        for (Holding holding : holdings) {
            PlatformStock stock = holding.getPlatformStock();
            String symbol = stock.getStock().getStockName();
            BigDecimal quantity = holding.getQuantity();

            // Extract the base symbol (remove USDT suffix if present)
            String baseSymbol = symbol.endsWith("USDT") ?
                    symbol.substring(0, symbol.length() - 4) : symbol;

            // Get the latest price from the market candle repository
            MarketCandle latestCandle = marketCandleRepository.findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                    stock, MarketCandle.Timeframe.M1);

            if (latestCandle != null) {
                BigDecimal price = latestCandle.getClosePrice();
                BigDecimal totalValue = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);

                // Add to total portfolio value
                totalPortfolioValue = totalPortfolioValue.add(totalValue);

                stocksList.add(new StockDetails(baseSymbol, totalValue));
            }
        }

        // Log success
        loggingService.logAction("Retrieved details for portfolio: " + portfolio.getPortfolioName());

        // Build and return the final DTO
        return new PortfolioDetailsResponse(
                portfolio.getPortfolioName(),
                portfolio.getCreationDate().format(DATE_FORMATTER),
                platformName,
                cashDetails,
                stocksList,
                totalPortfolioValue.setScale(2, RoundingMode.HALF_UP)
        );
    }
}
