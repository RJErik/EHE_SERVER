package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.entity.*;
import com.example.ehe_server.repository.*;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioDetailsServiceInterface;
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
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserContextService userContextService;

    public PortfolioDetailsService(
            PortfolioRepository portfolioRepository,
            HoldingRepository holdingRepository,
            MarketCandleRepository marketCandleRepository,
            LoggingServiceInterface loggingService, UserContextService userContextService) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    @Override
    public Map<String, Object> getPortfolioDetails(Integer portfolioId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get current user ID from user context
            Integer userId = Integer.parseInt(userContextService.getCurrentUserIdAsString());

            // Check if portfolio exists and belongs to the user
            Optional<Portfolio> portfolioOptional = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId);
            if (portfolioOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "Portfolio not found or doesn't belong to the user");
                loggingService.logAction("Portfolio details retrieval failed: Portfolio not found or doesn't belong to user, portfolioId=" + portfolioId);
                return result;
            }

            Portfolio portfolio = portfolioOptional.get();
            ApiKey apiKey = portfolio.getApiKey();
            String platformName = apiKey != null ? apiKey.getPlatformName() : "Unknown";

            // Get holdings for this portfolio
            List<Holding> holdings = holdingRepository.findByPortfolio(portfolio);

            // Prepare basic portfolio details
            Map<String, Object> portfolioDetails = new HashMap<>();
            portfolioDetails.put("name", portfolio.getPortfolioName());
            portfolioDetails.put("creationDate", portfolio.getCreationDate().format(DATE_FORMATTER));
            portfolioDetails.put("platform", platformName);

            // Handle reserved cash
            BigDecimal reservedCash = portfolio.getReservedCash() != null ?
                    portfolio.getReservedCash() : BigDecimal.ZERO;

            // Determine cash currency based on platform
            String cashCurrency = "Binance".equalsIgnoreCase(platformName) ? "USDT" : "USD";

            Map<String, Object> cashDetails = new HashMap<>();
            cashDetails.put("currency", cashCurrency);
            cashDetails.put("value", reservedCash.setScale(2, RoundingMode.HALF_UP));

            portfolioDetails.put("reservedCash", cashDetails);

            // Process holdings
            List<Map<String, Object>> stocksList = new ArrayList<>();
            BigDecimal totalPortfolioValue = reservedCash;

            for (Holding holding : holdings) {
                PlatformStock stock = holding.getPlatformStock();
                String symbol = stock.getStockSymbol();
                BigDecimal quantity = holding.getQuantity();

                // Extract the base symbol (remove USDT suffix if present)
                String baseSymbol = symbol;
                if (symbol.endsWith("USDT")) {
                    baseSymbol = symbol.substring(0, symbol.length() - 4);
                }

                // Get the latest price from the market candle repository
                MarketCandle latestCandle = marketCandleRepository.findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                        stock, MarketCandle.Timeframe.M1);

                if (latestCandle != null) {
                    BigDecimal price = latestCandle.getClosePrice();
                    BigDecimal totalValue = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);

                    // Add to total portfolio value
                    totalPortfolioValue = totalPortfolioValue.add(totalValue);

                    // Create stock details
                    Map<String, Object> stockDetail = new HashMap<>();
                    stockDetail.put("symbol", baseSymbol);
                    stockDetail.put("value", totalValue);

                    stocksList.add(stockDetail);
                }
            }

            // Add stocks list to portfolio details
            portfolioDetails.put("stocks", stocksList);
            portfolioDetails.put("totalValue", totalPortfolioValue.setScale(2, RoundingMode.HALF_UP));

            // Prepare success response
            result.put("success", true);
            result.put("portfolio", portfolioDetails);

            // Log success
            loggingService.logAction("Retrieved details for portfolio: " + portfolio.getPortfolioName());

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error retrieving portfolio details: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while retrieving portfolio details: " + e.getMessage());
        }

        return result;
    }
}
