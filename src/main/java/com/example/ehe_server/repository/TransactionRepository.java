package com.example.ehe_server.repository;

import com.example.ehe_server.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    List<Transaction> findByPortfolio_PortfolioId(Integer portfolioId);
    List<Transaction> findByPortfolio_User_UserId(Integer userId);
    List<Transaction> findByPlatformStock_StockSymbol(String stockSymbol);

    @Query("SELECT t FROM Transaction t WHERE t.status = 'Completed' ORDER BY t.transactionDate DESC LIMIT 3")
    List<Transaction> findLast3CompletedTransactions();
}