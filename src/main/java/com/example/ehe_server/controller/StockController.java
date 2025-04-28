package com.example.ehe_server.controller;

import com.example.ehe_server.dto.PlatformRequest;
import com.example.ehe_server.service.intf.audit.UserContextServiceInterface;
import com.example.ehe_server.service.intf.stock.PlatformServiceInterface;
import com.example.ehe_server.service.intf.stock.StockServiceInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class StockController {

    private final PlatformServiceInterface platformService;
    private final StockServiceInterface stockService;
    private final UserContextServiceInterface userContextService;

    public StockController(
            PlatformServiceInterface platformService,
            StockServiceInterface stockService,
            UserContextServiceInterface userContextService) {
        this.platformService = platformService;
        this.stockService = stockService;
        this.userContextService = userContextService;
    }

    /**
     * Endpoint to retrieve all available trading platforms
     *
     * @return List of platform names and success status
     */
    @GetMapping("/platforms")
    public ResponseEntity<Map<String, Object>> getPlatforms() {
        // Setup the user context from Spring Security
        userContextService.setupUserContext();

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
        // Setup the user context from Spring Security
        userContextService.setupUserContext();

        // Call stock service with the platform from request body
        Map<String, Object> responseBody = stockService.getStocksByPlatform(request.getPlatform());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }
}
