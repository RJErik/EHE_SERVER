package com.example.ehe_server.controller;

import com.example.ehe_server.dto.WatchlistDeleteRequest;
import com.example.ehe_server.dto.WatchlistItemRequest;
import com.example.ehe_server.dto.WatchlistSearchRequest;
import com.example.ehe_server.service.intf.watchlist.WatchlistCandleServiceInterface;
import com.example.ehe_server.service.intf.watchlist.WatchlistSearchServiceInterface;
import com.example.ehe_server.service.intf.watchlist.WatchlistServiceInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class WatchlistController {

    private final WatchlistServiceInterface watchlistService;
    private final WatchlistSearchServiceInterface watchlistSearchService;
    private final WatchlistCandleServiceInterface watchlistCandleService;

    public WatchlistController(
            WatchlistServiceInterface watchlistService,
            WatchlistSearchServiceInterface watchlistSearchService,
            WatchlistCandleServiceInterface watchlistCandleService) {
        this.watchlistService = watchlistService;
        this.watchlistSearchService = watchlistSearchService;
        this.watchlistCandleService = watchlistCandleService;
    }

    /**
     * Endpoint to retrieve all watchlist items for the current user
     *
     * @return List of watchlist items and success status
     */
    @GetMapping("/watchlist")
    public ResponseEntity<Map<String, Object>> getWatchlistItems() {
        // Call watchlist service
        Map<String, Object> responseBody = watchlistService.getWatchlistItems();

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    /**
     * Endpoint to add a new item to the user's watchlist
     *
     * @param request Contains platform name and stock symbol
     * @return Success status and details of added item
     */
    @PostMapping("/watchlist/add")
    public ResponseEntity<Map<String, Object>> addWatchlistItem(@RequestBody WatchlistItemRequest request) {
        // Call watchlist service to add item
        Map<String, Object> responseBody = watchlistService.addWatchlistItem(
                request.getPlatform(), request.getSymbol());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    /**
     * Endpoint to remove an item from the user's watchlist
     *
     * @param request Contains the watchlist item ID to delete
     * @return Success status and confirmation message
     */
    @DeleteMapping("/watchlist/remove")
    public ResponseEntity<Map<String, Object>> removeWatchlistItem(@RequestBody WatchlistDeleteRequest request) {
        // Call watchlist service to remove item
        Map<String, Object> responseBody = watchlistService.removeWatchlistItem(request.getId());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    /**
     * Endpoint to search watchlist items by platform and/or symbol
     *
     * @param request Contains optional platform name and/or stock symbol for filtering
     * @return Filtered list of watchlist items and success status
     */
    @PostMapping("/watchlist/search")
    public ResponseEntity<Map<String, Object>> searchWatchlist(@RequestBody WatchlistSearchRequest request) {
        // Call watchlist search service
        Map<String, Object> responseBody = watchlistSearchService.searchWatchlistItems(
                request.getPlatform(), request.getSymbol());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    /**
     * Endpoint to get latest candle data for all items in the user's watchlist
     *
     * @return List of latest candles for watchlist items and success status
     */
    @GetMapping("/watchlist/candles")
    public ResponseEntity<Map<String, Object>> getWatchlistCandles() {
        // Call watchlist candle service
        Map<String, Object> responseBody = watchlistCandleService.getLatestCandles();

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }
}
