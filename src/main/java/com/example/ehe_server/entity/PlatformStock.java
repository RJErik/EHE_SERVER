package com.example.ehe_server.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "platform_stock", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"platform_id", "stock_id"})
})
public class PlatformStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "platform_stock_id")
    private Integer platformStockId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id", nullable = false)
    private Platform platform;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    // Getters and setters
    public Integer getPlatformStockId() {
        return platformStockId;
    }

    public void setPlatformStockId(Integer platformStockId) {
        this.platformStockId = platformStockId;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public Integer getPlatformId() {
        return platform != null ? platform.getPlatformId() : null;
    }

    public Integer getStockId() {
        return stock != null ? stock.getStockId() : null;
    }
}