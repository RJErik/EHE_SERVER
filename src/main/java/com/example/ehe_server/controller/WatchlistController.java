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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class WatchlistController {

    private final WatchlistRetrievalServiceInterface watchlistRetrievalService;
    private final WatchlistCreationServiceInterface watchlistCreationService;
    private final WatchlistRemovalServiceInterface watchlistRemovalService;
    private final WatchlistSearchServiceInterface watchlistSearchService;
    private final WatchlistCandleServiceInterface watchlistCandleService;
    private final UserContextService userContextService;
    private final MessageSource messageSource;

    public WatchlistController(
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

    //LOOK OUT NAME!!!!!!!!!!!!!!!!!

    /**
     * Endpoint to retrieve all watchlist items for the current user
     *
     * @return List of watchlist items and success status
     */
    @GetMapping("/watchlists")
    public ResponseEntity<Map<String, Object>> getWatchlists() {
        // Call automated trade rule retrieval service
        List<WatchlistRetrievalResponse> watchlistRetrievalResponses = watchlistRetrievalService.getWatchlistItems(userContextService.getCurrentUserId());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.watchlist.get", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("watchlists", watchlistRetrievalResponses);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    //LOOK OUT NAME!

    /**
     * Endpoint to add a new item to the user's watchlist
     *
     * @param request Contains platform name and stock symbol
     * @return Success status and details of added item
     */
    @PostMapping("/watchlists")
    public ResponseEntity<Map<String, Object>> createWatchlist(@RequestBody WatchlistCreationRequest request) {
        // Call automated trade rule retrieval service
        WatchlistCreationResponse watchlistCreationResponse = watchlistCreationService.createWatchlistItem(
                userContextService.getCurrentUserId(), request.getPlatform(), request.getSymbol());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.watchlist.add", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("watchlist", watchlistCreationResponse);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    /**
     * Endpoint to remove an item from the user's watchlist
     *
     * @param request Contains the watchlist item ID to delete
     * @return Success status and confirmation message
     */
    @DeleteMapping("/watchlists")
    public ResponseEntity<Map<String, Object>> removeWatchlistItem(@RequestBody WatchlistRemovalRequest request) {
        // Call automated trade rule retrieval service
        watchlistRemovalService.removeWatchlistItem(userContextService.getCurrentUserId(), request.getId());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.watchlist.remove", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    //LOOK OUT NAME!!!!!!!!!!!!!!!!!!!!!

    /**
     * Endpoint to search watchlist items by platform and/or symbol
     *
     * @param request Contains optional platform name and/or stock symbol for filtering
     * @return Filtered list of watchlist items and success status
     */
    @PostMapping("/watchlists/search")
    public ResponseEntity<Map<String, Object>> searchWatchlists(@RequestBody WatchlistSearchRequest request) {
        // Call automated trade rule retrieval service
        List<WatchlistSearchResponse> watchlistSearchResponses = watchlistSearchService.searchWatchlistItems(
                userContextService.getCurrentUserId(), request.getPlatform(), request.getSymbol());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.watchlist.search", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("watchlists", watchlistSearchResponses);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    /**
     * Endpoint to get latest candle data for all items in the user's watchlist
     *
     * @return List of latest candles for watchlist items and success status
     */
    @GetMapping("/watchlists/candles")
    public ResponseEntity<Map<String, Object>> getWatchlistCandles() {
        // Call automated trade rule retrieval service
        List<WatchlistCandleResponse> watchlistCandleResponses = watchlistCandleService.getLatestCandles(userContextService.getCurrentUserId());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.watchlist.candles.get", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("candles", watchlistCandleResponses);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }
}