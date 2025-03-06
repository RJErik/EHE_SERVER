package com.example.ehe_server.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "platform_stock")
public class PlatformStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "platform_stock_id")
    private Integer platformStockId;

    @Column(name = "platform_name", nullable = false, unique = true, length = 100)
    private String platformName;

    @Column(name = "stock_symbol", nullable = false, unique = true, length = 50)
    private String stockSymbol;

    // Getters and setters
    public Integer getPlatformStockId() {
        return platformStockId;
    }

    public void setPlatformStockId(Integer platformStockId) {
        this.platformStockId = platformStockId;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public void setStockSymbol(String stockSymbol) {
        this.stockSymbol = stockSymbol;
    }
}
