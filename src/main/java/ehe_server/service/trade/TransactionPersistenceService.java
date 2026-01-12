package ehe_server.service.trade;

import ehe_server.entity.PlatformStock;
import ehe_server.entity.Portfolio;
import ehe_server.entity.Transaction;
import ehe_server.repository.TransactionRepository;
import ehe_server.service.intf.trade.TransactionPersistenceServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TransactionPersistenceService implements TransactionPersistenceServiceInterface {

    private static final int DECIMAL_SCALE = 8;

    private final TransactionRepository transactionRepository;

    public TransactionPersistenceService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction saveTransaction(
            Portfolio portfolio,
            PlatformStock platformStock,
            Transaction.TransactionType action,
            BigDecimal quantity,
            BigDecimal price,
            Transaction.Status status) {

        Transaction transaction = new Transaction();
        transaction.setPortfolio(portfolio);
        transaction.setPlatformStock(platformStock);
        transaction.setTransactionType(action);
        transaction.setQuantity(normalizeDecimal(quantity));
        transaction.setPrice(normalizeDecimal(price));
        transaction.setStatus(status);

        return transactionRepository.save(transaction);
    }

    private BigDecimal normalizeDecimal(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        return value.setScale(DECIMAL_SCALE, RoundingMode.DOWN);
    }
}