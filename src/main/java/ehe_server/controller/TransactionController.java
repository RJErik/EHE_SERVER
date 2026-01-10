package ehe_server.controller;

import ehe_server.annotation.validation.MinValue;
import ehe_server.annotation.validation.NotNullField;
import ehe_server.dto.PaginatedResponse;
import ehe_server.dto.TransactionResponse;
import ehe_server.dto.TransactionSearchRequest;
import ehe_server.exception.custom.InvalidPageNumberException;
import ehe_server.exception.custom.InvalidPageSizeException;
import ehe_server.exception.custom.MissingPageNumberException;
import ehe_server.exception.custom.MissingPageSizeException;
import ehe_server.service.intf.transaction.TransactionRetrievalServiceInterface;
import ehe_server.service.intf.transaction.TransactionSearchServiceInterface;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/transactions")
@Validated
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
     * Retrieve all transactions with pagination via query parameters.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTransactions(
            @NotNullField(exception = MissingPageSizeException.class)
            @MinValue(exception = InvalidPageSizeException.class,
                    min = 1)
            @RequestParam()
            Integer size,
            @NotNullField(exception = MissingPageNumberException.class)
            @MinValue(exception = InvalidPageNumberException.class,
                    min = 0)
            @RequestParam() Integer page) {

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
     * Search transactions with filters via query parameters.
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchTransactions(
            @Valid @ModelAttribute TransactionSearchRequest request) {

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