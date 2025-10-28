package com.example.ehe_server.controller;

import com.example.ehe_server.dto.HomeBestStockResponse;
import com.example.ehe_server.dto.HomeLatestTransactionsResponse;
import com.example.ehe_server.dto.HomeWorstStockResponse;
import com.example.ehe_server.service.intf.home.HomeBestStockServiceInterface;
import com.example.ehe_server.service.intf.home.HomeLatestTransactionsServiceInterface;
import com.example.ehe_server.service.intf.home.HomeWorstStockServiceInterface;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/home")
public class HomeInfoController {
    private final MessageSource messageSource;
    private final HomeBestStockServiceInterface homeBestStock;
    private final HomeWorstStockServiceInterface homeWorstStock;
    private final HomeLatestTransactionsServiceInterface homeLatestTransactions;

    public HomeInfoController(MessageSource messageSource,
                              HomeBestStockServiceInterface homeBestStock,
                              HomeWorstStockServiceInterface homeWorstStock,
                              HomeLatestTransactionsServiceInterface homeLatestTransactions) {
        this.homeBestStock = homeBestStock;
        this.messageSource = messageSource;
        this.homeWorstStock = homeWorstStock;
        this.homeLatestTransactions = homeLatestTransactions;
    }

    @GetMapping("/best-stock")
    public ResponseEntity<Map<String, Object>> getHomeBestStock() {
        // Call alert retrieval service
        List<HomeBestStockResponse> homeBestStockResponses = homeBestStock.getHomeBestStock();

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.home.bestStock", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("bestStocks", homeBestStockResponses);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/worst-stock")
    public ResponseEntity<Map<String, Object>> getHomeWorstStock() {
        // Call alert retrieval service
        List<HomeWorstStockResponse> homeWorstStockResponses = homeWorstStock.getHomeWorstStock();

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.home.worstStock", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("worstStocks", homeWorstStockResponses);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/latest-transactions")
    public ResponseEntity<Map<String, Object>> getHomeLatestTransactions() {
        // Call alert retrieval service
        List<HomeLatestTransactionsResponse> homeLatestTransactionsResponses = homeLatestTransactions.getLatestTransactions();

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.home.latestTransaction", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("latestTransactions", homeLatestTransactionsResponses);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }
}
