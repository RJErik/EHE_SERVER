package ehe_server.dto;

import ehe_server.annotation.validation.NotEmptyString;
import ehe_server.annotation.validation.NotNullField;
import ehe_server.entity.MarketCandle;
import com.example.ehe_server.exception.custom.*;
import ehe_server.exception.custom.MissingEndSequenceNumberException;
import ehe_server.exception.custom.MissingPlatformNameException;
import ehe_server.exception.custom.MissingStartSequenceNumberException;
import ehe_server.exception.custom.MissingStockSymbolException;

public class CandlesBySequenceRequest {

    @NotEmptyString(exception = MissingPlatformNameException.class)
    private String platform;

    @NotEmptyString(exception = MissingStockSymbolException.class)
    private String stockSymbol;

//    @NotNullField(exception = MissingTimeframeException.class)
    private MarketCandle.Timeframe timeframe;

    @NotNullField(exception = MissingStartSequenceNumberException.class)
    private Long fromSequence;

    @NotNullField(exception = MissingEndSequenceNumberException.class)
    private Long toSequence;

    public CandlesBySequenceRequest() {}

    public CandlesBySequenceRequest(String platform, String stockSymbol, MarketCandle.Timeframe timeframe,
                                    Long fromSequence, Long toSequence) {
        this.platform = platform;
        this.stockSymbol = stockSymbol;
        this.timeframe = timeframe;
        this.fromSequence = fromSequence;
        this.toSequence = toSequence;
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

    public Long getFromSequence() {
        return fromSequence;
    }

    public void setFromSequence(Long fromSequence) {
        this.fromSequence = fromSequence;
    }

    public Long getToSequence() {
        return toSequence;
    }

    public void setToSequence(Long toSequence) {
        this.toSequence = toSequence;
    }
}