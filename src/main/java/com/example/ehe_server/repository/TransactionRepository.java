package com.example.ehe_server.repository;

import com.example.ehe_server.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    @EntityGraph(attributePaths = {"portfolio", "portfolio.user", "platformStock"})
    List<Transaction> findByPortfolio_PortfolioId(Integer portfolioId);

    @EntityGraph(attributePaths = {"platformStock"})
    List<Transaction> findTop3ByStatusOrderByTransactionDateDesc(Transaction.Status status);

    @EntityGraph(attributePaths = {"portfolio", "portfolio.user", "platformStock"})
    Page<Transaction> findAllByOrderByTransactionDateDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"portfolio", "portfolio.user", "platformStock"})
    @Query("SELECT t FROM Transaction t WHERE " +
            "(:userId IS NULL OR t.portfolio.user.userId = :userId) AND " +
            "(:portfolioId IS NULL OR t.portfolio.portfolioId = :portfolioId) AND " +
            "(:platform IS NULL OR t.platformStock.platformName = :platform) AND " +
            "(:symbol IS NULL OR t.platformStock.stockSymbol = :symbol) AND " +
            "(:type IS NULL OR t.transactionType = :type) AND " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:fromTime IS NULL OR t.transactionDate >= :fromTime) AND " +
            "(:toTime IS NULL OR t.transactionDate <= :toTime) AND " +
            "(:fromQuantity IS NULL OR t.quantity >= :fromQuantity) AND " +
            "(:toQuantity IS NULL OR t.quantity <= :toQuantity) AND " +
            "(:fromPrice IS NULL OR t.price >= :fromPrice) AND " +
            "(:toPrice IS NULL OR t.price <= :toPrice) " +
            "ORDER BY t.transactionDate DESC")
    Page<Transaction> searchTransactions(
            @Param("userId") Integer userId,
            @Param("portfolioId") Integer portfolioId,  // ADD THIS
            @Param("platform") String platform,
            @Param("symbol") String symbol,
            @Param("type") Transaction.TransactionType type,
            @Param("status") Transaction.Status status,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("fromQuantity") BigDecimal fromQuantity,
            @Param("toQuantity") BigDecimal toQuantity,
            @Param("fromPrice") BigDecimal fromPrice,
            @Param("toPrice") BigDecimal toPrice,
            Pageable pageable
    );
}