package com.example.ehe_server.service.transaction;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.PaginatedResponse;
import com.example.ehe_server.dto.TransactionResponse;
import com.example.ehe_server.entity.Transaction;
import com.example.ehe_server.exception.custom.InvalidPageNumberException;
import com.example.ehe_server.exception.custom.InvalidPageSizeException;
import com.example.ehe_server.exception.custom.MissingPageNumberException;
import com.example.ehe_server.exception.custom.MissingPageSizeException;
import com.example.ehe_server.repository.TransactionRepository;
import com.example.ehe_server.service.intf.transaction.TransactionRetrievalServiceInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TransactionRetrievalService implements TransactionRetrievalServiceInterface {

    private final TransactionRepository transactionRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TransactionRetrievalService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @LogMessage(
            messageKey = "log.message.transaction.get",
            params = {
                    "#size",
                    "#page",
                    "#result.size()"
            }
    )
    @Override
    public PaginatedResponse<TransactionResponse> getAllTransactions(Integer size, Integer page) {
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

        // Retrieve all transactions
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactionPage = transactionRepository.findAllByOrderByTransactionDateDesc(pageable);

        // Transform entities to DTOs
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