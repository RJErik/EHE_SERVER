package ehe_server.service.intf.portfolio;

import ehe_server.dto.PortfolioDetailsResponse;

public interface PortfolioDetailsServiceInterface {
    PortfolioDetailsResponse getPortfolioDetails(Integer userId, Integer portfolioId);
}
