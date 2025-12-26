package com.example.ehe_server.service.intf.transaction;

import com.example.ehe_server.dto.PaginatedResponse;
import com.example.ehe_server.dto.TransactionResponse;

/**
 * Interface for transaction retrieval operations (Admin)
 */
public interface TransactionRetrievalServiceInterface {
    /**
     * Retrieves all transactions across all users (Admin only)
     * @return List of all transactions
     */
    PaginatedResponse<TransactionResponse> getAllTransactions(Integer size, Integer page);
}