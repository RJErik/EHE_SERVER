package com.example.ehe_server.exception.custom;

public class BinanceApiCommunicationException extends BusinessRuleException {
    public BinanceApiCommunicationException() {
        super("error.message.binanceApiCommunication", "error.logDetail.binanceApiCommunication");
    }
}