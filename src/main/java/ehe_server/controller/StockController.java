package ehe_server.controller;

import ehe_server.annotation.validation.NotEmptyString;
import ehe_server.annotation.validation.NotNullField;
import ehe_server.dto.*;
import ehe_server.exception.custom.MissingPlatformNameException;
import ehe_server.exception.custom.MissingPortfolioIdException;
import ehe_server.exception.custom.MissingStockSymbolException;
import ehe_server.service.audit.UserContextService;
import ehe_server.service.intf.stock.CandleRetrievalServiceInterface;
import ehe_server.service.intf.stock.PlatformServiceInterface;
import ehe_server.service.intf.stock.StockServiceInterface;
import ehe_server.service.intf.trade.TradingCapacityServiceInterface;
import ehe_server.service.intf.trade.TradingServiceInterface;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@Validated
public class StockController {

    private final PlatformServiceInterface platformService;
    private final StockServiceInterface stockService;
    private final TradingCapacityServiceInterface tradingCapacityService;
    private final TradingServiceInterface tradingService;
    private final UserContextService userContextService;
    private final CandleRetrievalServiceInterface candleRetrievalService;
    private final MessageSource messageSource;

    public StockController(
            PlatformServiceInterface platformService,
            StockServiceInterface stockService,
            TradingCapacityServiceInterface tradingCapacityService,
            TradingServiceInterface tradingService,
            UserContextService userContextService,
            CandleRetrievalServiceInterface candleRetrievalService,
            MessageSource messageSource) {
        this.platformService = platformService;
        this.stockService = stockService;
        this.tradingCapacityService = tradingCapacityService;
        this.tradingService = tradingService;
        this.userContextService = userContextService;
        this.candleRetrievalService = candleRetrievalService;
        this.messageSource = messageSource;
    }

    /**
     * GET /api/user/platforms
     * Retrieve all available trading platforms
     */
    @GetMapping("/platforms")
    public ResponseEntity<Map<String, Object>> getPlatforms() {
        List<String> platforms = platformService.getAllPlatforms();

        String successMessage = messageSource.getMessage(
                "success.message.stock.platform.get",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("platforms", platforms);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/user/platforms/{platform}/stocks
     * Retrieve all stocks for a specific platform
     */
    @GetMapping("/platforms/{platform}/stocks")
    public ResponseEntity<Map<String, Object>> getStocksByPlatform(
            @NotEmptyString(exception = MissingPlatformNameException.class)
            @PathVariable
            String platform) {
        StocksByPlatformResponse stocksByPlatformResponse = stockService.getStocksByPlatform(platform);

        String successMessage = messageSource.getMessage(
                "success.message.stock.stock.get",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("stocks", stocksByPlatformResponse);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/user/portfolios/{portfolioId}/stocks/{stockSymbol}/trading-capacity
     * Retrieve trading capacity for a specific stock in a portfolio
     */
    @GetMapping("/portfolios/{portfolioId}/stocks/{stockSymbol}/trading-capacity")
    public ResponseEntity<Map<String, Object>> getTradingCapacity(
            @NotNullField(exception = MissingPortfolioIdException.class)
            @PathVariable Integer portfolioId,
            @NotEmptyString(exception = MissingStockSymbolException.class)
            @PathVariable String stockSymbol) {

        TradingCapacityResponse tradingCapacityResponse = tradingCapacityService.getTradingCapacity(
                userContextService.getCurrentUserId(),
                portfolioId,
                stockSymbol);

        String successMessage = messageSource.getMessage(
                "success.message.stock.tradeCapacity.get",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("capacity", tradingCapacityResponse);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * POST /api/user/trades
     * Execute a market order (buy or sell)
     */
    @PostMapping("/trades")
    public ResponseEntity<Map<String, Object>> executeTrade(
            @Valid @RequestBody TradeRequest request) {

        TradeExecutionResponse tradeExecutionResponse = tradingService.executeTrade(
                userContextService.getCurrentUserId(),
                request.getPortfolioId(),
                request.getStockSymbol(),
                request.getAction(),
                request.getQuantity(),
                request.getQuantityType());

        String successMessage = messageSource.getMessage(
                "success.message.stock.trade",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("order", tradeExecutionResponse);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    /**
     * GET /api/user/candles/by-sequence?platform=X&stockSymbol=Y&timeframe=Z&fromSequence=1&toSequence=100
     * Retrieve candles by sequence number range
     */
    @GetMapping("/candles/by-sequence")
    public ResponseEntity<Map<String, Object>> getCandlesBySequence(
            @Valid @ModelAttribute CandlesBySequenceRequest request) {

        CandlesResponse candlesResponse = candleRetrievalService.getCandlesBySequence(
                request.getPlatform(),
                request.getStockSymbol(),
                request.getTimeframe(),
                request.getFromSequence(),
                request.getToSequence()
        );

        String successMessage = messageSource.getMessage(
                "success.message.stock.candles.sequence.get",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("data", candlesResponse);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/user/candles/by-date?platform=X&stockSymbol=Y&timeframe=Z&fromDate=2024-01-01T00:00:00&toDate=2024-12-31T23:59:59
     * Retrieve candles by date range
     */
    @GetMapping("/candles/by-date")
    public ResponseEntity<Map<String, Object>> getCandlesByDate(
            @Valid @ModelAttribute CandlesByDateRequest request) {

        CandlesResponse candlesResponse = candleRetrievalService.getCandlesByDate(
                request.getPlatform(),
                request.getStockSymbol(),
                request.getTimeframe(),
                request.getFromDate(),
                request.getToDate()
        );

        String successMessage = messageSource.getMessage(
                "success.message.stock.candles.date.get",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("data", candlesResponse);

        return ResponseEntity.ok(responseBody);
    }
}