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

    @Column(name = "stock_symbol", length = 255)
    private String stockSymbol;

    @OneToMany(mappedBy = "stock", fetch = FetchType.LAZY)
    private List<PlatformStock> platformStocks;

    public Integer getStockId() {
        return stockId;
    }

    public void setStockId(Integer stockId) {
        this.stockId = stockId;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public void setStockSymbol(String stockSymbol) {
        this.stockSymbol = stockSymbol;
    }

    public List<PlatformStock> getPlatformStocks() {
        return platformStocks;
    }

    public void setPlatformStocks(List<PlatformStock> platformStocks) {
        this.platformStocks = platformStocks;
    }
}