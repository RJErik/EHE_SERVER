package com.example.ehe_server.controller;

import com.example.ehe_server.dto.*;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.stock.PlatformServiceInterface;
import com.example.ehe_server.service.intf.stock.StockServiceInterface;
import com.example.ehe_server.service.intf.trade.TradingCapacityServiceInterface;
import com.example.ehe_server.service.intf.trade.TradingServiceInterface;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class StockController {

    private final PlatformServiceInterface platformService;
    private final StockServiceInterface stockService;
    private final TradingCapacityServiceInterface tradingCapacityService;
    private final TradingServiceInterface tradingService;
    private final UserContextService userContextService;
    private final MessageSource messageSource;

    public StockController(
            PlatformServiceInterface platformService,
            StockServiceInterface stockService,
            TradingCapacityServiceInterface tradingCapacityService,
            TradingServiceInterface tradingService,
            UserContextService userContextService,
            MessageSource messageSource) {
        this.platformService = platformService;
        this.stockService = stockService;
        this.tradingCapacityService = tradingCapacityService;
        this.tradingService = tradingService;
        this.userContextService = userContextService;
        this.messageSource = messageSource;
    }

    /**
     * Endpoint to retrieve all available trading platforms
     *
     * @return List of platform names and success status
     */
    @GetMapping("/platforms")
    public ResponseEntity<Map<String, Object>> getPlatforms() {
        // Call automated trade rule retrieval service
        List<String> platforms = platformService.getAllPlatforms();

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.stock.platform.get", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("platforms", platforms);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }


    //LOOK OUT!!!!!!!!!!!!!!!!!4

    /**
     * Endpoint to retrieve all stocks for a specific platform
     * The platform name is sent in the request body instead of URL
     *
     * @param request Contains the platform name
     * @return List of stock symbols and success status
     */
    @PostMapping("/stocks-by-platform")
    public ResponseEntity<Map<String, Object>> getStocksByPlatform(@RequestBody StockByPlatformRequest request) {
        // Call automated trade rule retrieval service
        StocksByPlatformResponse stocksByPlatformResponse = stockService.getStocksByPlatform(request.getPlatform());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.stock.stock.get", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("stocks", stocksByPlatformResponse);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    /**
     * Endpoint to retrieve trading capacity for a specific stock in a portfolio
     * Updates holdings from exchange API and returns current capacity
     *
     * @param request Contains the portfolio ID and stock symbol
     * @return Trading capacity details including current holdings and maximum buy/sell quantities
     */
    @PostMapping("/trading-capacity")
    public ResponseEntity<Map<String, Object>> getTradingCapacity(@RequestBody TradingCapacityRequest request) {
        // Call automated trade rule retrieval service
        TradingCapacityResponse tradingCapacityResponse = tradingCapacityService.getTradingCapacity(
                userContextService.getCurrentUserId().intValue(), request.getPortfolioId(), request.getStockSymbol());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.stock.tradeCapacity.get", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("capacity", tradingCapacityResponse);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    /**
     * Endpoint to execute a market order (buy or sell)
     *
     * @param request Contains portfolio ID, stock symbol, action (BUY/SELL), amount, and quantity type
     * @return Order details and success status
     */
    @PostMapping("/trade")
    public ResponseEntity<Map<String, Object>> executeTrade(@RequestBody TradeRequest request) {
        // Call automated trade rule retrieval service
        TradeExecutionResponse tradeExecutionResponse = tradingService.executeTrade(
                userContextService.getCurrentUserId().intValue(),
                request.getPortfolioId(),
                request.getStockSymbol(),
                request.getAction(),
                request.getAmount(),
                request.getQuantityType());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.stock.trade", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("order", tradeExecutionResponse);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }
}
