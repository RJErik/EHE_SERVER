package com.example.ehe_server.service.transaction;

import com.example.ehe_server.dto.TransactionSearchResponse;
import com.example.ehe_server.entity.Transaction;
import com.example.ehe_server.repository.TransactionRepository;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.transaction.TransactionSearchServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionSearchService implements TransactionSearchServiceInterface {

    private final TransactionRepository transactionRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TransactionSearchService(
            TransactionRepository transactionRepository,
            LoggingServiceInterface loggingService) {
        this.transactionRepository = transactionRepository;
        this.loggingService = loggingService;
    }

    @Override
    public List<TransactionSearchResponse> searchTransactions(
            Integer userId,
            String platform,
            String symbol,
            LocalDateTime fromTime,
            LocalDateTime toTime,
            BigDecimal fromAmount,
            BigDecimal toAmount,
            BigDecimal fromPrice,
            BigDecimal toPrice,
            String type,
            String status) {

        // Convert string enum values to actual enums
        Transaction.TransactionType transactionType = null;
        if (type != null && !type.trim().isEmpty()) {
            try {
                transactionType = Transaction.TransactionType.valueOf(type);
            } catch (IllegalArgumentException e) {
                loggingService.logAction("Invalid transaction type: " + type);
                return List.of();
            }
        }

        Transaction.Status transactionStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                transactionStatus = Transaction.Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                loggingService.logAction("Invalid transaction status: " + status);
                return List.of();
            }
        }

        // Perform search
        List<Transaction> transactions = transactionRepository.searchTransactions(
                userId,
                platform,
                symbol,
                transactionType,
                transactionStatus,
                fromTime,
                toTime,
                fromAmount,
                toAmount,
                fromPrice,
                toPrice
        );

        // Transform entities to DTOs
        List<TransactionSearchResponse> responses = transactions.stream()
                .map(transaction -> new TransactionSearchResponse(
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

        // Log success
        loggingService.logAction("Transaction search completed, found " + responses.size() + " results");

        return responses;
    }
}