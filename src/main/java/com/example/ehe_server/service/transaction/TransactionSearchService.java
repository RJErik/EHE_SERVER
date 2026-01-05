package com.example.ehe_server.service.transaction;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.PaginatedResponse;
import com.example.ehe_server.dto.TransactionResponse;
import com.example.ehe_server.entity.Transaction;
import com.example.ehe_server.repository.TransactionRepository;
import com.example.ehe_server.service.intf.transaction.TransactionSearchServiceInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TransactionSearchService implements TransactionSearchServiceInterface {

    private final TransactionRepository transactionRepository;

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
            Transaction.TransactionType type,
            Transaction.Status status,
            Integer size,
            Integer page) {

        // Data retrieval
        Pageable pageable = PageRequest.of(page, size);

        Page<Transaction> transactionPage = transactionRepository.searchTransactions(
                userId,
                portfolioId,
                platform,
                symbol,
                type,
                status,
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
                        transaction.getPlatformStock().getStock().getStockSymbol(),
                        transaction.getTransactionType(),
                        transaction.getQuantity(),
                        transaction.getPrice(),
                        transaction.getQuantity().multiply(transaction.getPrice()),
                        transaction.getStatus(),
                        transaction.getTransactionDate()
                ))
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                transactionPage.getNumber(),
                transactionPage.getTotalPages(),
                content
        );
    }
}