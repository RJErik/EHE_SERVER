package com.example.ehe_server.controller;

import com.example.ehe_server.dto.*;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.watchlist.WatchlistCandleServiceInterface;
import com.example.ehe_server.service.intf.watchlist.WatchlistSearchServiceInterface;
import com.example.ehe_server.service.intf.watchlist.WatchlistRetrievalServiceInterface;
import com.example.ehe_server.service.intf.watchlist.WatchlistCreationServiceInterface;
import com.example.ehe_server.service.intf.watchlist.WatchlistRemovalServiceInterface;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/watchlist-items")
public class WatchlistItemController {

    private final WatchlistRetrievalServiceInterface watchlistRetrievalService;
    private final WatchlistCreationServiceInterface watchlistCreationService;
    private final WatchlistRemovalServiceInterface watchlistRemovalService;
    private final WatchlistSearchServiceInterface watchlistSearchService;
    private final WatchlistCandleServiceInterface watchlistCandleService;
    private final UserContextService userContextService;
    private final MessageSource messageSource;

    public WatchlistItemController(
            WatchlistRetrievalServiceInterface watchlistRetrievalService,
            WatchlistCreationServiceInterface watchlistCreationService,
            WatchlistRemovalServiceInterface watchlistRemovalService,
            WatchlistSearchServiceInterface watchlistSearchService,
            WatchlistCandleServiceInterface watchlistCandleService,
            UserContextService userContextService,
            MessageSource messageSource) {
        this.watchlistRetrievalService = watchlistRetrievalService;
        this.watchlistCreationService = watchlistCreationService;
        this.watchlistRemovalService = watchlistRemovalService;
        this.watchlistSearchService = watchlistSearchService;
        this.watchlistCandleService = watchlistCandleService;
        this.userContextService = userContextService;
        this.messageSource = messageSource;
    }

    /**
     * GET /api/user/watchlist-items
     * Retrieve all watchlist items for the current user
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getWatchlistItems() {
        List<WatchlistResponse> watchlistRetrievalResponses =
                watchlistRetrievalService.getWatchlistItems(userContextService.getCurrentUserId());

        String successMessage = messageSource.getMessage(
                "success.message.watchlistItem.get",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("watchlistItems", watchlistRetrievalResponses);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * POST /api/user/watchlist-items
     * Add a new item to the user's watchlist
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createWatchlistItem(
            @RequestBody WatchlistCreationRequest request) {

        WatchlistResponse watchlistCreationResponse = watchlistCreationService.createWatchlistItem(
                userContextService.getCurrentUserId(),
                request.getPlatform(),
                request.getSymbol());

        String successMessage = messageSource.getMessage(
                "success.message.watchlistItem.add",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("watchlistItem", watchlistCreationResponse);

        // 201 Created for resource creation
        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    /**
     * DELETE /api/user/watchlist-items/{id}
     * Remove an item from the user's watchlist
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> removeWatchlistItem(@PathVariable Integer id) {
        watchlistRemovalService.removeWatchlistItem(userContextService.getCurrentUserId(), id);

        String successMessage = messageSource.getMessage(
                "success.message.watchlistItem.remove",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/user/watchlist-items/search?platform=BINANCE&symbol=BTC
     * Search watchlist items by platform and/or symbol
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchWatchlistItems(
            @ModelAttribute WatchlistSearchRequest request) {

        List<WatchlistResponse> watchlistSearchResponses = watchlistSearchService.searchWatchlistItems(
                userContextService.getCurrentUserId(),
                request.getPlatform(),
                request.getSymbol());

        String successMessage = messageSource.getMessage(
                "success.message.watchlistItem.search",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("watchlistItems", watchlistSearchResponses);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/user/watchlist-items/candles
     * Get latest candle data for all items in the user's watchlist
     */
    @GetMapping("/candles")
    public ResponseEntity<Map<String, Object>> getWatchlistCandles() {
        List<WatchlistCandleResponse> watchlistCandleResponses =
                watchlistCandleService.getLatestCandles(userContextService.getCurrentUserId());

        String successMessage = messageSource.getMessage(
                "success.message.watchlistItem.candles.get",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("candles", watchlistCandleResponses);

        return ResponseEntity.ok(responseBody);
    }
}