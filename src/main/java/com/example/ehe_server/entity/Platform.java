package com.example.ehe_server.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "platform")
public class Platform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "platform_id")
    private Integer platformId;

    @Column(name = "platform_name", nullable = false, unique = true, length = 100)
    private String platformName;

    @OneToMany(mappedBy = "platform", fetch = FetchType.LAZY)
    private List<PlatformStock> platformStocks;

    @OneToMany(mappedBy = "platform", fetch = FetchType.LAZY)
    private List<ApiKey> apiKeys;

    // Getters and setters
    public Integer getPlatformId() {
        return platformId;
    }

    public void setPlatformId(Integer platformId) {
        this.platformId = platformId;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public List<PlatformStock> getPlatformStocks() {
        return platformStocks;
    }

    public void setPlatformStocks(List<PlatformStock> platformStocks) {
        this.platformStocks = platformStocks;
    }

    public List<ApiKey> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(List<ApiKey> apiKeys) {
        this.apiKeys = apiKeys;
    }
}