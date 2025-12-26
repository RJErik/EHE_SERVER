package com.example.ehe_server.service.transaction;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.PaginatedResponse;
import com.example.ehe_server.dto.TransactionResponse;
import com.example.ehe_server.entity.Transaction;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.TransactionRepository;
import com.example.ehe_server.service.intf.transaction.TransactionSearchServiceInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TransactionSearchService implements TransactionSearchServiceInterface {

    private final TransactionRepository transactionRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TransactionSearchService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @LogMessage(
            messageKey = "log.message.transaction.search",
            params = {
                    "#userId",
                    "#portfolioId",
                    "#platform",
                    "#symbol",
                    "#fromTime",
                    "#toTime",
                    "#fromAmount",
                    "#toAmount",
                    "#fromPrice",
                    "#toPrice",
                    "#type",
                    "#status",
                    "#size",
                    "#page",
                    "#result.size()"
            }
    )
    @Override
    public PaginatedResponse<TransactionResponse> searchTransactions(
            Integer userId,
            Integer portfolioId,
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
            Integer page) {

        // Input Validation
        if (page == null) {
            throw new MissingPageNumberException();
        }

        if (size == null) {
            throw new MissingPageSizeException();
        }

        if (page < 0) {
            throw new InvalidPageNumberException(page);
        }

        if (size < 1) {
            throw new InvalidPageSizeException(size);
        }

        // Parsing logic
        Transaction.TransactionType transactionType = null;
        if (type != null && !type.trim().isEmpty()) {
            try {
                transactionType = Transaction.TransactionType.valueOf(type);
            } catch (IllegalArgumentException e) {
                throw new InvalidTransactionTypeException(type);
            }
        }

        Transaction.Status transactionStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                transactionStatus = Transaction.Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new InvalidTransactionStatusException(status);
            }
        }

        // Data retrieval
        Pageable pageable = PageRequest.of(page, size);

        Page<Transaction> transactionPage = transactionRepository.searchTransactions(
                userId,
                portfolioId,
                platform,
                symbol,
                transactionType,
                transactionStatus,
                fromTime,
                toTime,
                fromAmount,
                toAmount,
                fromPrice,
                toPrice,
                pageable
        );

        // Response mapping
        List<TransactionResponse> content = transactionPage.getContent().stream()
                .map(transaction -> new TransactionResponse(
                        transaction.getTransactionId(),
                        transaction.getPortfolio().getUser().getUserId(),
                        transaction.getPortfolio().getPortfolioId(),
                        transaction.getPlatformStock().getPlatform().getPlatformName(),
                        transaction.getPlatformStock().getStock().getStockName(),
                        transaction.getTransactionType().toString(),
                        transaction.getQuantity(),
                        transaction.getPrice(),
                        transaction.getQuantity().multiply(transaction.getPrice()),
                        transaction.getStatus().toString(),
                        transaction.getTransactionDate().format(DATE_FORMATTER)
                ))
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                transactionPage.getNumber(),
                transactionPage.getTotalPages(),
                content
        );
    }
}