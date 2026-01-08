package ehe_server.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "\"transaction\"")
public class Transaction {

    public enum TransactionType {BUY, SELL}
    public enum Status {PENDING, COMPLETED, FAILED}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Integer transactionId;

    @ManyToOne
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @ManyToOne
    @JoinColumn(name = "platform_stock_id", nullable = false)
    private PlatformStock platformStock;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private TransactionType transactionType;

    @DecimalMin("0.00000001")
    @Digits(integer = 10, fraction = 8)
    @Column(name = "quantity", nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    @DecimalMin("0.00000001")
    @Digits(integer = 10, fraction = 8)
    @Column(name = "price", nullable = false, precision = 18, scale = 8)
    private BigDecimal price;

    @CreationTimestamp
    @Column(name = "transaction_date", nullable = false, updatable = false)
    private LocalDateTime transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private Status status;

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public PlatformStock getPlatformStock() {
        return platformStock;
    }

    public void setPlatformStock(PlatformStock platformStock) {
        this.platformStock = platformStock;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
