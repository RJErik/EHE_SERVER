package ehe_server.service.intf.trade;

import ehe_server.entity.PlatformStock;
import ehe_server.entity.Portfolio;
import ehe_server.entity.Transaction;

import java.math.BigDecimal;

public interface TransactionPersistenceServiceInterface {

    Transaction saveTransaction(
            Portfolio portfolio,
            PlatformStock platformStock,
            Transaction.TransactionType action,
            BigDecimal quantity,
            BigDecimal price,
            Transaction.Status status
    );
}