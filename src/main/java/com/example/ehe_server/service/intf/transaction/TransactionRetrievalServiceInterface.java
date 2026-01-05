package com.example.ehe_server.service.intf.transaction;

import com.example.ehe_server.dto.PaginatedResponse;
import com.example.ehe_server.dto.TransactionResponse;

public interface TransactionRetrievalServiceInterface {
    PaginatedResponse<TransactionResponse> getAllTransactions(Integer size, Integer page);
}