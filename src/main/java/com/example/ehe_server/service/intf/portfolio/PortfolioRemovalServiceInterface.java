package com.example.ehe_server.service.intf.portfolio;

public interface PortfolioRemovalServiceInterface {
    /**
     * Deletes a portfolio and all its associated holdings
     * @param portfolioId The ID of the portfolio to delete
     * @return Map containing success status and deletion confirmation
     */
    void removePortfolio(Integer userId, Integer portfolioId);
}
