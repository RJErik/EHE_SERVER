package ehe_server.dto;

import ehe_server.entity.Alert;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class AlertResponse {
    private Integer id;
    private String platform;
    private String symbol;
    private Alert.ConditionType conditionType;
    private BigDecimal thresholdValue;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateCreated;

    public AlertResponse() {}

    public AlertResponse(Integer id, String platform, String symbol, Alert.ConditionType conditionType, BigDecimal thresholdValue, LocalDateTime dateCreated) {
        this.id = id;
        this.platform = platform;
        this.symbol = symbol;
        this.conditionType = conditionType;
        this.thresholdValue = thresholdValue;
        this.dateCreated = dateCreated;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public Alert.ConditionType getConditionType() { return conditionType; }
    public void setConditionType(Alert.ConditionType conditionType) { this.conditionType = conditionType; }

    public BigDecimal getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(BigDecimal thresholdValue) { this.thresholdValue = thresholdValue; }

    public LocalDateTime getDateCreated() { return dateCreated; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlertResponse that = (AlertResponse) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(platform, that.platform) &&
                Objects.equals(symbol, that.symbol) &&
                Objects.equals(conditionType, that.conditionType) &&
                Objects.equals(thresholdValue, that.thresholdValue) &&
                Objects.equals(dateCreated, that.dateCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, platform, symbol, conditionType, thresholdValue, dateCreated);
    }
}