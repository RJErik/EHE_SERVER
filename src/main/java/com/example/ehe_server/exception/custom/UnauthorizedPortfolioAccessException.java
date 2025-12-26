package com.example.ehe_server.exception.custom;

public class UnauthorizedPortfolioAccessException extends ResourceNotFoundException {
    public UnauthorizedPortfolioAccessException(Integer userId, Integer portfolioId) {
        super("error.message.portfolioNotFound", "error.logDetail.unauthorizedPortfolioAccess", userId, portfolioId);
    }
}