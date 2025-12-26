package com.example.ehe_server.exception.custom;

public class MissingPortfolioIdException extends ValidationException {
    public MissingPortfolioIdException() {
        super("error.message.missingPortfolioId", "error.logDetail.missingPortfolioId");
    }
}