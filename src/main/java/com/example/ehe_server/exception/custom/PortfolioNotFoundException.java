package com.example.ehe_server.exception.custom;

public class PortfolioNotFoundException extends ResourceNotFoundException {
    public PortfolioNotFoundException(Integer portfolioId, Integer userId) {
        super("error.message.portfolioNotFound", "error.logDetail.portfolioNotFound", portfolioId, userId);
    }
}