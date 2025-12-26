package com.example.ehe_server.service.intf.transaction;

import com.example.ehe_server.dto.PaginatedResponse;
import com.example.ehe_server.dto.TransactionResponse;
import com.example.ehe_server.dto.TransactionSearchResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface for transaction search operations (Admin)
 */
public interface TransactionSearchServiceInterface {
    /**
     * Searches transactions with multiple filter criteria (Admin only)
     * @param userId Optional user ID filter
     * @param portfolioId Optional portfolio ID filter
     * @param platform Optional platform filter
     * @param symbol Optional stock symbol filter
     * @param fromTime Optional start date filter
     * @param toTime Optional end date filter
     * @param fromAmount Optional minimum quantity filter
     * @param toAmount Optional maximum quantity filter
     * @param fromPrice Optional minimum price filter
     * @param toPrice Optional maximum price filter
     * @param type Optional transaction type filter
     * @param status Optional status filter
     * @return List of matching transactions
     */
    PaginatedResponse<TransactionResponse> searchTransactions(
            Integer userId,
            Integer portfolioId,  // ADD THIS
            String platform,
            String symbol,
            LocalDateTime fromTime,
            LocalDateTime toTime,
            BigDecimal fromAmount,
            BigDecimal toAmount,
            BigDecimal fromPrice,
            BigDecimal toPrice,
            String type,
            String status,
            Integer size,
            Integer page
    );
}