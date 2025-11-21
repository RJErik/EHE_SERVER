package com.example.ehe_server.service.transaction;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.TransactionRetrievalResponse;
import com.example.ehe_server.entity.Transaction;
import com.example.ehe_server.repository.TransactionRepository;
import com.example.ehe_server.service.intf.transaction.TransactionRetrievalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionRetrievalService implements TransactionRetrievalServiceInterface {

    private final TransactionRepository transactionRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TransactionRetrievalService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @LogMessage(
            messageKey = "log.message.transaction.get",
            params = {"#result.size()"}
    )
    @Override
    public List<TransactionRetrievalResponse> getAllTransactions() {
        // Retrieve all transactions
        List<Transaction> transactions = transactionRepository.findAll();

        // Transform entities to DTOs
        return transactions.stream()
                .map(transaction -> new TransactionRetrievalResponse(
                        transaction.getTransactionId(),
                        transaction.getPortfolio().getUser().getUserId(),
                        transaction.getPortfolio().getPortfolioId(),
                        transaction.getPlatformStock().getPlatformName(),
                        transaction.getPlatformStock().getStockSymbol(),
                        transaction.getTransactionType().toString(),
                        transaction.getQuantity(),
                        transaction.getPrice(),
                        transaction.getQuantity().multiply(transaction.getPrice()),
                        transaction.getStatus().toString(),
                        transaction.getTransactionDate().format(DATE_FORMATTER)
                ))
                .collect(Collectors.toList());
    }
}