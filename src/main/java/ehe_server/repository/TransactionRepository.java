package ehe_server.repository;

import ehe_server.entity.Transaction;
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
    @EntityGraph(attributePaths = {"portfolio", "portfolio.user", "platformStock", "platformStock.platform", "platformStock.stock"})
    List<Transaction> findByPortfolio_PortfolioId(Integer portfolioId);

    @EntityGraph(attributePaths = {"platformStock", "platformStock.platform", "platformStock.stock"})
    List<Transaction> findTop3ByStatusOrderByTransactionDateDesc(Transaction.Status status);

    @EntityGraph(attributePaths = {"portfolio", "portfolio.user", "platformStock", "platformStock.platform", "platformStock.stock"})
    Page<Transaction> findAllByOrderByTransactionDateDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"portfolio", "portfolio.user", "platformStock", "platformStock.platform", "platformStock.stock"})
    @Query("SELECT t FROM Transaction t WHERE " +
            "(:#{#userId == null} = true OR t.portfolio.user.userId = :userId) AND " +
            "(:#{#portfolioId == null} = true OR t.portfolio.portfolioId = :portfolioId) AND " +
            "(:#{#platform == null} = true OR t.platformStock.platform.platformName = :platform) AND " +
            "(:#{#symbol == null} = true OR t.platformStock.stock.stockSymbol = :symbol) AND " +
            "(:#{#type == null} = true OR t.transactionType = :type) AND " +
            "(:#{#status == null} = true OR t.status = :status) AND " +
            "(:#{#fromTime == null} = true OR t.transactionDate >= :fromTime) AND " +
            "(:#{#toTime == null} = true OR t.transactionDate <= :toTime) AND " +
            "(:#{#fromQuantity == null} = true OR t.quantity >= :fromQuantity) AND " +
            "(:#{#toQuantity == null} = true OR t.quantity <= :toQuantity) AND " +
            "(:#{#fromPrice == null} = true OR t.price >= :fromPrice) AND " +
            "(:#{#toPrice == null} = true OR t.price <= :toPrice) " +
            "ORDER BY t.transactionDate DESC")
    Page<Transaction> searchTransactions(
            @Param("userId") Integer userId,
            @Param("portfolioId") Integer portfolioId,
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