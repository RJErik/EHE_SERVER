package ehe_server.service.intf.portfolio;

import ehe_server.dto.PortfolioResponse;

import java.util.List;

/**
 * Interface for portfolio retrieval operations
 */
public interface PortfolioRetrievalServiceInterface {
    List<PortfolioResponse> getPortfolios(Integer userId);
}
