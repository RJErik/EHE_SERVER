package com.example.ehe_server.service.intf.portfolio;

import com.example.ehe_server.dto.PortfolioResponse;

import java.util.List;

/**
 * Interface for portfolio retrieval operations
 */
public interface PortfolioRetrievalServiceInterface {
    /**
     * Retrieves all portfolios for the current user with their calculated values
     * @param userId The user that has initiated the service
     * @return Map containing success status and list of portfolios with values
     */
    List<PortfolioResponse> getPortfolios(Integer userId);
}
