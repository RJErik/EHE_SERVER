package com.example.ehe_server.dto;

import com.example.ehe_server.annotation.validation.NotEmptyString;
import com.example.ehe_server.exception.custom.MissingPlatformNameException;
import com.example.ehe_server.exception.custom.MissingStockSymbolException;

public class WatchlistCreationRequest {

    @NotEmptyString(exception = MissingPlatformNameException.class)
    private String platform;
    @NotEmptyString(exception = MissingStockSymbolException.class)
    private String symbol;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}
