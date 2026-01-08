package ehe_server.service.intf.transaction;

import ehe_server.dto.PaginatedResponse;
import ehe_server.dto.TransactionResponse;

public interface TransactionRetrievalServiceInterface {
    PaginatedResponse<TransactionResponse> getAllTransactions(Integer size, Integer page);
}