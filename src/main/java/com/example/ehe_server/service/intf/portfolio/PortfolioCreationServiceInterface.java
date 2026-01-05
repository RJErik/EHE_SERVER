package com.example.ehe_server.service.intf.portfolio;

import com.example.ehe_server.dto.PortfolioResponse;


public interface PortfolioCreationServiceInterface {
    /**
     * Creates a new portfolio for the current user
     * @param portfolioName The name of the portfolio
     * @param apiKeyId The API key ID to associate with the portfolio
     * @return Map containing success status and created portfolio details
     */
    PortfolioResponse createPortfolio(Integer userId, String portfolioName, Integer apiKeyId);
}
