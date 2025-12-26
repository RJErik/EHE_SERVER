package com.example.ehe_server.exception.custom;

public class MissingStockSymbolException extends ValidationException {
    public MissingStockSymbolException() {
        super("error.message.missingStockSymbol", "error.logDetail.missingStockSymbol");
    }
}