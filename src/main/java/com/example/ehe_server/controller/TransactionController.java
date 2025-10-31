package com.example.ehe_server.controller;

import com.example.ehe_server.dto.*;
import com.example.ehe_server.service.intf.transaction.TransactionRetrievalServiceInterface;
import com.example.ehe_server.service.intf.transaction.TransactionSearchServiceInterface;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class TransactionController {

    private final TransactionRetrievalServiceInterface transactionRetrievalService;
    private final TransactionSearchServiceInterface transactionSearchService;
    private final MessageSource messageSource;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public TransactionController(
            TransactionRetrievalServiceInterface transactionRetrievalService,
            TransactionSearchServiceInterface transactionSearchService,
            MessageSource messageSource) {
        this.transactionRetrievalService = transactionRetrievalService;
        this.transactionSearchService = transactionSearchService;
        this.messageSource = messageSource;
    }

    /**
     * Endpoint to retrieve all transactions across all users (Admin only)
     *
     * @return List of all transactions and success status
     */
    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> getAllTransactions() {
        // Call transaction retrieval service
        List<TransactionRetrievalResponse> transactionResponses = transactionRetrievalService.getAllTransactions();

        // Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.transaction.get",
                null,
                LocaleContextHolder.getLocale()
        );

        // Build the final response body
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("transactions", transactionResponses);

        // Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    /**
     * Endpoint to search transactions with multiple filter criteria (Admin only)
     *
     * @param request Contains optional filters for userId, platform, symbol, dates, amounts, prices, type, and status
     * @return Filtered list of transactions and success status
     */
    @PostMapping("/transactions/search")
    public ResponseEntity<Map<String, Object>> searchTransactions(@RequestBody TransactionSearchRequest request) {
        // Parse date strings to LocalDateTime
        LocalDateTime fromTime = parseLocalDateTime(request.getFromTime());
        LocalDateTime toTime = parseLocalDateTime(request.getToTime());

        // Call transaction search service with extracted parameters
        List<TransactionSearchResponse> transactionResponses = transactionSearchService.searchTransactions(
                request.getUserId(),
                request.getPlatform(),
                request.getSymbol(),
                fromTime,
                toTime,
                request.getFromAmount(),
                request.getToAmount(),
                request.getFromPrice(),
                request.getToPrice(),
                request.getType(),
                request.getStatus()
        );

        // Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.transaction.search",
                null,
                LocaleContextHolder.getLocale()
        );

        // Build the final response body
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("transactions", transactionResponses);

        // Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    /**
     * Parse ISO format LocalDateTime string
     */
    private LocalDateTime parseLocalDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeString, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
}