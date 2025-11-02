package com.example.ehe_server.dto;

import java.math.BigDecimal;
import java.util.Objects;

public class PortfolioSearchResponse {
    private Integer id;
    private String name;
    private String platform;
    private String creationDate;
    private BigDecimal value;

    public PortfolioSearchResponse() {}

    public PortfolioSearchResponse(Integer id, String name, String platform, String creationDate, BigDecimal value) {
        this.id = id;
        this.name = name;
        this.platform = platform;
        this.creationDate = creationDate;
        this.value = value;
    }

    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getCreationDate() { return creationDate; }
    public void setCreationDate(String creationDate) { this.creationDate = creationDate; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortfolioSearchResponse that = (PortfolioSearchResponse) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(platform, that.platform) &&
                Objects.equals(creationDate, that.creationDate) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, platform, creationDate, value);
    }
}