package ehe_server.dto;

import ehe_server.annotation.validation.NotEmptyString;
import ehe_server.annotation.validation.NotNullField;
import ehe_server.entity.MarketCandle;
import ehe_server.exception.custom.MissingPlatformNameException;
import ehe_server.exception.custom.MissingStockSymbolException;
import ehe_server.exception.custom.MissingTimeframeException;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class CandlesByDateRequest {

    @NotEmptyString(exception = MissingPlatformNameException.class)
    private String platform;

    @NotEmptyString(exception = MissingStockSymbolException.class)
    private String stockSymbol;

    @NotNullField(exception = MissingTimeframeException.class)
    private MarketCandle.Timeframe timeframe;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime toDate;

    public CandlesByDateRequest() {
    }

    public CandlesByDateRequest(String platform, String stockSymbol, MarketCandle.Timeframe timeframe,
                                LocalDateTime fromDate, LocalDateTime toDate) {
        this.platform = platform;
        this.stockSymbol = stockSymbol;
        this.timeframe = timeframe;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public void setStockSymbol(String stockSymbol) {
        this.stockSymbol = stockSymbol;
    }

    public MarketCandle.Timeframe getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(MarketCandle.Timeframe timeframe) {
        this.timeframe = timeframe;
    }

    public LocalDateTime getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDateTime fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDateTime getToDate() {
        return toDate;
    }

    public void setToDate(LocalDateTime toDate) {
        this.toDate = toDate;
    }
}