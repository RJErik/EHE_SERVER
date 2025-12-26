package com.example.ehe_server.exception.custom;

public class MissingPortfolioNameException extends ValidationException {
    public MissingPortfolioNameException() {
        super("error.message.missingPortfolioName", "error.logDetail.missingPortfolioName");
    }
}