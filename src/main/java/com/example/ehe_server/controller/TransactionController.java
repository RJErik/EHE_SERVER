package com.example.ehe_server.controller;

import com.example.ehe_server.dto.*;
import com.example.ehe_server.service.intf.transaction.TransactionRetrievalServiceInterface;
import com.example.ehe_server.service.intf.transaction.TransactionSearchServiceInterface;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/transactions")
public class TransactionController {

    private final TransactionRetrievalServiceInterface transactionRetrievalService;
    private final TransactionSearchServiceInterface transactionSearchService;
    private final MessageSource messageSource;

    public TransactionController(
            TransactionRetrievalServiceInterface transactionRetrievalService,
            TransactionSearchServiceInterface transactionSearchService,
            MessageSource messageSource) {
        this.transactionRetrievalService = transactionRetrievalService;
        this.transactionSearchService = transactionSearchService;
        this.messageSource = messageSource;
    }

    /**
     * GET /api/admin/transactions?size=20&page=0
     *
     * Retrieve all transactions with pagination via query parameters.
     *
     * Example: GET /api/admin/transactions?size=20&page=0
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTransactions(
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "0") Integer page) {

        PaginatedResponse<TransactionResponse> transactionResponses =
                transactionRetrievalService.getAllTransactions(size, page);

        String successMessage = messageSource.getMessage(
                "success.message.transaction.get",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("transactions", transactionResponses);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/admin/transactions/search?userId=1&platform=BINANCE&symbol=BTC...
     *
     * Search transactions with filters via query parameters.
     * Spring automatically binds query params to the DTO using @ModelAttribute.
     *
     * Example: GET /api/admin/transactions/search?platform=BINANCE&symbol=BTC&size=20&page=0
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchTransactions(
            @ModelAttribute TransactionSearchRequest request) {

        PaginatedResponse<TransactionResponse> transactionResponses =
                transactionSearchService.searchTransactions(
                        request.getUserId(),
                        request.getPortfolioId(),
                        request.getPlatform(),
                        request.getSymbol(),
                        request.getFromTime(),
                        request.getToTime(),
                        request.getFromAmount(),
                        request.getToAmount(),
                        request.getFromPrice(),
                        request.getToPrice(),
                        request.getType(),
                        request.getStatus(),
                        request.getSize(),
                        request.getPage()
                );

        String successMessage = messageSource.getMessage(
                "success.message.transaction.search",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("transactions", transactionResponses);

        return ResponseEntity.ok(responseBody);
    }
}