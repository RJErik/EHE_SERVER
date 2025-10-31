package com.example.ehe_server.service.intf.transaction;

import com.example.ehe_server.dto.TransactionRetrievalResponse;

import java.util.List;

/**
 * Interface for transaction retrieval operations (Admin)
 */
public interface TransactionRetrievalServiceInterface {
    /**
     * Retrieves all transactions across all users (Admin only)
     * @return List of all transactions
     */
    List<TransactionRetrievalResponse> getAllTransactions();
}