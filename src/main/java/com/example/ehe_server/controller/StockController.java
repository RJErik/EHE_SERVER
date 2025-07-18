package com.example.ehe_server.controller;

import com.example.ehe_server.dto.PlatformRequest;
import com.example.ehe_server.dto.TradeRequest;
import com.example.ehe_server.dto.TradingCapacityRequest;
import com.example.ehe_server.service.intf.stock.PlatformServiceInterface;
import com.example.ehe_server.service.intf.stock.StockServiceInterface;
import com.example.ehe_server.service.intf.trade.TradingCapacityServiceInterface;
import com.example.ehe_server.service.intf.trade.TradingServiceInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class StockController {

    private final PlatformServiceInterface platformService;
    private final StockServiceInterface stockService;
    private final TradingCapacityServiceInterface tradingCapacityService;
    private final TradingServiceInterface tradingService;

    public StockController(
            PlatformServiceInterface platformService,
            StockServiceInterface stockService,
            TradingCapacityServiceInterface tradingCapacityService,
            TradingServiceInterface tradingService) {
        this.platformService = platformService;
        this.stockService = stockService;
        this.tradingCapacityService = tradingCapacityService;
        this.tradingService = tradingService;
    }

    /**
     * Endpoint to retrieve all available trading platforms
     *
     * @return List of platform names and success status
     */
    @GetMapping("/platforms")
    public ResponseEntity<Map<String, Object>> getPlatforms() {
        // Call platform service
        Map<String, Object> responseBody = platformService.getAllPlatforms();

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    /**
     * Endpoint to retrieve all stocks for a specific platform
     * The platform name is sent in the request body instead of URL
     *
     * @param request Contains the platform name
     * @return List of stock symbols and success status
     */
    @PostMapping("/stocks")
    public ResponseEntity<Map<String, Object>> getStocksByPlatform(@RequestBody PlatformRequest request) {
        // Call stock service with the platform from request body
        Map<String, Object> responseBody = stockService.getStocksByPlatform(request.getPlatform());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
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
        // Call the trading capacity service
        Map<String, Object> responseBody = tradingCapacityService.getTradingCapacity(
                request.getPortfolioId(), request.getStockSymbol());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    /**
     * Endpoint to execute a market order (buy or sell)
     *
     * @param request Contains portfolio ID, stock symbol, action (BUY/SELL), amount, and quantity type
     * @return Order details and success status
     */
    @PostMapping("/trade")
    public ResponseEntity<Map<String, Object>> executeMarketOrder(@RequestBody TradeRequest request) {
        // Call the trading service
        Map<String, Object> responseBody = tradingService.executeMarketOrder(
                request.getPortfolioId(),
                request.getStockSymbol(),
                request.getAction(),
                request.getAmount(),
                request.getQuantityType(),
                null);

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }
}
