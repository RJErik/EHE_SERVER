package ehe_server.service.transaction;

import ehe_server.annotation.LogMessage;
import ehe_server.dto.PaginatedResponse;
import ehe_server.dto.TransactionResponse;
import ehe_server.entity.Transaction;
import ehe_server.repository.TransactionRepository;
import ehe_server.service.intf.transaction.TransactionRetrievalServiceInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TransactionRetrievalService implements TransactionRetrievalServiceInterface {

    private final TransactionRepository transactionRepository;

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