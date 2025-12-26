package com.example.ehe_server.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "stock")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Integer stockId;

    @Column(name = "stock_name", length = 255)
    private String stockName;

    @OneToMany(mappedBy = "stock", fetch = FetchType.LAZY)
    private List<PlatformStock> platformStocks;

    // Getters and setters
    public Integer getStockId() {
        return stockId;
    }

    public void setStockId(Integer stockId) {
        this.stockId = stockId;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public List<PlatformStock> getPlatformStocks() {
        return platformStocks;
    }

    public void setPlatformStocks(List<PlatformStock> platformStocks) {
        this.platformStocks = platformStocks;
    }
}