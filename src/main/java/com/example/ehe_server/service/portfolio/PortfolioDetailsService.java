package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.dto.CashDetails;
import com.example.ehe_server.dto.PortfolioDetailsResponse;
import com.example.ehe_server.dto.StockDetails;
import com.example.ehe_server.entity.*;
import com.example.ehe_server.exception.custom.PortfolioNotFoundException;
import com.example.ehe_server.repository.*;
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

    public PortfolioDetailsService(
            PortfolioRepository portfolioRepository,
            HoldingRepository holdingRepository,
            MarketCandleRepository marketCandleRepository,
            LoggingServiceInterface loggingService) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.loggingService = loggingService;
    }

    @Override
    public PortfolioDetailsResponse getPortfolioDetails(Integer userId, Integer portfolioId) {

        // Check if portfolio exists and belongs to the user
        Optional<Portfolio> portfolioOptional = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId);
        if (portfolioOptional.isEmpty()) {
            throw new PortfolioNotFoundException(portfolioId, userId);
        }

        Portfolio portfolio = portfolioOptional.get();
        ApiKey apiKey = portfolio.getApiKey();
        String platformName = apiKey != null ? apiKey.getPlatformName() : "Unknown";

        // Get holdings for this portfolio
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

        cashDetails.setCurrency(cashCurrency);
        cashDetails.setValue(reservedCash.setScale(2, RoundingMode.HALF_UP));


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

                StockDetails stockDetailsDto = new StockDetails(baseSymbol, totalValue);

                stocksList.add(stockDetailsDto);
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
